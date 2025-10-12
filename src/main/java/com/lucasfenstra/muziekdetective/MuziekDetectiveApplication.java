package com.lucasfenstra.muziekdetective;

import com.lucasfenstra.muziekdetective.ollama.SimpleOllamaService;
import com.lucasfenstra.muziekdetective.scraper.playlistDataScraper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MuziekDetectiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(MuziekDetectiveApplication.class, args);

        String playlistUrl = "https://open.spotify.com/playlist/0cVG2aGlZnT4Iy8dv3KPeW";

        playlistDataScraper scraper = new playlistDataScraper();
        SimpleOllamaService ollamaService = new SimpleOllamaService();

        try {
            System.out.println("🚀 Start scraping Spotify playlist...");
            String jsonResult = scraper.scrapeSpotifyPlaylist(playlistUrl);
            System.out.println("✅ Scraping voltooid!");

            System.out.println("\n📊 Playlist data:");
            System.out.println(jsonResult);

            System.out.println("\n🤖 Vraag muziekontdekking aan Ollama Mistral...");
            String discovery = ollamaService.getDiscoveryRecommendation(jsonResult);

            System.out.println("\n" + "=".repeat(70));
            System.out.println("🎵 MUZIEK ONTDEKKING - IETS NIEUWS VOOR JOU!");
            System.out.println("=".repeat(70));
            System.out.println(discovery);
            System.out.println("=".repeat(70));
            System.out.println("\n💡 Tip: Zoek dit nummer op Spotify om het te beluisteren!");

        } catch (Exception e) {
            System.err.println("❌ Fout bij het proces: " + e.getMessage());
            e.printStackTrace();
        }
    }
}