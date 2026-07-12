package com.lucasfenstra.muziekdetective.scraper;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class playlistDataScraper {

    public static class Track {
        private String title;
        private String artist;
        private int position;

        public Track(String title, String artist, int position) {
            this.title = title;
            this.artist = artist;
            this.position = position;
        }

        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public int getPosition() { return position; }
    }

    public String scrapeSpotifyPlaylist(String playlistUrl) throws Exception {
        // In containers (Docker/Railway) gebruiken we de vooraf geïnstalleerde chromedriver
        // (zie Dockerfile: CHROMEDRIVER_BIN). Lokaal valt dit terug op WebDriverManager,
        // die de juiste driver automatisch download.
        String chromeDriverBin = System.getenv("CHROMEDRIVER_BIN");
        if (chromeDriverBin != null && !chromeDriverBin.isBlank()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverBin);
        } else {
            WebDriverManager.chromedriver().setup();
        }

        WebDriver driver = null;
        List<Track> tracks = new ArrayList<>();

        try {
            // Chrome options voor headless browsing
            ChromeOptions options = new ChromeOptions();
            String chromeBin = System.getenv("CHROME_BIN");
            if (chromeBin != null && !chromeBin.isBlank()) {
                options.setBinary(chromeBin);
            }
            options.addArguments("--headless=new"); // Headless mode
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--remote-allow-origins=*");

            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            System.out.println("WebDriverManager heeft ChromeDriver geïnstalleerd");
            System.out.println("Navigeren naar Spotify playlist: " + playlistUrl);

            driver.get(playlistUrl);

            // Wacht en accepteer cookies indien nodig
            try {
                WebElement acceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(), 'Accept')]")));
                acceptButton.click();
                System.out.println("Cookies geaccepteerd");
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Geen cookie popup gevonden: " + e.getMessage());
            }

            // Wacht tot de pagina laadt en tracks zichtbaar zijn
            System.out.println("Wachten op tracks...");
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("div[data-testid='tracklist-row']")));
                System.out.println("Tracklist gevonden!");
            } catch (Exception e) {
                System.out.println("Timeout bij wachten op tracks, probeert alternatieve selectors...");
            }

            // Scroll naar beneden om alle tracks te laden
            scrollToBottom(driver, 5);

            // Probeer verschillende selectors voor tracks
            List<WebElement> trackRows = new ArrayList<>();

            // Primary selector
            trackRows = driver.findElements(By.cssSelector("div[data-testid='tracklist-row']"));
            System.out.println("Aantal tracks gevonden met primary selector: " + trackRows.size());

            // Als geen tracks, probeer alternatieve selectors
            if (trackRows.isEmpty()) {
                trackRows = driver.findElements(By.cssSelector("[data-testid*='track']"));
                System.out.println("Aantal tracks gevonden met alternatieve selector: " + trackRows.size());
            }

            if (trackRows.isEmpty()) {
                trackRows = driver.findElements(By.cssSelector("div[class*='tracklist-row']"));
                System.out.println("Aantal tracks gevonden met class selector: " + trackRows.size());
            }

            // Verzamel track informatie
            for (int i = 0; i < trackRows.size(); i++) {
                try {
                    WebElement trackRow = trackRows.get(i);
                    Track track = extractTrackInfo(driver, trackRow, i + 1);
                    if (track != null && !track.getTitle().equals("Onbekende titel")) {
                        tracks.add(track);
                        System.out.println("✓ Track " + track.getPosition() + ": " +
                                track.getTitle() + " - " + track.getArtist());
                    }
                } catch (Exception e) {
                    System.err.println("✗ Fout bij track " + (i + 1) + ": " + e.getMessage());
                }
            }

            System.out.println("Totaal aantal succesvolle tracks: " + tracks.size());

        } catch (Exception e) {
            System.err.println("Fout tijdens scraping: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Browser gesloten");
            }
        }

        return convertToJson(tracks);
    }

    private void scrollToBottom(WebDriver driver, int scrollCount) {
        try {
            for (int i = 0; i < scrollCount; i++) {
                ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("window.scrollTo(0, document.body.scrollHeight);");
                System.out.println("Scroll " + (i + 1) + "/" + scrollCount);
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Track extractTrackInfo(WebDriver driver, WebElement trackRow, int position) {
        try {
            String title = "Onbekende titel";
            String artist = "Onbekende artiest";

            // Probeer verschillende selectors voor titel
            try {
                // Meest betrouwbare selector eerst proberen
                WebElement titleElement = trackRow.findElement(By.cssSelector("div[data-testid='tracklist-row'] div[dir='auto']"));
                title = titleElement.getText();
            } catch (Exception e) {
                try {
                    WebElement titleElement = trackRow.findElement(By.cssSelector("a[data-testid='internal-track-link']"));
                    title = titleElement.getText();
                } catch (Exception e2) {
                    try {
                        // Fallback: zoek naar anchor met track in href
                        WebElement titleElement = trackRow.findElement(By.cssSelector("a[href*='/track/']"));
                        title = titleElement.getText();
                    } catch (Exception e3) {
                        System.err.println("Kon titel niet vinden voor track " + position);
                    }
                }
            }

            // Probeer verschillende selectors voor artiest
            try {
                WebElement artistElement = trackRow.findElement(By.cssSelector("a[data-testid='internal-artist-link']"));
                artist = artistElement.getText();
            } catch (Exception e) {
                try {
                    WebElement artistElement = trackRow.findElement(By.cssSelector("a[href*='/artist/']"));
                    artist = artistElement.getText();
                } catch (Exception e2) {
                    try {
                        // Fallback: zoek in spans
                        List<WebElement> spans = trackRow.findElements(By.cssSelector("span"));
                        for (WebElement span : spans) {
                            String text = span.getText();
                            if (!text.isEmpty() && !text.equals(title) &&
                                    !text.matches("\\d+:\\d+") && !text.contains("•") &&
                                    !text.matches("\\d+$")) {
                                artist = text;
                                break;
                            }
                        }
                    } catch (Exception e3) {
                        System.err.println("Kon artiest niet vinden voor track " + position);
                    }
                }
            }

            // Als nog steeds onbekend, probeer met JavaScript
            if (artist.equals("Onbekende artiest")) {
                try {
                    String script = "return arguments[0].innerText;";
                    String fullText = (String) ((org.openqa.selenium.JavascriptExecutor) driver)
                            .executeScript(script, trackRow);
                    if (fullText.contains(" - ")) {
                        String[] parts = fullText.split(" - ");
                        if (parts.length >= 2) {
                            artist = parts[1].split("\n")[0]; // Neem eerste regel na de dash
                        }
                    }
                } catch (Exception e) {
                    // Negeer JavaScript fouten
                }
            }

            return new Track(title, artist, position);

        } catch (Exception e) {
            System.err.println("Extractie fout voor track " + position + ": " + e.getMessage());
            return new Track("Onbekende titel", "Onbekende artiest", position);
        }
    }

    private String convertToJson(List<Track> tracks) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject result = new JsonObject();
        result.addProperty("total_tracks", tracks.size());

        JsonArray tracksArray = new JsonArray();
        for (Track track : tracks) {
            JsonObject trackObject = new JsonObject();
            trackObject.addProperty("position", track.getPosition());
            trackObject.addProperty("title", track.getTitle());
            trackObject.addProperty("artist", track.getArtist());
            tracksArray.add(trackObject);
        }

        result.add("tracks", tracksArray);
        return gson.toJson(result);
    }
}