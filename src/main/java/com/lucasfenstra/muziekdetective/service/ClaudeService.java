package com.lucasfenstra.muziekdetective.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);

    private static final String API_KEY_NAME = "ANTHROPIC_API_KEY";
    private static final String MODEL = "claude-haiku-5-5";

    private final String apiKey;

    public ClaudeService() {
        this(resolveApiKey());
    }

    ClaudeService(String apiKey) {
        this.apiKey = apiKey;
    }

    private static String resolveApiKey() {
        String key = System.getenv(API_KEY_NAME);
        return (key != null && !key.isBlank()) ? key : readKeyFromDotEnv();
    }

    private static String readKeyFromDotEnv() {
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
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(API_KEY_NAME + " ontbreekt (zet hem in de omgeving of in .env)");
            }

            String prompt = buildDiscoveryPrompt(jsonPlaylist);

            AnthropicClient client = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();

            StructuredMessageCreateParams<Ontdekking> params = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(16000L)
                    .outputConfig(OutputConfig.builder()
                            .effort(OutputConfig.Effort.MEDIUM)
                            .build())
                    // Structured output: Claude moet JSON volgens het schema van Ontdekking teruggeven.
                    .outputConfig(Ontdekking.class)
                    .addUserMessage(prompt)
                    .build();

            log.info("Aanbeveling opvragen bij {}", MODEL);
            StructuredMessage<Ontdekking> response = client.messages().create(params);

            if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
                throw new RuntimeException("Claude weigerde dit verzoek te beantwoorden");
            }
            if (response.stopReason().map(StopReason.MAX_TOKENS::equals).orElse(false)) {
                throw new RuntimeException("Antwoord van Claude werd afgekapt");
            }

            Ontdekking ontdekking = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(typed -> typed.text())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Leeg antwoord van Claude"));

            log.info("Aanbeveling: {} - {}", ontdekking.titel(), ontdekking.artiest());
            return formatOntdekking(ontdekking);

        } catch (Exception e) {
            log.error("Kon geen aanbeveling genereren", e);
            return "❌ Kon geen ontdekkingsaanbeveling genereren: " + e.getMessage() +
                    "\n\n💡 Controleer of de " + API_KEY_NAME + " correct is ingesteld";
        }
    }

    static String buildDiscoveryPrompt(String jsonPlaylist) {
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
                "Antwoord in het Nederlands, in de velden van het gevraagde JSON-formaat.";
    }

    static String formatOntdekking(Ontdekking o) {
        return "ONTDEKKING: " + o.titel() + " - " + o.artiest() + "\n\n" +
                "STIJL: " + o.stijl() + "\n\n" +
                "REDEN: " + o.reden() + "\n\n" +
                "VOOR WIE: " + o.voorWie();
    }
}
