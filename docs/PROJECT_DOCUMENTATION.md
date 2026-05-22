# LabFlow — Project Documentation

## Problem statement

LabFlow solves the operational chaos of managing equipment in educational laboratories. Schools need a tool that can track inventory, borrowing, maintenance, defect reports, reservations, and user roles in one coherent workflow.

## Proposed solution

LabFlow is a JavaFX desktop application with a SQLite database and a companion Android app. It centralizes laboratory operations while keeping the installation lightweight and suitable for a school environment.

## Target audience

- teachers / professors
- technicians
- school admins
- students
- guest users in controlled contexts

## Main functionality

- multi-lab management
- role-based login and member management
- equipment inventory with QR generation
- containers and kits for organization
- borrowing and returns
- defect reporting and resolution workflow
- reservations and calendar
- maintenance planning
- consumables and stock movements
- activity log and notifications
- dashboard analytics and AI weekly report
- Android companion integration

## Architecture

Main architecture:

```text
UI -> Service -> DAO -> SQLite
```

- UI: JavaFX views and dialogs
- Service: business rules
- DAO: SQL and persistence
- SQLite: local data storage

## Distinctive strengths

- local-first deployment
- educational-lab-specific workflow
- QR + mobile companion flow
- advanced dashboard with risk, health, and recommendations
- polished presentation-ready interface

## Installation

See `docs/INSTALL_AND_RUN.md`.

## Technology choices

### Why JavaFX

JavaFX provides a desktop-native user interface that is fast to iterate on, visually flexible, and suitable for rich dashboard-like workflows. It is a strong fit for a school computer lab where local deployment matters more than public cloud hosting.

### Why SQLite

SQLite keeps the system easy to deploy and maintain. Schools can run the system locally without needing a separate database server, while still keeping structured relational data for labs, users, equipment, and workflows.

### Why QR + Android companion

QR codes reduce friction in real use. A technician or teacher can scan equipment quickly instead of searching manually, which improves both speed and accuracy during borrow/return operations.

## Author perspective

The core idea behind LabFlow is practical: school labs often have real equipment-management problems, but they rarely have the budget or infrastructure for enterprise systems. A lightweight but serious local-first tool is therefore useful both educationally and operationally. For example, a robotics lab can use it to track kits, borrowed boards, reported faults, and maintenance status during a competition season.

## Roadmap

- onboarding tutorial and shipping polish
- stronger admin preferences and startup safety
- more reporting and print flows
- further companion/desktop design alignment

## Testimonials

Add real testimonials here before final submission if available.

Example format:

> “LabFlow made it easier to see who borrowed which equipment before practical sessions.”

> “The QR flow is much faster than checking a spreadsheet.”
