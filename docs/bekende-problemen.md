# Bekende problemen

Stand van zaken op 2026-11-22 (commit `b9f54a4`). Werk dit bestand bij zodra iets is opgelost of er iets nieuws gevonden wordt.

## 0. ~~AI-model uitgefaseerd~~ (opgelost 2026-11-26)

Opgelost door over te stappen op Claude (`claude-haiku-5-5`), zie werklog. Oorspronkelijke beschrijving:

Groq heeft `llama-3.3-70b-versatile` uitgefaseerd: voor het free/developer-tier is het sinds 2026-08-16 uitgeschakeld. De aanroep faalt dan waarschijnlijk, en door probleem #3 ziet de gebruiker die fout als "succes". Groq noemt `openai/gpt-oss-120b` als vervanger. Zie de modelkeuze in [heropstart-verbeterplan.md](heropstart-verbeterplan.md#ai-model-kiezen).

## 1. Scraper haalt maar ~25-30 nummers op

**Bestand:** `scraper/playlistDataScraper.java` (`scrollToBottom`, regel ~148)

Spotify's webspeler gebruikt een **gevirtualiseerde lijst**: alleen de rijen die (bijna) in beeld zijn bestaan in de DOM. Daarnaast:

- `scrollToBottom` scrollt `window`, maar op Spotify scrollt een binnenste container, niet het venster. Het scrollen doet dus niets en alleen de eerste batch rijen wordt ooit geladen.
- Zelfs als het scrollen wel werkt, verwijdert Spotify rijen die uit beeld raken. Alle rijen pas ná het scrollen ophalen geeft dan alleen de laatste batch.

**Mogelijke oplossingen:**
- Beste: de **Spotify Web API** gebruiken (`GET /playlists/{id}/items`, gepagineerd per 100). Geen browser nodig, sneller en betrouwbaarder. Vereist een Spotify developer-app (client credentials). Let op: Spotify heeft de API voor nieuwe apps beperkt, onder andere geen toegang meer tot Spotify's eigen redactionele/algoritmische playlists. Gebruikersplaylists werken wel.
- Als Selenium moet blijven: de juiste scroll-container scrollen en **tijdens** het scrollen rijen verzamelen (dedupliceren op `aria-rowindex` of positie) tot het aantal niet meer groeit.

## 2. AI-aanbevelingen zijn matig of verzonnen

**Bestand:** `service/ClaudeService.java`

- Het oude model `llama-3.3-70b-versatile` verzon regelmatig titel/artiest-combinaties (daarom staat er een "verzin niets"-regel in de prompt). Sinds 2026-11-26 draait de app op `claude-haiku-5-5`; nog niet getest hoe vaak dat model nummers verzint.
- Er is geen controle of het aangeraden nummer echt bestaat, en ook niet of het niet al in de playlist staat.

**Mogelijke oplossingen:** sterker model, en de suggestie verifiëren via een zoekopdracht (bijv. Spotify Search API). Laat het model meerdere kandidaten geven en kies de eerste die echt bestaat en niet in de playlist staat.

## 3. Fouten worden als succes getoond

- De scraper vangt alle exceptions af en geeft dan een lege lijst terug (`total_tracks: 0`). De AI krijgt een lege playlist en raadt dan maar wat.
- `ClaudeService` vangt fouten af en geeft een foutmelding terug als gewone tekst. De controller zet dan `success=true`, dus de UI toont "Playlist succesvol geanalyseerd" boven een foutmelding.

## 4. ~~Nep-tekst als het antwoordformaat afwijkt~~ (opgelost 2026-11-26)

Claude geeft nu JSON terug volgens een vast schema (structured outputs, record `Ontdekking`); de nep-zinnen zijn verwijderd. Oorspronkelijke beschrijving:


`formatDiscoveryResponse` / `extractValue` vullen bij een afwijkend AI-antwoord **hardgecodeerde zinnen** in ("Dit nummer past perfect bij je muzieksmaak..."). De echte inhoud van het antwoord gaat dan verloren. Beter: gestructureerde output (JSON) vragen aan het model.

## 5. ~~`/progress`-endpoint wordt niet gebruikt~~ (opgelost 2026-11-26)

Endpoint, `progressUpdates` en `sessionId` zijn verwijderd. De voortgangsbalk in `index.html` is nog steeds alleen een animatie op tijd; echte voortgang (bijv. via SSE) is een idee voor later. Oorspronkelijke beschrijving:


De controller maakt een `sessionId` aan, maar stuurt die nooit naar de browser. De voortgangsbalk in `index.html` is een **getimede animatie** en volgt het echte werk niet. Het endpoint en de `progressUpdates`-map zijn in feite dode code.

## 6. Traag: tot 30 s wachten op een cookie-knop (deels opgelost 2026-11-26)

De cookieknop krijgt nu maximaal 3 s. Nog over: 5 × 2 s scrollen (dat bovendien niet werkt, zie #1) en per verzoek een nieuwe Chrome. Oorspronkelijke beschrijving:


De scraper wacht tot 30 seconden op een "Accept"-cookieknop. In headless/niet-EU-omgevingen verschijnt die vaak niet, dus elke analyse verliest dan die tijd. Daarna nog eens 5 × 2 s scrollen. Ook wordt per verzoek een nieuwe Chrome gestart.

## 7. ~~Rate limiter~~ (opgelost 2026-11-26)

IP komt nu uit het laatste `X-Forwarded-For`-item (alleen met `TRUST_PROXY=true`), en verlopen IP's worden opgeruimd. Oorspronkelijke beschrijving:


- Gebruikt het eerste IP in `X-Forwarded-For`. Dat deel kan een client zelf meesturen, dus de limiet is te omzeilen. Achter Railway's proxy is het laatste door de proxy toegevoegde IP betrouwbaarder.
- De map met tijdstempels wordt nooit opgeruimd en groeit dus met elk nieuw IP.

## 8. Overig

- Klassenaam `playlistDataScraper` volgt de Java-conventie niet (`PlaylistDataScraper`).
- ~~Services worden per verzoek met `new` gemaakt~~ (opgelost 2026-11-26: Spring beans).
- ~~Logging gaat via `System.out`/`System.err`~~ (opgelost 2026-11-26: SLF4J).
- ~~Er zijn geen echte tests~~ (opgelost 2026-11-26: 21 tests). Nog niet getest: de Selenium-scraper zelf (die heeft een echte browser en Spotify nodig) en de echte Claude-aanroep.
