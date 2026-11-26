# Heropstart: verbeterplan MuziekDetective

Opgesteld op 2026-11-26, bij het weer oppakken van het project (laatste code-commit `b9f54a4`).
Dit is het plan om MuziekDetective te vernieuwen en te verbeteren. Het bouwt voort op [bekende-problemen.md](bekende-problemen.md); de nummers `#1` t/m `#8` verwijzen daarnaar.

Vink stappen af (`[x]`) zodra ze klaar zijn en noteer ze in [werklog.md](werklog.md).

## Samenvatting: waar het nu wringt

De app werkt, maar de twee kernonderdelen zijn de zwakste schakels:

1. **Data ophalen** (scraper): ziet maar ~25-30 nummers, is traag (tot 30 s wachten op een cookieknop, per verzoek een nieuwe Chrome) en breekt zodra Spotify zijn HTML verandert. (#1, #6)
2. **Aanbeveling** (LLM): het model verzint nummers, er is geen controle of het nummer bestaat of al in de playlist staat, en het antwoord wordt als vrije tekst geparsed. (#2, #4)

Daaromheen: fouten worden als succes getoond (#3), een nep-voortgangsbalk (#5) en geen tests (#8).

De grootste winst zit dus niet in "een slimmer model" alleen, maar in de combinatie: **betere input + slimmer model + verificatie van de output**.

---

## Fase 1: snelle winst (een middag)

Kleine wijzigingen met direct merkbaar effect.

- [ ] **Model configureerbaar maken.** `MODEL` en de API-URL staan hard in `GroqService.java`. Verplaats ze naar `application.properties` (`muziekdetective.llm.model=...`, overschrijfbaar met een env-variabele). Dan kun je modellen wisselen zonder code aan te passen of opnieuw te bouwen.
- [ ] **Sterker model kiezen.** Zie [AI-model kiezen](#ai-model-kiezen) hieronder.
- [ ] **Gestructureerde output (JSON).** Laat het model antwoorden met JSON (`response_format: {"type": "json_object"}` bij Groq/OpenAI-compatibele API's, of tool use / structured output bij Claude), bijvoorbeeld:
  ```json
  {"kandidaten": [{"titel": "...", "artiest": "...", "stijl": "...", "reden": "...", "voor_wie": "..."}]}
  ```
  Daarmee verdwijnen `formatDiscoveryResponse` en `extractValue` met hun hardgecodeerde nep-zinnen (#4).
- [ ] **Fouten eerlijk tonen (#3).** Laat `GroqService` en de scraper een exception gooien in plaats van een foutmelding als tekst / een lege lijst terug te geven. De controller zet dan `success=false`. Weiger ook een analyse als de playlist 0 nummers heeft.
- [ ] **Cookieknop-wachttijd verlagen (#6)** van 30 s naar ~3 s, of helemaal overslaan in headless.
- [ ] **Dode code weg (#5):** `progressUpdates`, `sessionId` en `/progress` verwijderen, of in fase 3 echt aansluiten.

## Fase 2: betere aanbevelingen (de kern)

Dit lost het verzinnen van nummers echt op.

- [ ] **Meerdere kandidaten vragen.** Vraag 5 kandidaten in plaats van 1.
- [ ] **Verifiëren.** Zoek elke kandidaat op via de **Spotify Search API** (`GET /v1/search?q=track:<titel> artist:<artiest>&type=track`). Neem de eerste die:
  - echt bestaat (artiest én titel matchen, niet alleen ongeveer),
  - niet al in de playlist staat.
- [ ] **Echte link tonen.** Met het Spotify-resultaat heb je ook de track-URL, albumhoes en eventueel een embed-speler. Toon die in `index.html`: veel overtuigender dan alleen tekst.
- [ ] **Fallback:** als geen enkele kandidaat bestaat, één keer opnieuw vragen met de afgewezen namen erbij ("deze bestaan niet / staan al in de playlist").

Voor de Search API heb je een Spotify developer-app nodig (client credentials flow, gratis). Die heb je in fase 3 ook nodig, dus dat is dubbel nuttig.

## Fase 3: scraper vervangen (#1, #6)

- [ ] **Spotify Web API in plaats van Selenium.** `GET /v1/playlists/{id}/items`, gepagineerd per 100. Voordelen:
  - alle nummers in plaats van ~28,
  - seconden in plaats van 30+ seconden,
  - geen Chromium nodig: de Docker-image wordt veel kleiner (Debian + Chromium eruit, een gewone JRE-image volstaat),
  - breekt niet als Spotify zijn website aanpast.
- Let op: Spotify heeft de Web API voor nieuwe apps beperkt (onder andere geen toegang tot Spotify's eigen redactionele/algoritmische playlists, en geen `recommendations`/`audio-features` meer). **Gebruikersplaylists werken wel.** Toon een duidelijke melding als een playlist niet opgehaald kan worden.
- Optioneel: Selenium als fallback houden voor playlists die de API weigert. Eerlijk gezegd: waarschijnlijk niet de moeite, het verdubbelt het onderhoud.
- Extra data die de API gratis geeft en de LLM helpt: genres van artiesten (`GET /v1/artists?ids=...`), releasejaar, populariteit. Geef die mee in de prompt voor betere aanbevelingen.

## Fase 4: codekwaliteit

- [ ] Services als Spring beans (`@Service`) injecteren in plaats van `new` per verzoek (#8).
- [ ] `RestClient` (Spring) of een gedeelde `HttpClient` gebruiken.
- [ ] `playlistDataScraper` hernoemen naar `PlaylistDataScraper`, of verwijderen na fase 3.
- [ ] SLF4J-logger in plaats van `System.out`.
- [ ] Configuratie via `@ConfigurationProperties` (API-sleutels, model, rate-limit).
- [ ] Rate limiter: betrouwbaar IP gebruiken achter Railway's proxy en oude IP's opruimen (#7). Of de library Bucket4j gebruiken.
- [ ] **Tests:** unit tests voor het parsen van de LLM-JSON, de verificatielogica en de rate limiter; de externe API's mocken (bijv. met WireMock of `MockRestServiceServer`).
- [ ] Afhankelijkheden bijwerken: Spring Boot 3.5.6 → de nieuwste versie (controleer of 4.x stabiel is en wat er breekt), Gson/jsoup/Selenium naar de laatste versies. Java 21 → 25 (de nieuwste LTS) is optioneel.

## Fase 5: nieuwe features (ideeën)

Kies wat je leuk vindt; niets hiervan is nodig.

- **Echte voortgang** via Server-Sent Events (`SseEmitter`) in plaats van de getimede animatie.
- **Meerdere aanbevelingen** tonen (bijv. 3), met per stuk een Spotify-embed.
- **"Nog een"-knop** die een nieuwe suggestie geeft en de vorige uitsluit.
- **Uitleg van de muzieksmaak**: een korte analyse van de playlist zelf (genres, tijdperk, sfeer) vóór de aanbeveling.
- **Caching** per playlist-ID (bijv. Caffeine, 1 uur) zodat dezelfde playlist niet steeds opnieuw opgehaald en geanalyseerd wordt.
- **Toevoegen aan Spotify**: met user-login (OAuth) het nummer direct in een playlist zetten. Veel meer werk (login, tokens), alleen doen als je dat wilt leren.
- **Streaming-antwoord**: de uitleg woord voor woord tonen terwijl de LLM schrijft.

---

## AI-model kiezen

Het huidige model `llama-3.3-70b-versatile` (via Groq) is snel en gratis, maar verzint regelmatig titel/artiest-combinaties. Muziekkennis van obscure nummers is precies waar kleinere modellen zwak in zijn.

**Let op (nagekeken 2026-11-26):** Groq heeft `llama-3.3-70b-versatile` uitgefaseerd. Voor het free/developer-tier is het sinds 2026-08-16 uitgeschakeld, dus de app werkt nu waarschijnlijk niet meer. Groq raadt `openai/gpt-oss-120b` aan als vervanger.

Concrete kandidaten (prijzen per 1 miljoen tokens, invoer/uitvoer):

| Model | Provider | Prijs | Opmerking |
|---|---|---|---|
| `openai/gpt-oss-120b` | Groq | $0,15 / $0,60 | Officiële vervanger, ondersteunt strikte JSON-schema-output, één regel wijzigen |
| `openai/gpt-oss-20b` | Groq | $0,075 / $0,30 | Kleiner en goedkoper, minder feitenkennis |
| `claude-haiku-5-5` | Anthropic | $0,10 / $0,50 | Goedkoop, sterkere feitenkennis, andere API (Messages API) |
| `claude-sonnet-5-5` | Anthropic | $2 / $10 | Sterkst, maar ~20× duurder dan Haiku. Voor deze taak waarschijnlijk overkill |

Kosten per analyse (ongeveer 2.000 tokens invoer, 500 uitvoer): bij gpt-oss-120b en Haiku 5.5 allebei minder dan $0,001. Kosten zijn dus geen argument bij de keuze tussen die twee.

**Keuze:** eerst `openai/gpt-oss-120b` als noodreparatie (alleen de modelnaam wijzigen), daarna `claude-haiku-5-5` als echte upgrade, samen met de Spotify-verificatie uit fase 2.

**Gedaan (2026-11-26):** direct overgestapt op `claude-haiku-5-5` via de Anthropic Java-SDK (`ClaudeService`), sleutel in `ANTHROPIC_API_KEY`.

Opties, van minste naar meeste wijziging:

| Optie | Wat | Voor | Tegen |
|---|---|---|---|
| **A. Ander Groq-model** | Een groter/nieuwer model op Groq kiezen. Bekijk het actuele aanbod op de Groq-modellenpagina (console.groq.com/docs/models); het aanbod wisselt vaak. | Eén regel aanpassen (na fase 1 alleen een env-variabele), blijft snel en gratis/goedkoop | Open modellen blijven zwakker in feitenkennis over muziek |
| **B. Claude (Anthropic API)** | Bijvoorbeeld `claude-haiku-5-5` (snel, goedkoop) of `claude-sonnet-5-5` (sterker). | Betere feitenkennis en betrouwbare structured output / tool use | Betaald per token, andere API-vorm (niet OpenAI-compatibel), Java-SDK of eigen HTTP-aanroep nodig |
| **C. OpenAI / Google Gemini** | Hun huidige modellen via hun API. | Ook sterk, OpenAI-compatibele API is bijna drop-in voor de huidige code | Betaald, aparte API-sleutel |
| **D. Lokaal (Ollama)** | Model op je eigen machine draaien, zoals in een eerdere versie van dit project. | Gratis, privé | Werkt niet op Railway zonder dure GPU, kleine modellen verzinnen nog meer |

**Advies:**
1. Doe eerst fase 1 (model configureerbaar + JSON-output) en fase 2 (verificatie via Spotify). Met verificatie wordt zelfs een middelmatig model bruikbaar, want verzonnen nummers worden eruit gefilterd.
2. Probeer daarna een paar modellen naast elkaar op dezelfde 5-10 testplaylists en kijk hoeveel van de kandidaten echt bestaan en hoe goed ze passen. Kies op basis daarvan, niet op gevoel.
3. Wil je de beste kwaliteit en vind je een paar cent per analyse geen probleem: optie B. Wil je gratis blijven: optie A met verificatie.

**Tip voor de architectuur:** maak een interface `MusicRecommender` (of `LlmClient`) met per provider een implementatie (`GroqClient`, `ClaudeClient`, ...). Kies de provider via configuratie. Dan is wisselen of vergelijken triviaal. Spring AI is een alternatief dat dit voor je doet, maar voegt een grote afhankelijkheid toe voor één aanroep.

---

## Aanbevolen volgorde

1. Fase 1 (snelle winst), inclusief model configureerbaar maken.
2. Spotify developer-app aanmaken.
3. Fase 3 (scraper → Web API): grootste verbetering in snelheid en betrouwbaarheid, en de Docker-image wordt veel lichter.
4. Fase 2 (verificatie): lost het verzinnen van nummers op.
5. Modellen vergelijken en kiezen.
6. Fase 4 en 5 naar smaak.

Fase 3 vóór fase 2, omdat je dan de Spotify-client al hebt staan en de verificatie er een kleine uitbreiding op is.

## Nieuwe configuratie (verwacht na de upgrade)

| Variabele | Doel |
|---|---|
| `SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET` | Spotify Web API (playlist ophalen + zoeken) |
| `LLM_PROVIDER` | Bijv. `groq` of `anthropic` |
| `LLM_MODEL` | Modelnaam bij die provider |
| `GROQ_API_KEY` / `ANTHROPIC_API_KEY` | Sleutel voor de gekozen provider |
| `CHROME_BIN` / `CHROMEDRIVER_BIN` | Vervallen zodra Selenium eruit is |

Werk `README.md` en `.env.example` bij zodra deze echt worden ingevoerd.
