package com.lucasfenstra.muziekdetective.scraper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistJsonTest {

    @Test
    void zetTracksOmNaarJson() {
        String json = playlistDataScraper.convertToJson(List.of(
                new playlistDataScraper.Track("Titel A", "Artiest A", 1),
                new playlistDataScraper.Track("Titel B", "Artiest B", 2)));

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertThat(root.get("total_tracks").getAsInt()).isEqualTo(2);
        JsonObject second = root.getAsJsonArray("tracks").get(1).getAsJsonObject();
        assertThat(second.get("title").getAsString()).isEqualTo("Titel B");
        assertThat(second.get("artist").getAsString()).isEqualTo("Artiest B");
        assertThat(second.get("position").getAsInt()).isEqualTo(2);
    }

    @Test
    void legeLijstGeeftNulTracks() {
        JsonObject root = JsonParser.parseString(playlistDataScraper.convertToJson(List.of())).getAsJsonObject();
        assertThat(root.get("total_tracks").getAsInt()).isZero();
        assertThat(root.getAsJsonArray("tracks")).isEmpty();
    }
}
