package com.lucasfenstra.muziekdetective.service;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

public class GroqService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient;
    private final String apiKey;

    public GroqService() {
        this.httpClient = HttpClient.newHttpClient();
        String key = System.getenv("GROQ_API_KEY");
        this.apiKey = (key != null && !key.isBlank()) ? key : readKeyFromDotEnv();
    }

    private String readKeyFromDotEnv() {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("GROQ_API_KEY=")) {
                    return trimmed.substring("GROQ_API_KEY=".length()).trim();
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public String getDiscoveryRecommendation(String jsonPlaylist) {
        return getDiscoveryRecommendation(jsonPlaylist, progress -> {});
    }

    public String getDiscoveryRecommendation(String jsonPlaylist, Consumer<String> progressCallback) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("GROQ_API_KEY omgevingsvariabele ontbreekt");
            }

            progressCallback.accept("Playlist data voorbereiden...");
            String prompt = buildDiscoveryPrompt(jsonPlaylist);

            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", MODEL);
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.8);

            progressCallback.accept("Verbinden met AI...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            progressCallback.accept("AI analyseert je muzieksmaak...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            if (responseJson.has("error")) {
                String message1 = responseJson.getAsJsonObject("error").get("message").getAsString();
                throw new RuntimeException(message1);
            }

            progressCallback.accept("Resultaat verwerken...");
            String aiResponse = responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            progressCallback.accept("Aanbeveling samenvatten...");
            return formatDiscoveryResponse(aiResponse);

        } catch (Exception e) {
            progressCallback.accept("Fout opgetreden");
            return "❌ Kon geen ontdekkingsaanbeveling genereren: " + e.getMessage() +
                    "\n\n💡 Controleer of de GROQ_API_KEY omgevingsvariabele correct is ingesteld";
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
                "ONTDEKKING: [titel] - [artiest]\n\n" +
                "STIJL: [muziekstijl/genre]\n\n" +
                "REDEN: [leg in 2-3 zinnen uit waarom dit nummer perfect past bij de playlist en waarom het een goede ontdekking is]\n\n" +
                "VOOR WIE: [beschrijf welk type luisteraar hier vooral van zal genieten]";
    }

    private String formatDiscoveryResponse(String aiResponse) {
        if (aiResponse.contains("ONTDEKKING:")) {
            return aiResponse;
        } else {
            return "ONTDEKKING: " + extractValue(aiResponse, "ontdekking", "song", "nummer") + "\n\n" +
                    "STIJL: " + extractValue(aiResponse, "stijl", "genre", "style") + "\n\n" +
                    "REDEN: " + extractValue(aiResponse, "reden", "uitleg", "reason") + "\n\n" +
                    "VOOR WIE: " + extractValue(aiResponse, "voor wie", "appeal", "luisteraar");
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
