package com.lucasfenstra.muziekdetective.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ClaudeService {

    private static final String API_KEY_NAME = "ANTHROPIC_API_KEY";
    private static final String MODEL = "claude-haiku-5-5";

    private final String apiKey;

    public ClaudeService() {
        String key = System.getenv(API_KEY_NAME);
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
                if (trimmed.startsWith(API_KEY_NAME + "=")) {
                    return trimmed.substring((API_KEY_NAME + "=").length()).trim();
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
                throw new IllegalStateException(API_KEY_NAME + " ontbreekt (zet hem in de omgeving of in .env)");
            }

            progressCallback.accept("Playlist data voorbereiden...");
            String prompt = buildDiscoveryPrompt(jsonPlaylist);

            AnthropicClient client = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();

            MessageCreateParams params = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(16000L)
                    .outputConfig(OutputConfig.builder()
                            .effort(OutputConfig.Effort.MEDIUM)
                            .build())
                    .addUserMessage(prompt)
                    .build();

            progressCallback.accept("AI analyseert je muzieksmaak...");
            Message response = client.messages().create(params);

            if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
                throw new RuntimeException("Claude weigerde dit verzoek te beantwoorden");
            }

            progressCallback.accept("Resultaat verwerken...");
            String aiResponse = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining("\n"))
                    .trim();

            if (aiResponse.isEmpty()) {
                throw new RuntimeException("Leeg antwoord van Claude");
            }

            progressCallback.accept("Aanbeveling samenvatten...");
            return formatDiscoveryResponse(aiResponse);

        } catch (Exception e) {
            progressCallback.accept("Fout opgetreden");
            return "❌ Kon geen ontdekkingsaanbeveling genereren: " + e.getMessage() +
                    "\n\n💡 Controleer of de " + API_KEY_NAME + " correct is ingesteld";
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
                "BELANGRIJKSTE REGEL: de artiestnaam moet feitelijk correct zijn. Verzin nooit een artiest bij een titel " +
                "en raad niet als je het niet zeker weet. Kies liever een iets bekendere combinatie waarvan je zeker " +
                "weet dat titel en artiest bij elkaar horen, dan een obscure combinatie die je gokt.\n\n" +
                "Geef je antwoord in dit format, zonder extra tekst ervoor of erna:\n" +
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
