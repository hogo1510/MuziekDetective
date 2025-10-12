package com.lucasfenstra.muziekdetective;

import com.lucasfenstra.muziekdetective.scraper.playlistDataScraper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MuziekDetectiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(MuziekDetectiveApplication.class, args);

        String playlistUrl = "https://open.spotify.com/playlist/0cVG2aGlZnT4Iy8dv3KPeW";

        playlistDataScraper scraper = new playlistDataScraper();

        try {
            String jsonResult = scraper.scrapeSpotifyPlaylist(playlistUrl);
            System.out.println("Scraping resultaat:");
            System.out.println(jsonResult);
        } catch (Exception e) {
            System.err.println("Fout bij het scrapen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}