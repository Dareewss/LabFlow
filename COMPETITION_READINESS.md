# LabFlow — Competition Readiness

This checklist is aligned with the InfoEducație Software Utilitar regulation from `utilitar.pdf`.

## What is already covered

- Clear target audience: schools, labs, teachers, technicians, students.
- Real utility: equipment inventory, borrowing, maintenance, fault reports, reservations, QR flows, analytics.
- Architecture is structured: `UI -> Service -> DAO -> SQLite`.
- Modern desktop UI with dark/light themes and per-lab palettes.
- Role-based access and local authentication.
- Local API and Android companion app.
- Demo presentation lab with pre-seeded realistic data.
- Build verification:
  - `mvn -q compile`
  - `mvn -q test`

## What was added for the contest pack

- `README.md` already explains the app and the main setup path.
- `des.txt` already contains a technical audit and architecture summary.
- `docs/INSTALL_AND_RUN.md`
- `docs/MARKET_ANALYSIS.md`
- `docs/PROJECT_DOCUMENTATION.md`
- `docs/EXTERNAL_RESOURCES.md`
- `docs/TESTING_AND_DEMO.md`
- `docs/PRESENTATION_SCRIPT.md`
- `ai.env.example`
- `start.bat` no longer ships with a hardcoded AI key.

## Important manual items before submission

These are contest-relevant and should be handled honestly:

1. Use a real version-control repository with visible history.
2. Prepare a short oral presentation based on `docs/PRESENTATION_SCRIPT.md`.
3. Add real testimonials from teachers/students if you have them.
4. Declare all external libraries and assets in `docs/EXTERNAL_RESOURCES.md`.
5. Keep the final archive runnable offline except for AI features.

## Recommended submission package

- source code
- `pom.xml`
- `start.bat`
- `README.md`
- `des.txt`
- `docs/`
- Android companion source in `android-companion/`
- optional demo screenshots / slides
