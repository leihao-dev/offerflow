# OfferFlow Design Spec

**Date:** 2026-07-11  
**Status:** Approved (2026-07-11)  
**Author:** Brainstorming session with Ray

## 1. Problem & Goal

### Problem

Job seekers juggle four recurring pains:

| ID | Pain | Symptom |
|----|------|---------|
| A1 | Pipeline chaos | Lost track of applications and follow-ups |
| A2 | Weak interview debrief | Same mistakes repeat; notes scattered |
| A3 | Unstructured prep | No clear plan per role |
| A4 | Fragmented intel | JDs, company notes, referrals in many places |

### Goal (MVP — few days)

Build **OfferFlow**, a local Spring Boot web app that solves **A1 + A2** as the daily core, with **A3 + A4** reserved as lightweight fields for future phases.

### Success Criteria

- User can add an application, move it through stages, and set follow-up dates.
- User can attach interview debrief notes to any application.
- Dashboard shows active count, upcoming interviews, overdue follow-ups.
- Runs locally with one command: `./gradlew bootRun` (Windows: `gradlew.bat bootRun`).
- Satisfies CC week assignment: README, JOURNAL, `.claude/`, multi-day git history.

### Non-Goals (MVP)

- User authentication / OAuth
- Job board scraping or API integrations
- AI-generated interview answers
- Mobile app or cloud sync
- Payment / subscription features

---

## 2. Product Vision (Long-Term)

```
Phase 0 (now)     Local web MVP — pipeline + debrief
Phase 1           User accounts + MySQL + email/calendar reminders
Phase 2           Mobile app (React Native / Flutter) sharing same API
Phase 3           Community debrief templates + question bank (A2 moat)
Phase 4           Skill gap analysis per JD + study plans (A3)
Phase 5           Company dossier + referral tracking (A4)
Phase 6           Freemium: free tier cap, paid unlimited + AI debrief summary
```

**ToC wedge:** "Never lose track of a job application again" — expands into full job-search OS.

---

## 3. Architecture

### Stack

| Layer | Choice |
|-------|--------|
| Runtime | Java 17+ |
| Framework | Spring Boot 3.3.x |
| Web | Spring Web MVC + Thymeleaf |
| Persistence | Spring Data JPA |
| Database | H2 file mode (persists across restarts) |
| Validation | Jakarta Bean Validation |
| Build | Gradle (Groovy DSL) + Gradle Wrapper |
| CSS | Minimal custom CSS in `static/css/app.css` |

### Package Structure

```
com.offerflow
├── OfferFlowApplication.java
├── model/
│   ├── ApplicationStage.java    (enum)
│   ├── JobApplication.java
│   └── InterviewNote.java
├── repository/
│   ├── JobApplicationRepository.java
│   └── InterviewNoteRepository.java
├── service/
│   ├── JobApplicationService.java
│   ├── InterviewNoteService.java
│   ├── DashboardService.java
│   └── MarkdownExportService.java   (Phase 0 optional, low priority)
├── dto/
│   ├── ApplicationForm.java
│   └── InterviewNoteForm.java
├── controller/
│   ├── DashboardController.java
│   ├── ApplicationController.java
│   └── InterviewController.java
└── config/
    └── DataInitializer.java         (optional demo seed, disabled by default)
```

### Request Flow

```
Browser
  → Controller (MVC)
    → Service (business rules)
      → Repository (JPA)
        → H2 database
  ← Thymeleaf template + model attributes
```

No separate frontend build. All UI server-rendered.

---

## 4. Data Model

### ApplicationStage (enum)

```
APPLIED, SCREENING, TECH_INTERVIEW, FINAL_INTERVIEW, OFFER, REJECTED, WITHDRAWN
```

### JobApplication

| Field | Type | Notes |
|-------|------|-------|
| id | Long | PK |
| companyName | String | Required, max 200 |
| positionTitle | String | Required, max 200 |
| source | String | e.g. Boss, 内推, 官网 |
| stage | ApplicationStage | Required |
| appliedAt | LocalDate | Required |
| nextFollowUpAt | LocalDate | Optional; drives overdue alerts |
| salaryRange | String | Optional |
| jdContent | TEXT | A4 placeholder — paste JD |
| companyNotes | TEXT | A4 placeholder — research notes |
| prepChecklist | TEXT | A3 placeholder — newline-separated items |
| prepDone | Boolean | A3 placeholder — simple done flag |
| createdAt | LocalDateTime | Auto |
| updatedAt | LocalDateTime | Auto |

### InterviewNote

| Field | Type | Notes |
|-------|------|-------|
| id | Long | PK |
| application | JobApplication | FK, required |
| interviewDate | LocalDate | Required |
| roundLabel | String | e.g. 一面, HR面 |
| questionsAsked | TEXT | A2 core |
| selfAssessment | TEXT | A2 core — what went well / poorly |
| improvements | TEXT | A2 core — action items |
| createdAt | LocalDateTime | Auto |

**Relationship:** JobApplication 1 — N InterviewNote (cascade delete notes when application deleted).

---

## 5. Pages & Routes

| Route | Method | Purpose |
|-------|--------|---------|
| `/` | GET | Dashboard |
| `/applications` | GET | List all; optional `?stage=` filter |
| `/applications/new` | GET/POST | Create application |
| `/applications/{id}` | GET | Detail: app info + notes list + A3/A4 fields |
| `/applications/{id}/edit` | GET/POST | Edit application |
| `/applications/{id}/stage` | POST | Quick stage update (form button) |
| `/applications/{id}/delete` | POST | Delete with confirm |
| `/applications/{id}/interviews/new` | GET/POST | Add interview note |
| `/interviews/{id}/edit` | GET/POST | Edit note |
| `/interviews/{id}/delete` | POST | Delete note |

### Dashboard Widgets

1. **Summary cards:** active applications (not REJECTED/WITHDRAWN), interviews this week, overdue follow-ups.
2. **Overdue list:** `nextFollowUpAt < today` and stage not terminal.
3. **Recent activity:** last 5 updated applications.

### UI Principles

- Single-column layout, mobile-friendly CSS (future App reuse patterns).
- Stage shown as color-coded badge.
- Detail page is the hub: pipeline info + debrief list + JD/notes/prep sections collapsed or below fold.

---

## 6. Business Rules

1. **Overdue follow-up:** `nextFollowUpAt` is before today AND stage ∉ {OFFER, REJECTED, WITHDRAWN}.
2. **Interview this week:** exists `InterviewNote.interviewDate` in current calendar week OR `nextFollowUpAt` in current week (configurable; MVP uses interview date only).
3. **Stage transitions:** unrestricted in MVP (user can jump any stage); no workflow engine.
4. **Validation:** company and position required; dates must not be null where marked required.
5. **Delete:** deleting application removes all interview notes (orphan prevention).

---

## 7. Error Handling

| Scenario | Behavior |
|----------|----------|
| Application not found | 404 custom Thymeleaf error page |
| Validation failure | Re-render form with field errors (BindingResult) |
| DB unavailable | Spring Boot default error page; log stack trace |
| Empty dashboard | Friendly empty state with CTA "Add first application" |

No global API — all HTML forms. CSRF enabled (Spring Security default when added later; MVP uses Spring Boot without Security, CSRF off for simplicity in dev-only local tool).

**MVP security note:** Local single-user tool, no auth. Document in README: do not expose to public network.

---

## 8. Testing (MVP)

| Test | Scope |
|------|-------|
| `JobApplicationServiceTest` | CRUD, stage filter, overdue query |
| `InterviewNoteServiceTest` | Create note linked to app, cascade delete |
| `DashboardServiceTest` | Counts and weekly interview aggregation |
| Optional `@WebMvcTest` | One smoke test for `GET /` returns 200 |

Target: 5–8 unit tests. No full E2E in MVP.

---

## 9. CC Week Assignment Alignment

### Repository Layout

```
offerflow/
├── README.md
├── JOURNAL.md
├── .claude/
│   ├── skills/
│   │   └── offerflow-coach/SKILL.md
│   └── hooks/
│       └── hooks.json
├── docs/superpowers/specs/2026-07-11-offerflow-design.md
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
└── src/...
```

### `.claude/skills/offerflow-coach`

Guides user to:

- Log a new application after each real submission.
- Write interview debrief within 24h of each interview.
- Export weekly summary bullets into JOURNAL.md.

### `sessionStart` Hook

Inject reminder: "Any applications or interviews to log today?"

### JOURNAL Strategy

- Days 1–2: building; document Superpowers friction and wins.
- Days 3–7: daily use with real job data; cognitive shift Day 1 vs Day 7.
- Include one "still broken" CC moment with screenshot.

### Git Strategy

Minimum 7 commits across 7 days (features, docs, skills, JOURNAL updates). Never single-commit submission.

---

## 10. Two-Day Build Schedule

### Day 1

1. Spring Boot scaffold (Web, Thymeleaf, JPA, H2, Validation).
2. Entities + repositories.
3. Application list, create, edit, detail pages.
4. Stage filter on list.
5. Commit: `day1: application crud and list views`.
6. Log real first application entry.

### Day 2

1. InterviewNote CRUD on application detail.
2. Dashboard with summary cards and overdue list.
3. Quick stage update buttons on detail page.
4. README + `.claude/` skill and hook.
5. Basic service tests.
6. Commit: `day2: dashboard, interview notes, claude config`.
7. Full manual test checklist pass.

### Days 3–7

- Use app daily; update JOURNAL; refine skill; record demo video; additional commits.

---

## 11. Manual Acceptance Checklist

- [ ] `./gradlew bootRun` starts without error.
- [ ] Create application with all required fields.
- [ ] Filter list by stage.
- [ ] Change stage from detail page.
- [ ] Add interview note; appears on detail page.
- [ ] Dashboard overdue count correct when follow-up date passed.
- [ ] Delete application removes linked notes.
- [ ] Restart app — H2 file data persists.
- [ ] README steps reproducible on fresh clone.

---

## 12. Rollback & Risk

| Risk | Mitigation |
|------|------------|
| H2 data loss | File mode path documented in README; optional export later |
| Scope creep into A3/A4 depth | Fields only; no checklist UI beyond textarea in MVP |
| 2-day slip | Cut MarkdownExport; keep dashboard + notes |
| CC JOURNAL empty | Mandate daily log in offerflow-coach skill |

---

## 13. Open Decisions (Resolved)

| Decision | Resolution |
|----------|------------|
| Product name | OfferFlow |
| MVP core | A1 pipeline + A2 debrief |
| A3/A4 in MVP | Text fields only |
| Frontend | Thymeleaf monolith |
| Build tool | Gradle (Groovy DSL) + Wrapper — not Maven |
| Auth | None in MVP |

No blocking unknowns remain. Ready for implementation plan.

---

## 14. Gradle Setup Notes

### Key Files

| File | Purpose |
|------|---------|
| `build.gradle` | Plugins, dependencies, Java toolchain |
| `settings.gradle` | Root project name `offerflow` |
| `gradlew` / `gradlew.bat` | Wrapper scripts (committed to repo) |
| `gradle/wrapper/` | Wrapper JAR + properties (committed) |

### Core Dependencies (via Spring Boot BOM)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.x'
    id 'io.spring.dependency-management' version '1.1.x'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'com.h2database:h2'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### Common Commands

| Task | Command |
|------|---------|
| Run app | `./gradlew bootRun` |
| Run tests | `./gradlew test` |
| Build JAR | `./gradlew build` |
| Clean | `./gradlew clean` |

Scaffold via [start.spring.io](https://start.spring.io) with **Gradle - Groovy** selected, or generate with Spring Boot CLI.
