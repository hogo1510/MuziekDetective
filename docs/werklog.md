# Werklog

Nieuwste bovenaan. Noteer per wijziging: wat, waarom, en eventuele beslissingen of afwegingen.

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
