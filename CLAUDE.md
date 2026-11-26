# MuziekDetective

Spring Boot-app die een Spotify-playlist scrapet en via een LLM één nieuw nummer aanraadt. Zie `README.md` voor de architectuur.

## Documentatie bijhouden (verplicht)

Bij elke wijziging aan code, configuratie of deployment:
- Voeg een entry toe bovenaan `docs/werklog.md` (datum, wat, waarom).
- Werk `README.md` bij als architectuur, setup of configuratie verandert.
- Werk `docs/bekende-problemen.md` bij: verwijder of markeer opgeloste punten, voeg nieuwe toe.

Documentatie is in het Nederlands.

## Commando's

- Draaien: `./mvnw spring-boot:run` (vereist `ANTHROPIC_API_KEY` in env of `.env`)
- Testen: `./mvnw test`
