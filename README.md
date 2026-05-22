# LabFlow

LabFlow este o aplicație desktop pentru managementul laboratoarelor, realizată în JavaFX, Java 21 și SQLite, împreună cu o aplicație Android Companion pentru fluxuri mobile bazate pe coduri QR.

Aplicația este gândită pentru laboratoare școlare sau universitare care au nevoie de:
- inventar de echipamente
- evidența împrumuturilor și returnărilor
- rapoarte de defecțiuni
- rezervări
- mentenanță
- coduri QR
- acces pe roluri
- automatizări AI locale

## Structura proiectului

```text
src/main/java/com/labflow
  api/        API HTTP local pentru integrarea cu aplicația companion
  dao/        Acces la SQLite
  model/      Modele de domeniu
  service/    Logică de business
  ui/         Ecrane JavaFX
  util/       Teme, sesiune, export, QR, notificări, directoare aplicație

src/main/resources
  css/        Temele desktop
  fonts/      Fonturile Montserrat
  logback.xml

android-companion/
  Aplicația Android Companion
```

## Funcționalități principale

- mai multe laboratoare, fiecare cu cod de invitație
- sistem de roluri: `ADMIN`, `PROFESSOR`, `TECHNICIAN`, `STUDENT`, `GUEST`
- inventar cu tag-uri, containere, filtre și arhivare
- împrumuturi și returnări cu evidență overdue
- rapoarte de defecțiuni și mentenanță
- rezervări și calendar
- leaderboard și activitate studenți
- AI Helper cu provideri configurabili
- API local pentru companion
- dark/light theme și palete de culoare per laborator

## Cerințe

### Pentru aplicația desktop

- Windows
- Java 21 disponibil în `PATH`
- internet doar dacă dependențele Maven nu sunt deja descărcate

### Pentru aplicația Android Companion

- Android Studio sau un mediu compatibil de build Android
- telefon Android conectat în aceeași rețea Wi-Fi cu PC-ul

## Cum pornești aplicația desktop

### Varianta recomandată pe Windows

Folosește:

```bat
start.bat
```

Acest script:
- verifică Java
- caută sau descarcă Maven dacă lipsește
- încarcă opțional configurația AI din `ai.env`
- pornește LabFlow

### Pornire manuală cu Maven

Dacă ai deja Maven instalat:

```bat
mvn javafx:run
```

### Configurare AI

Dacă vrei să activezi AI Helper, creează fișierul:

```text
ai.env
```

pe baza fișierului:

```text
ai.env.example
```

Aplicația desktop poate încărca și setări AI direct din ecranul `Settings`.

## Conturi implicite desktop

| Username | Parolă | Rol |
| --- | --- | --- |
| admin | admin123 | ADMIN |
| professor | professor123 | PROFESSOR |
| technician | technician123 | TECHNICIAN |

## Laboratoare demo

Proiectul creează date demo pentru prezentare, inclusiv un laborator bogat pentru contul de admin, cu inventar, împrumuturi, rezervări, activitate și defecțiuni.

## API local

Aplicația desktop pornește un API local folosit de companion.

Valori implicite:

```text
Port: 8080
API Key: LABFLOW_LOCAL_API_KEY
```

Endpoint de health:

```http
GET http://localhost:8080/api/health
```

Pentru endpointurile protejate se folosește:

```http
Authorization: Bearer LABFLOW_LOCAL_API_KEY
```

## Cum pornești aplicația Android Companion

Proiectul Android este în:

```text
android-companion/
```

Pentru a genera APK-ul debug:

```bat
cd android-companion
gradlew.bat assembleDebug
```

După aceea:
1. pornești LabFlow Desktop
2. afli IP-ul PC-ului cu `ipconfig`
3. deschizi aplicația Android Companion
4. introduci IP-ul PC-ului, portul `8080` și cheia API
5. testezi conexiunea
6. scanezi codurile QR sau folosești acțiunile mobile

## Build și testare

### Desktop

Compile:

```bat
mvn -q compile
```

Teste:

```bat
mvn -q test
```

### Android

```bat
cd android-companion
gradlew.bat assembleDebug
```

## Documentație pentru concurs

Materialele de concurs sunt incluse aici:

- `COMPETITION_READINESS.md`
- `des.txt`
- `docs/INSTALL_AND_RUN.md`
- `docs/MARKET_ANALYSIS.md`
- `docs/PROJECT_DOCUMENTATION.md`
- `docs/EXTERNAL_RESOURCES.md`
- `docs/TESTING_AND_DEMO.md`
- `docs/PRESENTATION_SCRIPT.md`

## Observații

- datele de runtime sunt salvate în profilul utilizatorului, nu în folderul sursă
- secretele locale precum `ai.env` sunt ignorate de Git
- baza de date, logurile, codurile QR și semnăturile generate nu sunt versionate
