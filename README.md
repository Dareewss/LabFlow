<<<<<<< HEAD
# LabFlow
=======
# LabFlow

LabFlow is a desktop laboratory management system built with JavaFX, Java 21, and SQLite, with an Android companion app for QR-based equipment workflows over the local network.

It is designed for school and university labs that need:
- equipment inventory
- borrowing and returns
- fault reports
- reservations
- maintenance tracking
- QR code workflows
- role-based access
- local AI-assisted automation

## Project Structure

```text
src/main/java/com/labflow
  api/        Local HTTP API for companion integration
  dao/        SQLite access layer
  model/      Domain models
  service/    Business logic
  ui/         JavaFX screens
  util/       Theme, session, export, QR, notifications, app directories

src/main/resources
  css/        Desktop themes
  fonts/      Montserrat
  logback.xml

android-companion/
  Native Android companion app
```

## Main Features

- multi-lab workflow with invite codes
- role system: `ADMIN`, `PROFESSOR`, `TECHNICIAN`, `STUDENT`, `GUEST`
- inventory with tags, containers, filters, and archive states
- borrowing and returns with overdue tracking
- fault reports and maintenance
- reservations and calendar support
- leaderboard and student activity features
- local AI helper with provider settings
- local HTTP API for Android companion
- dark/light themes and per-lab color palettes

## Requirements

### Desktop

- Windows
- Java 21 in `PATH`
- internet access only if Maven dependencies are not already cached

### Android Companion

- Android Studio or a compatible Android build environment
- Android device on the same Wi-Fi network as the desktop app

## Run The Desktop App

### Recommended on Windows

Use:

```bat
start.bat
```

That script:
- checks Java
- finds or downloads Maven if needed
- loads optional AI config from `ai.env`
- launches LabFlow

### Manual Maven Run

If you already have Maven installed:

```bat
mvn javafx:run
```

### AI Configuration

If you want AI Helper enabled, create:

```text
ai.env
```

based on:

```text
ai.env.example
```

The desktop app can also load AI settings from its own Settings view.

## Default Desktop Accounts

| Username | Password | Role |
| --- | --- | --- |
| admin | admin123 | ADMIN |
| professor | professor123 | PROFESSOR |
| technician | technician123 | TECHNICIAN |

## Demo Labs

The project seeds demo data for presentation, including a richer admin demo lab with equipment, borrow history, reservations, activity, and fault workflows.

## Local API

The desktop app starts a local API for companion integration.

Default:

```text
Port: 8080
API Key: LABFLOW_LOCAL_API_KEY
```

Health endpoint:

```http
GET http://localhost:8080/api/health
```

Protected endpoints require:

```http
Authorization: Bearer LABFLOW_LOCAL_API_KEY
```

## Run The Android Companion

The Android project is in:

```text
android-companion/
```

Build debug APK:

```bat
cd android-companion
gradlew.bat assembleDebug
```

Then:
1. start LabFlow desktop
2. find your PC IPv4 address with `ipconfig`
3. open the Android companion
4. enter PC IP, port `8080`, and API key
5. test connection
6. scan QR codes or use companion actions

## Build & Test

### Desktop

Compile:

```bat
mvn -q compile
```

Run tests:

```bat
mvn -q test
```

### Android

```bat
cd android-companion
gradlew.bat assembleDebug
```

## Contest Docs

Supporting documentation is included here:

- `COMPETITION_READINESS.md`
- `des.txt`
- `docs/INSTALL_AND_RUN.md`
- `docs/MARKET_ANALYSIS.md`
- `docs/PROJECT_DOCUMENTATION.md`
- `docs/EXTERNAL_RESOURCES.md`
- `docs/TESTING_AND_DEMO.md`
- `docs/PRESENTATION_SCRIPT.md`

## Notes

- runtime data is stored under the user profile, not inside the source tree
- generated local secrets such as `ai.env` are intentionally ignored by Git
- generated databases, logs, QR images, and signatures are not versioned
>>>>>>> f3cc9c3 (docs: prepare contest-ready project package)
