package com.lucasfenstra.muziekdetective.service;

import com.google.gson.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Service voor AI-powered muziekaanbevelingen via Groq API
 * Groq biedt ultra-snelle inferentie met Mixtral modellen
 */
public class SimpleOllamaService {

    private final HttpClient httpClient;
    private final String groqApiUrl;
    private final String groqApiKey;
    private static final String DEFAULT_MODEL = "mixtral-8x7b-32768";
    private static final int REQUEST_TIMEOUT_SECONDS = 60;
    private static final int MAX_TOKENS = 1024;
    private static final double TEMPERATURE = 0.7;

    /**
     * Constructor - initialiseert Groq API client
     * Vereist GROQ_API_KEY environment variable
     */
    public SimpleOllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.groqApiUrl = "https://api.groq.com/openai/v1/chat/completions";
        this.groqApiKey = System.getenv("GROQ_API_KEY");

        if (this.groqApiKey == null || this.groqApiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "GROQ_API_KEY environment variable is niet ingesteld.\n" +
                "Verkrijg een gratis API key op: https://console.groq.com/keys\n" +
                "Stel in via: export GROQ_API_KEY=\"your_key_here\""
            );
        }
    }

    /**
     * Genereer muziekaanbeveling zonder progress updates
     */
    public String getDiscoveryRecommendation(String jsonPlaylist) {
        return getDiscoveryRecommendation(jsonPlaylist, progress -> {});
    }

    /**
     * Genereer muziekaanbeveling met progress callback
     *
     * @param jsonPlaylist JSON string met playlist data
     * @param progressCallback Callback voor progress updates
     * @return Geformatteerde muziekaanbeveling
     */
    public String getDiscoveryRecommendation(String jsonPlaylist, Consumer<String> progressCallback) {
        try {
            progressCallback.accept("📋 Playlist data voorbereiden...");
            String prompt = buildDiscoveryPrompt(jsonPlaylist);

            progressCallback.accept("🔧 API request voorbereiden...");
            String requestBodyJson = buildGroqRequestBody(prompt);

            progressCallback.accept("🌐 Verbinden met Groq AI...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .build();

            progressCallback.accept("🎵 AI analyseert je muzieksmaak...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Controleer HTTP status code
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "Groq API fout (HTTP " + response.statusCode() + "): " +
                    extractErrorMessage(response.body())
                );
            }

            progressCallback.accept("📦 Resultaat verwerken...");
            String aiResponse = parseGroqResponse(response.body());

            progressCallback.accept("✨ Aanbeveling formatteren...");
            return formatDiscoveryResponse(aiResponse);

        } catch (java.io.IOException e) {
            progressCallback.accept("❌ Netwerkfout");
            return "❌ Netwerkfout bij verbinden met Groq API: " + e.getMessage() +
                    "\n\n💡 Controleer je internetverbinding en probeer opnieuw.";

        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
            progressCallback.accept("❌ Request onderbroken");
            return "❌ Request werd onderbroken. Probeer opnieuw.";

        } catch (IllegalStateException e) {
            progressCallback.accept("❌ Configuratiefout");
            return "❌ " + e.getMessage();

        } catch (Exception e) {
            progressCallback.accept("❌ Fout opgetreden");
            e.printStackTrace(); // Voor debugging
            return "❌ Onverwachte fout: " + e.getMessage() +
                    "\n\n💡 Controleer of de GROQ_API_KEY correct is ingesteld." +
                    "\n🔗 Verkrijg een key op: https://console.groq.com/keys";
        }
    }

    /**
     * Bouw Groq API request body in OpenAI format
     */
    private String buildGroqRequestBody(String prompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", DEFAULT_MODEL);
        requestBody.addProperty("temperature", TEMPERATURE);
        requestBody.addProperty("max_tokens", MAX_TOKENS);

        // Maak messages array (OpenAI chat format)
        JsonArray messages = new JsonArray();

        // System message voor betere context
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content",
            "Je bent een ervaren muziekexpert en curator die gespecialiseerd is in het ontdekken " +
            "van nieuwe muziek. Je begrijpt muziekstijlen, emotionele tonen en artistieke connecties. " +
            "Geef altijd concrete, bestaande nummers als aanbevelingen."
        );
        messages.add(systemMessage);

        // User message met prompt
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        requestBody.add("messages", messages);

        return requestBody.toString();
    }

    /**
     * Parse Groq API response en extraheer AI antwoord
     */
    private String parseGroqResponse(String responseBody) throws Exception {
        try {
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();

            // Controleer op error in response
            if (responseJson.has("error")) {
                JsonObject error = responseJson.getAsJsonObject("error");
                throw new RuntimeException("Groq API error: " + error.get("message").getAsString());
            }

            // Extraheer AI antwoord
            JsonArray choices = responseJson.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new RuntimeException("Geen antwoord ontvangen van Groq API");
            }

            return choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Ongeldige JSON response van Groq API: " + e.getMessage());
        }
    }

    /**
     * Extraheer error message uit response body
     */
    private String extractErrorMessage(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("error")) {
                return json.getAsJsonObject("error").get("message").getAsString();
            }
        } catch (Exception e) {
            // Als parsing faalt, return hele body
        }
        return responseBody;
    }

    /**
     * Bouw de prompt voor muziekontdekking
     */
    private String buildDiscoveryPrompt(String jsonPlaylist) {
        return "Je bent een muziekexpert gespecialiseerd in het ontdekken van nieuwe muziek. " +
                "Analyseer deze Spotify playlist en suggereer ÉÉN NIEUW nummer dat NIET in deze playlist staat, " +
                "maar wel perfect zou passen gebaseerd op de muzieksmaak die uit deze playlist blijkt.\n\n" +
                "PLAYLIST DATA:\n" + jsonPlaylist +
                "\n\n=== CRITERIA VOOR SUGGESTIE ===\n" +
                "✓ Het moet een BESTAAND, echt nummer zijn (geen fictief nummer)\n" +
                "✓ Het moet een ander nummer zijn dan wat al in de playlist staat\n" +
                "✓ Het moet van een andere artiest zijn, of een minder bekend nummer van een artiest die wel in de playlist staat\n" +
                "✓ Het moet dezelfde muziekstijl, sfeer of vibe hebben als de playlist\n" +
                "✓ Het moet een nummer zijn dat mensen die deze playlist leuk vinden, waarschijnlijk ook leuk zullen vinden\n" +
                "✓ Kies voor verrassing en ontdekking, niet voor de meest voor de hand liggende keuze\n" +
                "✓ Vermeld altijd de exacte titel en artiest\n\n" +
                "=== VERPLICHT ANTWOORD FORMAT ===\n" +
                "Geef je antwoord PRECIES in dit format:\n\n" +
                "🎵 ONTDEKKING: [exacte titel] - [exacte artiestnaam]\n\n" +
                "🎼 STIJL: [muziekstijl/genre]\n\n" +
                "💭 REDEN: [leg in 2-3 zinnen uit waarom dit nummer perfect past bij de playlist en waarom het een goede ontdekking is]\n\n" +
                "👥 VOOR WIE: [beschrijf welk type luisteraar hier vooral van zal genieten]\n\n" +
                "BELANGRIJK: Gebruik exact dit format met emojis, en zorg dat het nummer echt bestaat!";
    }

    /**
     * Formatteer AI response naar gewenste output format
     */
    private String formatDiscoveryResponse(String aiResponse) {
        // Als response al correct geformatteerd is, return as-is
        if (aiResponse.contains("🎵 ONTDEKKING:") &&
            aiResponse.contains("🎼 STIJL:") &&
            aiResponse.contains("💭 REDEN:") &&
            aiResponse.contains("👥 VOOR WIE:")) {
            return aiResponse.trim();
        }

        // Anders probeer te formatteren
        return "🎵 NIEUWE ONTDEKKING\n\n" + aiResponse.trim() +
               "\n\n✨ Gegenereerd door Groq AI (Mixtral-8x7b)";
    }

    /**
     * Utility method - niet meer gebruikt maar behouden voor backwards compatibility
     */
    @Deprecated
    private String extractValue(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.toLowerCase().contains(keyword)) {
                return "Dit nummer past perfect bij je muzieksmaak en biedt een verfrissende nieuwe sound.";
            }
        }
        return "Perfect voor liefhebbers van deze muziekstijl die iets nieuws willen ontdekken!";
    }
}
