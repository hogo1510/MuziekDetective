# Werklog

Nieuwste bovenaan. Noteer per wijziging: wat, waarom, en eventuele beslissingen of afwegingen.

## 2026-11-26: ChromeDriver-versiefout lokaal opgelost

- Fout: `session not created: This version of ChromeDriver only supports Chrome version 158` terwijl Chromium 156 (Fedora) geïnstalleerd is.
- Oorzaak: WebDriverManager 5.6.0 herkende de Chromium-versie niet en downloadde de nieuwste chromedriver (158).
- Oplossing: WebDriverManager verwijderd (uit `pom.xml` en de scraper). Selenium Manager (ingebouwd in Selenium) regelt nu lokaal de driver. De scraper geeft het browserpad expliciet mee (`CHROME_BIN`, anders de eerste van `/usr/bin/chromium-browser`, `chromium`, `google-chrome`, `google-chrome-stable`), zodat de driver bij precies die versie past.
- Docker/Railway ongewijzigd: daar wijzen `CHROME_BIN` en `CHROMEDRIVER_BIN` nog steeds naar de geïnstalleerde Chromium en driver.
- Getest: headless Chromium start met chromedriver 156.0.8078.80, `./mvnw test` slaagt.

## 2026-11-26: AI-model gewisseld naar Claude Haiku 5.5

- `GroqService` vervangen door `ClaudeService`: roept nu `claude-haiku-5-5` aan via de officiële Anthropic Java-SDK (`com.anthropic:anthropic-java` 3.3.0, toegevoegd aan `pom.xml`).
- Waarom: Groq had `llama-3.3-70b-versatile` uitgeschakeld voor het free/developer-tier (bekend probleem #0), en Claude heeft betere feitenkennis over muziek.
- API-sleutel heet nu `ANTHROPIC_API_KEY` (env-variabele, of `.env` als terugval). `.env.example` aangepast; een lege `.env` aangemaakt om de sleutel in te zetten (staat in `.gitignore`). `GROQ_API_KEY` wordt niet meer gebruikt.
- `temperature` weggelaten: Haiku 5.5 geeft een fout bij een niet-standaardwaarde. Effort expliciet op `medium` gezet.
- Prompt en antwoordformaat (ONTDEKKING / STIJL / REDEN / VOOR WIE) ongewijzigd, op één regel na ("zonder extra tekst ervoor of erna"). Gestructureerde JSON-output staat nog open (verbeterplan fase 1).
- Getest: compileert, `./mvnw test` slaagt. Nog geen echte aanroep naar Claude gedaan (geen sleutel beschikbaar).
- Railway: `ANTHROPIC_API_KEY` moet daar als variabele worden ingesteld.

## 2026-11-26: Project weer opgepakt, verbeterplan geschreven

- `docs/heropstart-verbeterplan.md` toegevoegd: gefaseerd plan om het project te vernieuwen (snelle winst, verificatie van aanbevelingen via Spotify, scraper vervangen door de Spotify Web API, codekwaliteit, nieuwe features) plus een vergelijking van AI-modellen.
- Waarom: bij het weer oppakken van het project een duidelijke volgorde hebben in plaats van losse ideeën.
- Nog geen codewijzigingen.
- Modelkeuze uitgezocht. `llama-3.3-70b-versatile` blijkt door Groq uitgefaseerd (sinds 2026-08-16 uit voor het free/developer-tier); toegevoegd als bekend probleem #0. Advies: eerst `openai/gpt-oss-120b` (Groq), daarna `claude-haiku-5-5` (Anthropic).

## 2026-11-22: Documentatie opgezet

- `README.md` toegevoegd: wat het project doet, architectuur, draaien en configuratie.
- `docs/bekende-problemen.md` toegevoegd: inventaris van beperkingen en bugs na code-review, waaronder de oorzaak van de ~28-nummers-limiet van de scraper.
- Volgende stappen: AI-model upgraden en scraper repareren of vervangen (zie bekende problemen #1 en #2).

## Eerdere geschiedenis (uit git)

| Commit | Wijziging |
|---|---|
| `b9f54a4` | Rate limiter: 5 analyses per IP per 10 minuten |
| `508e9b2` | Prompt-regel tegen verzonnen artiesten |
| `cd84213`, `c894ca2` | Dockerfile voor Railway (Debian + Chromium) |
| `c1523d3` | Redesign van de frontend; Ollama vervangen door Groq |
| `1f6ed5a` | Eerste frontend + backend-aanpassingen |
| `4190fe6` | Ollama/Mistral-client toegevoegd |
| `600ca52` | Eerste versie: Selenium-scraper voor Spotify-playlists |
