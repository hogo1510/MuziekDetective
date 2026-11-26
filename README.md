# MuziekDetective

Een webapp die een **Spotify-playlist analyseert** en met AI **één nieuw nummer aanraadt** dat bij de muzieksmaak van die playlist past.

De gebruiker plakt een playlist-URL, de server haalt de tracklijst op met een headless Chrome-browser, stuurt die naar Claude (Anthropic) en toont de aanbeveling.

## Hoe het werkt

```
Browser (index.html)
   │  POST /analyze  (playlistUrl)
   ▼
MusicController ──► RateLimiter        max 5 analyses per IP per 10 minuten (instelbaar)
   │
   ├─► playlistDataScraper            Selenium + headless Chromium opent de playlist-pagina
   │      └─► JSON: { total_tracks, tracks: [{position, title, artist}] }
   │
   ├─► ClaudeService                  stuurt de JSON + prompt naar Claude (claude-haiku-5-5)
   │      └─► JSON (structured output) ──► tekst: ONTDEKKING / STIJL / REDEN / VOOR WIE
   │
   └─► redirect:/ met flash-attributen ──► index.html toont het resultaat
```

| Bestand | Rol |
|---|---|
| `MuziekDetectiveApplication.java` | Spring Boot entrypoint |
| `controller/MusicController.java` | Routes `/` en `/analyze` (POST); krijgt scraper, ClaudeService en rate limiter geïnjecteerd |
| `scraper/playlistDataScraper.java` | Scrapet titels en artiesten uit de Spotify-webpagina met Selenium |
| `service/ClaudeService.java` | Bouwt de prompt en roept de Claude Messages API aan via de Anthropic Java-SDK |
| `service/Ontdekking.java` | Record met het JSON-schema dat Claude moet invullen (titel, artiest, stijl, reden, voorWie) |
| `service/RateLimiter.java` | Sliding-window rate limiter per IP, in geheugen; ruimt verlopen IP's eens per venster op |
| `service/RateLimitConfig.java` | Maakt de rate limiter aan met de waarden uit `application.properties` |
| `service/ClientIpResolver.java` | Bepaalt het client-IP (achter een proxy het laatste `X-Forwarded-For`-item) |
| `templates/index.html` | Thymeleaf-pagina: formulier, voortgangsanimatie en resultaat |

## Tech stack

- Java 21, Spring Boot 3.5 (web + Thymeleaf)
- Selenium 4 + Selenium Manager (headless Chromium)
- Gson voor JSON
- SLF4J/Logback voor logging (niveau voor de app staat op DEBUG in `application.properties`)
- Anthropic Claude API via de officiële Java-SDK (`com.anthropic:anthropic-java`), model `claude-haiku-5-5`
- Docker (Debian + Chromium), gehost op Railway

## Lokaal draaien

Vereisten: Java 21 en Google Chrome/Chromium geïnstalleerd.

```bash
cp .env.example .env        # vul ANTHROPIC_API_KEY in
./mvnw spring-boot:run
```

Open daarna http://localhost:8080.

Testen: `./mvnw test`. Unit tests voor de rate limiter, IP-bepaling, prompt en JSON-omzetting, plus MockMvc-tests voor de controller (scraper en Claude worden gemockt, er gaan geen echte verzoeken naar buiten).

De API-sleutel wordt gelezen uit de omgevingsvariabele `ANTHROPIC_API_KEY`, met als terugval het `.env`-bestand in de werkmap.

## Docker / Railway

```bash
docker build -t muziekdetective .
docker run -p 8080:8080 -e ANTHROPIC_API_KEY=... muziekdetective
```

In de container wijzen `CHROME_BIN` en `CHROMEDRIVER_BIN` naar de vooraf geïnstalleerde Chromium. Lokaal (zonder die variabelen) zoekt de scraper zelf Chromium/Chrome in `/usr/bin` en downloadt Selenium Manager een chromedriver die bij die versie past.

## Configuratie

| Variabele | Verplicht | Doel |
|---|---|---|
| `ANTHROPIC_API_KEY` | ja | Sleutel voor de Claude API (console.anthropic.com) |
| `CHROME_BIN` | nee | Pad naar de Chrome/Chromium binary (gezet in Docker; lokaal automatisch gezocht) |
| `CHROMEDRIVER_BIN` | nee | Pad naar chromedriver (gezet in Docker) |
| `TRUST_PROXY` | nee | `true` achter een vertrouwde reverse proxy (gezet in Docker voor Railway): dan telt het laatste `X-Forwarded-For`-item als client-IP. Standaard `false`: de header wordt genegeerd |

In `application.properties` staan verder `muziekdetective.ratelimit.max-requests` (standaard 5) en `muziekdetective.ratelimit.window` (standaard `10m`).

## Meer documentatie

- [docs/bekende-problemen.md](docs/bekende-problemen.md): bekende beperkingen en bugs, met oorzaak
- [docs/werklog.md](docs/werklog.md): logboek van wijzigingen en beslissingen
- [docs/heropstart-verbeterplan.md](docs/heropstart-verbeterplan.md): plan om het project te vernieuwen en te verbeteren (incl. keuze AI-model)
