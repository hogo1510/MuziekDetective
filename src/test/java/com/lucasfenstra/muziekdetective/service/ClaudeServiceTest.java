package com.lucasfenstra.muziekdetective.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeServiceTest {

    @Test
    void zonderSleutelKomtErEenFoutmeldingZonderApiAanroep() {
        String result = new ClaudeService((String) null).getDiscoveryRecommendation("{}");
        assertThat(result).startsWith("❌").contains("ANTHROPIC_API_KEY ontbreekt");
    }

    @Test
    void promptBevatDePlaylistEnHetAntwoordformaat() {
        String prompt = ClaudeService.buildDiscoveryPrompt("{\"tracks\":[\"Song X\"]}");
        assertThat(prompt)
                .contains("{\"tracks\":[\"Song X\"]}")
                .contains("JSON");
    }

    @Test
    void ontdekkingWordtNetjesGeformatteerd() {
        Ontdekking o = new Ontdekking("Titel", "Artiest", "indie pop", "Past goed.", "Liefhebbers van indie.");
        assertThat(ClaudeService.formatOntdekking(o)).isEqualTo(
                "ONTDEKKING: Titel - Artiest\n\n" +
                "STIJL: indie pop\n\n" +
                "REDEN: Past goed.\n\n" +
                "VOOR WIE: Liefhebbers van indie.");
    }
}
