# MuziekDetective

Een webapp die een **Spotify-playlist analyseert** en met AI **één nieuw nummer aanraadt** dat bij de muzieksmaak van die playlist past.

De gebruiker plakt een playlist-URL, de server haalt de tracklijst op met een headless Chrome-browser, stuurt die naar een LLM (via Groq) en toont de aanbeveling.

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
   ├─► GroqService                    stuurt de JSON + prompt naar Groq (llama-3.3-70b-versatile)
   │      └─► tekst: ONTDEKKING / STIJL / REDEN / VOOR WIE
   │
   └─► redirect:/ met flash-attributen ──► index.html toont het resultaat
```

| Bestand | Rol |
|---|---|
| `MuziekDetectiveApplication.java` | Spring Boot entrypoint |
| `controller/MusicController.java` | Routes `/`, `/analyze` (POST) en `/progress` |
| `scraper/playlistDataScraper.java` | Scrapet titels en artiesten uit de Spotify-webpagina met Selenium |
| `service/GroqService.java` | Bouwt de prompt en roept de Groq chat-completions API aan |
| `service/RateLimiter.java` | Sliding-window rate limiter per IP, in geheugen |
| `templates/index.html` | Thymeleaf-pagina: formulier, voortgangsbalk en resultaat |

## Tech stack

- Java 21, Spring Boot 3.5 (web + Thymeleaf)
- Selenium 4 + WebDriverManager (headless Chromium)
- Gson voor JSON
- Groq API (OpenAI-compatibel), model `llama-3.3-70b-versatile`
- Docker (Debian + Chromium), gehost op Railway

## Lokaal draaien

Vereisten: Java 21 en Google Chrome/Chromium geïnstalleerd.

```bash
cp .env.example .env        # vul GROQ_API_KEY in
./mvnw spring-boot:run
```

Open daarna http://localhost:8080.

De API-sleutel wordt gelezen uit de omgevingsvariabele `GROQ_API_KEY`, met als terugval het `.env`-bestand in de werkmap.

## Docker / Railway

```bash
docker build -t muziekdetective .
docker run -p 8080:8080 -e GROQ_API_KEY=... muziekdetective
```

In de container wijzen `CHROME_BIN` en `CHROMEDRIVER_BIN` naar de vooraf geïnstalleerde Chromium. Lokaal (zonder die variabelen) downloadt WebDriverManager zelf de juiste driver.

## Configuratie

| Variabele | Verplicht | Doel |
|---|---|---|
| `GROQ_API_KEY` | ja | Sleutel voor de Groq API |
| `CHROME_BIN` | nee | Pad naar de Chrome/Chromium binary (gezet in Docker) |
| `CHROMEDRIVER_BIN` | nee | Pad naar chromedriver (gezet in Docker) |

## Meer documentatie

- [docs/bekende-problemen.md](docs/bekende-problemen.md): bekende beperkingen en bugs, met oorzaak
- [docs/werklog.md](docs/werklog.md): logboek van wijzigingen en beslissingen
