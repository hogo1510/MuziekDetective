package com.lucasfenstra.muziekdetective.controller;

import com.lucasfenstra.muziekdetective.scraper.playlistDataScraper;
import com.lucasfenstra.muziekdetective.service.ClaudeService;
import com.lucasfenstra.muziekdetective.service.ClientIpResolver;
import com.lucasfenstra.muziekdetective.service.RateLimitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MusicController.class)
@Import({RateLimitConfig.class, ClientIpResolver.class})
@TestPropertySource(properties = {
        "muziekdetective.ratelimit.max-requests=2",
        "muziekdetective.ratelimit.window=10m",
        "muziekdetective.trust-proxy=false"
})
class MusicControllerTest {

    private static final String URL = "https://open.spotify.com/playlist/abc";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private playlistDataScraper scraper;

    @MockitoBean
    private ClaudeService claudeService;

    @Test
    void homeToontDeIndexPagina() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
    }

    @Test
    void analyseGeeftDeAanbevelingDoor() throws Exception {
        when(scraper.scrapeSpotifyPlaylist(URL)).thenReturn("{\"total_tracks\":1}");
        when(claudeService.getDiscoveryRecommendation("{\"total_tracks\":1}")).thenReturn("ONTDEKKING: X - Y");

        mvc.perform(post("/analyze").param("playlistUrl", URL).with(r -> { r.setRemoteAddr("1.0.0.1"); return r; }))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("success", true))
                .andExpect(flash().attribute("discovery", "ONTDEKKING: X - Y"))
                .andExpect(flash().attribute("playlistUrl", URL));
    }

    @Test
    void scraperFoutWordtAlsFoutGetoond() throws Exception {
        when(scraper.scrapeSpotifyPlaylist(anyString())).thenThrow(new RuntimeException("kapot"));

        mvc.perform(post("/analyze").param("playlistUrl", URL).with(r -> { r.setRemoteAddr("1.0.0.2"); return r; }))
                .andExpect(flash().attribute("success", false))
                .andExpect(flash().attribute("error", "Fout bij analyseren: kapot"));
    }

    @Test
    void naHetMaximumWordtHetVerzoekGeweigerd() throws Exception {
        when(scraper.scrapeSpotifyPlaylist(anyString())).thenReturn("{}");
        when(claudeService.getDiscoveryRecommendation(anyString())).thenReturn("ok");

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/analyze").param("playlistUrl", URL).with(r -> { r.setRemoteAddr("1.0.0.3"); return r; }))
                    .andExpect(flash().attribute("success", true));
        }
        mvc.perform(post("/analyze").param("playlistUrl", URL).with(r -> { r.setRemoteAddr("1.0.0.3"); return r; }))
                .andExpect(flash().attribute("success", false))
                .andExpect(flash().attribute("error",
                        "Je hebt het maximum van 2 analyses per 10 minuten bereikt. Probeer het straks opnieuw."));
    }

    @Test
    void verzonnenForwardedForHeaderOmzeiltDeLimietNiet() throws Exception {
        when(scraper.scrapeSpotifyPlaylist(anyString())).thenReturn("{}");
        when(claudeService.getDiscoveryRecommendation(anyString())).thenReturn("ok");

        for (int i = 0; i < 3; i++) {
            String fakeIp = "9.9.9." + i;
            mvc.perform(post("/analyze").param("playlistUrl", URL).header("X-Forwarded-For", fakeIp)
                    .with(r -> { r.setRemoteAddr("1.0.0.4"); return r; }));
        }
        verify(scraper, org.mockito.Mockito.times(2)).scrapeSpotifyPlaylist(anyString());
    }

    @Test
    void progressEndpointBestaatNietMeer() throws Exception {
        mvc.perform(get("/progress").param("sessionId", "x")).andExpect(status().isNotFound());
        verify(scraper, never()).scrapeSpotifyPlaylist(anyString());
    }
}
