# Groq API Setup

## Stappen om Groq te configureren:

### 1. Verkrijg een Groq API Key
- Ga naar: https://console.groq.com/keys
- Log in of maak een account aan
- Klik op "Create API Key"
- Kopieer je API key

### 2. Stel de Environment Variable in

#### Op Windows (PowerShell):
```powershell
$env:GROQ_API_KEY="jouw_api_key_hier"
```

#### Of permanent via System Properties:
1. Zoek naar "Omgevingsvariabelen" in Windows
2. Klik op "Omgevingsvariabelen bewerken voor uw account"
3. Klik op "Nieuw"
4. Variabelenaam: `GROQ_API_KEY`
5. Waarde: jouw API key
6. Klik op OK

#### Op Linux/Mac:
```bash
export GROQ_API_KEY="jouw_api_key_hier"
```

Of voeg toe aan je `~/.bashrc` of `~/.zshrc`:
```bash
echo 'export GROQ_API_KEY="jouw_api_key_hier"' >> ~/.bashrc
source ~/.bashrc
```

### 3. Start de applicatie
```bash
./mvnw spring-boot:run
```

## Beschikbare Groq Modellen

De applicatie gebruikt standaard `mixtral-8x7b-32768`. Je kunt ook deze modellen gebruiken:
- `mixtral-8x7b-32768` - Snelle en capabele Mixtral model
- `llama-3.3-70b-versatile` - Zeer krachtig model voor complexe taken
- `llama-3.1-8b-instant` - Ultra-snel model voor simpele taken
- `gemma2-9b-it` - Google's Gemma model

Om het model te wijzigen, pas de `model` property aan in `SimpleOllamaService.java` regel 32.

## Troubleshooting

### Error: "GROQ_API_KEY environment variable is not set"
- Controleer of de environment variable correct is ingesteld
- Herstart je terminal/IDE na het instellen van de variable
- Op Windows: herstart PowerShell of je IDE

### Error: "401 Unauthorized"
- Controleer of je API key correct is
- Zorg dat je API key niet verlopen is

### Error: "429 Rate Limit"
- Groq heeft rate limits. Wacht even en probeer opnieuw
- Gratis tier heeft lagere limits dan betaalde tiers
