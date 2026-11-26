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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class playlistDataScraper {

    private static final Logger log = LoggerFactory.getLogger(playlistDataScraper.class);

    /** De cookiebanner verschijnt meteen of helemaal niet (headless/buiten de EU), dus kort wachten. */
    private static final Duration COOKIE_WAIT = Duration.ofSeconds(3);
    private static final Duration TRACKLIST_WAIT = Duration.ofSeconds(30);

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

    /**
     * Pad naar de browser: CHROME_BIN als die gezet is, anders de eerste gevonden
     * Chromium/Chrome. Zo kiest Selenium Manager een driver voor precies die versie
     * (anders pakt hij de nieuwste chromedriver, die niet bij Chromium hoeft te passen).
     */
    private static String findChromeBinary() {
        String chromeBin = System.getenv("CHROME_BIN");
        if (chromeBin != null && !chromeBin.isBlank()) {
            return chromeBin;
        }
        for (String path : List.of("/usr/bin/chromium-browser", "/usr/bin/chromium",
                "/usr/bin/google-chrome", "/usr/bin/google-chrome-stable")) {
            if (java.nio.file.Files.isExecutable(java.nio.file.Path.of(path))) {
                return path;
            }
        }
        return null; // Selenium Manager zoekt zelf
    }

    public String scrapeSpotifyPlaylist(String playlistUrl) throws Exception {
        // In containers (Docker/Railway) gebruiken we de vooraf geïnstalleerde chromedriver
        // (zie Dockerfile: CHROMEDRIVER_BIN). Lokaal regelt Selenium Manager (ingebouwd in
        // Selenium) zelf een chromedriver die past bij de browserversie van CHROME_BIN of
        // de automatisch gevonden Chromium/Chrome.
        String chromeDriverBin = System.getenv("CHROMEDRIVER_BIN");
        if (chromeDriverBin != null && !chromeDriverBin.isBlank()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverBin);
        }

        WebDriver driver = null;
        List<Track> tracks = new ArrayList<>();

        try {
            // Chrome options voor headless browsing
            ChromeOptions options = new ChromeOptions();
            String chromeBin = findChromeBinary();
            if (chromeBin != null) {
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
            WebDriverWait wait = new WebDriverWait(driver, TRACKLIST_WAIT);

            log.info("Navigeren naar Spotify playlist: {}", playlistUrl);

            driver.get(playlistUrl);

            acceptCookiesIfShown(driver);

            // Wacht tot de pagina laadt en tracks zichtbaar zijn
            log.debug("Wachten op tracks...");
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("div[data-testid='tracklist-row']")));
                log.debug("Tracklist gevonden");
            } catch (Exception e) {
                log.warn("Timeout bij wachten op tracks, probeert alternatieve selectors");
            }

            // Scroll naar beneden om alle tracks te laden
            scrollToBottom(driver, 5);

            // Probeer verschillende selectors voor tracks
            List<WebElement> trackRows = new ArrayList<>();

            // Primary selector
            trackRows = driver.findElements(By.cssSelector("div[data-testid='tracklist-row']"));
            log.debug("Tracks gevonden met primary selector: {}", trackRows.size());

            // Als geen tracks, probeer alternatieve selectors
            if (trackRows.isEmpty()) {
                trackRows = driver.findElements(By.cssSelector("[data-testid*='track']"));
                log.debug("Tracks gevonden met alternatieve selector: {}", trackRows.size());
            }

            if (trackRows.isEmpty()) {
                trackRows = driver.findElements(By.cssSelector("div[class*='tracklist-row']"));
                log.debug("Tracks gevonden met class selector: {}", trackRows.size());
            }

            // Verzamel track informatie
            for (int i = 0; i < trackRows.size(); i++) {
                try {
                    WebElement trackRow = trackRows.get(i);
                    Track track = extractTrackInfo(driver, trackRow, i + 1);
                    if (track != null && !track.getTitle().equals("Onbekende titel")) {
                        tracks.add(track);
                        log.debug("Track {}: {} - {}", track.getPosition(), track.getTitle(), track.getArtist());
                    }
                } catch (Exception e) {
                    log.warn("Fout bij track {}: {}", i + 1, e.getMessage());
                }
            }

            log.info("{} tracks opgehaald", tracks.size());

        } catch (Exception e) {
            log.error("Fout tijdens scraping", e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.debug("Browser gesloten");
            }
        }

        return convertToJson(tracks);
    }

    private void acceptCookiesIfShown(WebDriver driver) {
        try {
            WebElement acceptButton = new WebDriverWait(driver, COOKIE_WAIT).until(
                    ExpectedConditions.elementToBeClickable(By.xpath(
                            "//button[@id='onetrust-accept-btn-handler' or contains(text(), 'Accept')]")));
            acceptButton.click();
            log.debug("Cookies geaccepteerd");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("Geen cookiebanner binnen {} s", COOKIE_WAIT.toSeconds());
        }
    }

    private void scrollToBottom(WebDriver driver, int scrollCount) {
        try {
            for (int i = 0; i < scrollCount; i++) {
                ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("window.scrollTo(0, document.body.scrollHeight);");
                log.debug("Scroll {}/{}", i + 1, scrollCount);
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
                        log.debug("Kon titel niet vinden voor track {}", position);
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
                        log.debug("Kon artiest niet vinden voor track {}", position);
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
            log.warn("Extractiefout voor track {}: {}", position, e.getMessage());
            return new Track("Onbekende titel", "Onbekende artiest", position);
        }
    }

    static String convertToJson(List<Track> tracks) {
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