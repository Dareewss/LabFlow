# LabFlow — Testing and Demo Checklist

## Build checks

Run:

```bash
mvn -q compile
mvn -q test
```

## Functional demo flow

1. Login as `admin`.
2. Open `Presentation Demo Lab`.
3. Show Dashboard:
   - equipment analytics
   - notifications
   - risky equipment
   - recommendations
4. Open Inventory:
   - containers
   - consumables
   - archived / retired examples
   - QR labels
5. Open Borrowing:
   - overdue borrow
   - returned defect example
   - normal returned example
6. Open Fault Reports:
   - open
   - in progress
   - resolved
   - rejected
7. Open Reservations / Calendar:
   - pending
   - approved
   - rejected
   - completed
   - cancelled
8. Open Leaderboard:
   - student points
   - history
9. Open Student Activity:
   - pick a student
   - export PDF
10. Show AI Helper:
   - explain that AI is optional and key-based
11. Show Android companion workflow if available

## Non-functional checks

- app starts normally
- no blocking errors during navigation
- dark/light themes work
- multiple labs remain isolated
- role-restricted views behave correctly

## Backup demo

Open Settings and show:

- backup database
- restore database
- open backup folder
