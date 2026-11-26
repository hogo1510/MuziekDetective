package com.lucasfenstra.muziekdetective.service;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Het antwoord dat Claude als JSON moet teruggeven (structured output). */
public record Ontdekking(
        @JsonPropertyDescription("Titel van het aangeraden nummer, precies zoals op Spotify")
        String titel,
        @JsonPropertyDescription("Artiest van het nummer; moet feitelijk kloppen bij de titel")
        String artiest,
        @JsonPropertyDescription("Muziekstijl of genre")
        String stijl,
        @JsonPropertyDescription("2-3 zinnen: waarom dit nummer bij de playlist past en een goede ontdekking is")
        String reden,
        @JsonPropertyDescription("Welk type luisteraar hier vooral van zal genieten")
        String voorWie) {
}
