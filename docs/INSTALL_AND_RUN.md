# LabFlow — Install and Run Guide

## Requirements

- Windows
- Java 21 in `PATH`
- Internet connection for first Maven dependency download
- Optional: AI API key if you want the AI Helper enabled

## Desktop app

1. Open the project folder.
2. Put your AI key in an `ai.env` file based on `ai.env.example` if you want AI enabled.
3. Run:

```bat
start.bat
```

The application will:

- verify Java
- locate or download Maven 3.9.5
- start LabFlow with JavaFX
- create/update `labflow.db`

## Alternative run

```bash
mvn javafx:run
```

## Default demo accounts

| Username | Password | Role |
| --- | --- | --- |
| admin | admin123 | ADMIN |
| professor | professor123 | PROFESSOR |
| technician | technician123 | TECHNICIAN |
| student.alpha | student123 | STUDENT |
| student.beta | student123 | STUDENT |
| student.gamma | student123 | STUDENT |
| guest.demo | guest123 | GUEST |

## Demo laboratory

Use the seeded lab:

- `Presentation Demo Lab`

It contains:

- active and overdue borrows
- reservations in multiple states
- maintenance examples
- low stock consumables
- fault reports in multiple states
- leaderboard points
- notifications
- activity logs

## Android companion

The mobile companion project is in:

```text
android-companion/
```

Build it separately with Gradle/Android Studio.
