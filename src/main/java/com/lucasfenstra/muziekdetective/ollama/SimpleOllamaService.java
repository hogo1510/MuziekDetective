package com.lucasfenstra.muziekdetective.service;

import com.google.gson.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

public class SimpleOllamaService {

    private final HttpClient httpClient;
    private final String ollamaUrl;

    public SimpleOllamaService() {
        this.httpClient = HttpClient.newHttpClient();
        this.ollamaUrl = "http://localhost:11434/api/generate";
    }

    public String getDiscoveryRecommendation(String jsonPlaylist) {
        return getDiscoveryRecommendation(jsonPlaylist, progress -> {});
    }

    public String getDiscoveryRecommendation(String jsonPlaylist, Consumer<String> progressCallback) {
        try {
            progressCallback.accept("Playlist data voorbereiden...");
            String prompt = buildDiscoveryPrompt(jsonPlaylist);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "mistral");
            requestBody.addProperty("prompt", prompt);
            requestBody.addProperty("stream", false);

            progressCallback.accept("Verbinden met AI...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            progressCallback.accept("AI analyseert je muzieksmaak...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            progressCallback.accept("Resultaat verwerken...");
            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String aiResponse = responseJson.get("response").getAsString();

            progressCallback.accept("Aanbeveling samenvatten...");
            return formatDiscoveryResponse(aiResponse);

        } catch (Exception e) {
            progressCallback.accept("Fout opgetreden");
            return "❌ Kon geen ontdekkingsaanbeveling genereren: " + e.getMessage() +
                    "\n\n💡 Controleer of Ollama draait op localhost:11434";
        }
    }

    private String buildDiscoveryPrompt(String jsonPlaylist) {
        return "Je bent een muziekexpert gespecialiseerd in het ontdekken van nieuwe muziek. " +
                "Analyseer deze Spotify playlist en suggereer ÉÉN NIEUW nummer dat NIET in deze playlist staat, " +
                "maar wel perfect zou passen gebaseerd op de muzieksmaak die uit deze playlist blijkt.\n\n" +
                "PLAYLIST DATA:\n" + jsonPlaylist +
                "\n\nCRITERIA VOOR SUGGESTIE:\n" +
                "- Het moet een ander nummer zijn dan wat al in de playlist staat\n" +
                "- Het moet van een andere artiest zijn, of een minder bekend nummer van een artiest die wel in de playlist staat\n" +
                "- Het moet dezelfde muziekstijl, sfeer of vibe hebben als de playlist\n" +
                "- Het moet een nummer zijn dat mensen die deze playlist leuk vinden, waarschijnlijk ook leuk zullen vinden\n" +
                "- Kies voor verrassing en ontdekking, niet voor de meest voor de hand liggende keuze\n\n" +
                "Geef je antwoord in dit format:\n" +
                "🎵 ONTDEKKING: [titel] - [artiest]\n\n" +
                "🎼 STIJL: [muziekstijl/genre]\n\n" +
                "💭 REDEN: [leg in 2-3 zinnen uit waarom dit nummer perfect past bij de playlist en waarom het een goede ontdekking is]\n\n" +
                "👥 VOOR WIE: [beschrijf welk type luisteraar hier vooral van zal genieten]";
    }

    private String formatDiscoveryResponse(String aiResponse) {
        if (aiResponse.contains("🎵 ONTDEKKING:")) {
            return aiResponse;
        } else {
            return "🎵 ONTDEKKING: " + extractValue(aiResponse, "ontdekking", "song", "nummer") + "\n\n" +
                    "🎼 STIJL: " + extractValue(aiResponse, "stijl", "genre", "style") + "\n\n" +
                    "💭 REDEN: " + extractValue(aiResponse, "reden", "uitleg", "reason") + "\n\n" +
                    "👥 VOOR WIE: " + extractValue(aiResponse, "voor wie", "appeal", "luisteraar");
        }
    }

    private String extractValue(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.toLowerCase().contains(keyword)) {
                return "Dit nummer past perfect bij je muzieksmaak en biedt een verfrissende nieuwe sound.";
            }
        }
        return "Perfect voor liefhebbers van deze muziekstijl die iets nieuws willen ontdekken!";
    }
}