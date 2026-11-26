# MuziekDetective

Een webapp die een **Spotify-playlist analyseert** en met AI **één nieuw nummer aanraadt** dat bij de muzieksmaak van die playlist past.

De gebruiker plakt een playlist-URL, de server haalt de tracklijst op met een headless Chrome-browser, stuurt die naar Claude (Anthropic) en toont de aanbeveling.

## Hoe het werkt

```
Browser (index.html)
   │  POST /analyze  (playlistUrl)
   ▼
MusicController ──► RateLimiter        max 5 analyses per IP per 10 minuten
   │
   ├─► playlistDataScraper            Selenium + headless Chromium opent de playlist-pagina
   │      └─► JSON: { total_tracks, tracks: [{position, title, artist}] }
   │
   ├─► ClaudeService                  stuurt de JSON + prompt naar Claude (claude-haiku-5-5)
   │      └─► tekst: ONTDEKKING / STIJL / REDEN / VOOR WIE
   │
   └─► redirect:/ met flash-attributen ──► index.html toont het resultaat
```

| Bestand | Rol |
|---|---|
| `MuziekDetectiveApplication.java` | Spring Boot entrypoint |
| `controller/MusicController.java` | Routes `/`, `/analyze` (POST) en `/progress` |
| `scraper/playlistDataScraper.java` | Scrapet titels en artiesten uit de Spotify-webpagina met Selenium |
| `service/ClaudeService.java` | Bouwt de prompt en roept de Claude Messages API aan via de Anthropic Java-SDK |
| `service/RateLimiter.java` | Sliding-window rate limiter per IP, in geheugen |
| `templates/index.html` | Thymeleaf-pagina: formulier, voortgangsbalk en resultaat |

## Tech stack

- Java 21, Spring Boot 3.5 (web + Thymeleaf)
- Selenium 4 + Selenium Manager (headless Chromium)
- Gson voor JSON
- Anthropic Claude API via de officiële Java-SDK (`com.anthropic:anthropic-java`), model `claude-haiku-5-5`
- Docker (Debian + Chromium), gehost op Railway

## Lokaal draaien

Vereisten: Java 21 en Google Chrome/Chromium geïnstalleerd.

```bash
cp .env.example .env        # vul ANTHROPIC_API_KEY in
./mvnw spring-boot:run
```

Open daarna http://localhost:8080.

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

## Meer documentatie

- [docs/bekende-problemen.md](docs/bekende-problemen.md): bekende beperkingen en bugs, met oorzaak
- [docs/werklog.md](docs/werklog.md): logboek van wijzigingen en beslissingen
- [docs/heropstart-verbeterplan.md](docs/heropstart-verbeterplan.md): plan om het project te vernieuwen en te verbeteren (incl. keuze AI-model)
