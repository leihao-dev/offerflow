# OfferFlow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local Spring Boot + Thymeleaf job-application tracker (pipeline + interview debrief) runnable via Gradle Wrapper.

**Architecture:** Single Gradle monolith. MVC controllers render Thymeleaf templates. JPA entities persist to H2 file database. Services encapsulate business rules (overdue, dashboard counts).

**Tech Stack:** Java 17+, Spring Boot 3.3.x, Thymeleaf, Spring Data JPA, H2, Gradle Groovy DSL, JUnit 5

**Spec:** `docs/superpowers/specs/2026-07-11-offerflow-design.md`

**Prerequisite:** JDK 17+ on PATH. Verify: `java -version`. Gradle Wrapper handles build tool (`gradlew.bat bootRun` on Windows).

---

## File Map

| File | Responsibility |
|------|----------------|
| `build.gradle` | Dependencies, Java toolchain, Boot plugin |
| `settings.gradle` | Root project name |
| `src/main/java/com/offerflow/OfferFlowApplication.java` | Entry point |
| `model/ApplicationStage.java` | Stage enum |
| `model/JobApplication.java` | Application entity |
| `model/InterviewNote.java` | Interview debrief entity |
| `repository/*Repository.java` | JPA repositories |
| `service/JobApplicationService.java` | Application CRUD + queries |
| `service/InterviewNoteService.java` | Note CRUD |
| `service/DashboardService.java` | Dashboard aggregates |
| `controller/DashboardController.java` | GET `/` |
| `controller/ApplicationController.java` | Application routes |
| `controller/InterviewController.java` | Interview note routes |
| `resources/application.yml` | H2 file datasource |
| `resources/templates/*.html` | Thymeleaf views |
| `resources/static/css/app.css` | Minimal styling |
| `test/.../JobApplicationServiceTest.java` | Service unit tests |
| `README.md` | Setup and run instructions |
| `JOURNAL.md` | CC assignment journal |
| `.claude/skills/offerflow-coach/SKILL.md` | CC skill |

---

## Task 1: Project Scaffold + Git Init

**Files:**
- Create: `build.gradle`, `settings.gradle`, Gradle Wrapper files
- Create: `src/main/java/com/offerflow/OfferFlowApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `.gitignore`

- [ ] **Step 1: Initialize git repo**

Run from `C:\Users\Ray\offerflow`:
```powershell
git init
```

- [ ] **Step 2: Create `settings.gradle`**

```groovy
rootProject.name = 'offerflow'
```

- [ ] **Step 3: Create `build.gradle`**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.offerflow'
version = '0.1.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'com.h2database:h2'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Generate Gradle Wrapper**

If Gradle installed globally:
```powershell
gradle wrapper --gradle-version 8.10.2
```
Otherwise download wrapper from Spring Initializr zip or copy from another project. Must have `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 5: Create `OfferFlowApplication.java`**

```java
package com.offerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OfferFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(OfferFlowApplication.class, args);
    }
}
```

- [ ] **Step 6: Create `application.yml`**

```yaml
spring:
  application:
    name: offerflow
  datasource:
    url: jdbc:h2:file:./data/offerflow;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  thymeleaf:
    cache: false

server:
  port: 8080
```

- [ ] **Step 7: Create `.gitignore`**

```
.gradle/
build/
data/
*.iml
.idea/
out/
.DS_Store
```

- [ ] **Step 8: Verify boot**

Run: `gradlew.bat bootRun`
Expected: App starts on port 8080 (Whitelabel 404 is OK)

- [ ] **Step 9: Commit**

```powershell
git add .
git commit -m "day1: gradle spring boot scaffold"
```

---

## Task 2: Domain Model + Repositories

**Files:**
- Create: `model/ApplicationStage.java`, `model/JobApplication.java`, `model/InterviewNote.java`
- Create: `repository/JobApplicationRepository.java`, `repository/InterviewNoteRepository.java`

- [ ] **Step 1: Create `ApplicationStage.java`**

```java
package com.offerflow.model;

public enum ApplicationStage {
    APPLIED,
    SCREENING,
    TECH_INTERVIEW,
    FINAL_INTERVIEW,
    OFFER,
    REJECTED,
    WITHDRAWN
}
```

- [ ] **Step 2: Create `JobApplication.java`**

```java
package com.offerflow.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 200)
    private String positionTitle;

    @Column(length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStage stage = ApplicationStage.APPLIED;

    @Column(nullable = false)
    private LocalDate appliedAt;

    private LocalDate nextFollowUpAt;

    @Column(length = 100)
    private String salaryRange;

    @Lob
    private String jdContent;

    @Lob
    private String companyNotes;

    @Lob
    private String prepChecklist;

    private Boolean prepDone = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("interviewDate DESC")
    private List<InterviewNote> interviewNotes = new ArrayList<>();

    // getters/setters omitted in plan — generate or write all
}
```

Implement all getters/setters. Add `@PreUpdate void touch() { updatedAt = LocalDateTime.now(); }`.

- [ ] **Step 3: Create `InterviewNote.java`**

```java
package com.offerflow.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_notes")
public class InterviewNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Column(nullable = false)
    private LocalDate interviewDate;

    @Column(length = 100)
    private String roundLabel;

    @Lob
    private String questionsAsked;

    @Lob
    private String selfAssessment;

    @Lob
    private String improvements;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters/setters
}
```

- [ ] **Step 4: Create repositories**

```java
package com.offerflow.repository;

import com.offerflow.model.ApplicationStage;
import com.offerflow.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByStageOrderByUpdatedAtDesc(ApplicationStage stage);
    List<JobApplication> findAllByOrderByUpdatedAtDesc();
    long countByStageNotIn(List<ApplicationStage> terminalStages);
    List<JobApplication> findByNextFollowUpAtBeforeAndStageNotIn(
        LocalDate date, List<ApplicationStage> terminalStages);
    List<JobApplication> findTop5ByOrderByUpdatedAtDesc();
}
```

```java
package com.offerflow.repository;

import com.offerflow.model.InterviewNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface InterviewNoteRepository extends JpaRepository<InterviewNote, Long> {
    List<InterviewNote> findByInterviewDateBetween(LocalDate start, LocalDate end);
}
```

- [ ] **Step 5: Verify compile**

Run: `gradlew.bat compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```powershell
git commit -am "day1: domain model and repositories"
```

---

## Task 3: JobApplicationService + Tests (TDD)

**Files:**
- Create: `service/JobApplicationService.java`
- Create: `test/.../JobApplicationServiceTest.java`

- [ ] **Step 1: Write failing test `createsApplication`**

Use `@DataJpaTest` or `@SpringBootTest` with `@Transactional`.

```java
@SpringBootTest
@Transactional
class JobApplicationServiceTest {

    @Autowired JobApplicationService service;

    @Test
    void createsApplication() {
        JobApplication app = service.create(new ApplicationForm(
            "Acme", "Backend Engineer", "Boss",
            ApplicationStage.APPLIED, LocalDate.now(), null, null,
            null, null, null, false));
        assertNotNull(app.getId());
        assertEquals("Acme", app.getCompanyName());
    }
}
```

- [ ] **Step 2: Run test — expect FAIL** (`gradlew.bat test`)

- [ ] **Step 3: Implement `ApplicationForm` record + `JobApplicationService`**

`ApplicationForm` as Java record mirroring entity fields.

Service methods: `create`, `update`, `findById`, `findAll(Optional<ApplicationStage>)`, `delete`, `updateStage`, `findOverdue`.

Terminal stages: `OFFER`, `REJECTED`, `WITHDRAWN`.

- [ ] **Step 4: Run tests — expect PASS**

- [ ] **Step 5: Commit**

```powershell
git commit -am "day1: JobApplicationService with tests"
```

---

## Task 4: Application UI (List, Create, Edit, Detail)

**Files:**
- Create: `controller/ApplicationController.java`
- Create: `templates/layout.html`, `applications/list.html`, `form.html`, `detail.html`
- Create: `static/css/app.css`

- [ ] **Step 1: Create Thymeleaf layout with nav** (Dashboard | Applications | New)

- [ ] **Step 2: Implement `ApplicationController`**

Routes per spec §5: list, new GET/POST, detail, edit GET/POST, stage POST, delete POST.

Use `@Valid ApplicationForm` + `BindingResult` for validation errors.

- [ ] **Step 3: Stage filter** — `GET /applications?stage=TECH_INTERVIEW`

- [ ] **Step 4: Detail page** — show all fields, quick stage buttons, link to add interview

- [ ] **Step 5: Manual test** — create one real application entry

- [ ] **Step 6: Commit**

```powershell
git commit -am "day1: application crud and list views"
```

---

## Task 5: Interview Notes

**Files:**
- Create: `service/InterviewNoteService.java`
- Create: `controller/InterviewController.java`
- Create: `templates/interviews/form.html`
- Modify: `templates/applications/detail.html`

- [ ] **Step 1: `InterviewNoteService`** — create, update, delete, listByApplication

- [ ] **Step 2: Routes** — `/applications/{id}/interviews/new`, `/interviews/{id}/edit`, delete POST

- [ ] **Step 3: Show notes on detail page**

- [ ] **Step 4: Test + commit**

```powershell
git commit -am "day2: interview note crud"
```

---

## Task 6: Dashboard

**Files:**
- Create: `service/DashboardService.java`, `controller/DashboardController.java`
- Create: `templates/dashboard.html`
- Create: `test/.../DashboardServiceTest.java`

- [ ] **Step 1: DashboardService**

Returns DTO with: activeCount, interviewsThisWeek, overdueCount, overdueList, recentApplications.

`interviewsThisWeek`: notes where `interviewDate` between Monday and Sunday of current week.

- [ ] **Step 2: Dashboard template** — summary cards + overdue table + recent list

- [ ] **Step 3: Set `/` to dashboard**

- [ ] **Step 4: Tests for counts**

- [ ] **Step 5: Commit**

```powershell
git commit -am "day2: dashboard with overdue and weekly stats"
```

---

## Task 7: CC Assignment Artifacts

**Files:**
- Create: `README.md`, `JOURNAL.md`
- Create: `.claude/skills/offerflow-coach/SKILL.md`
- Create: `.claude/hooks/hooks.json` (optional sessionStart reminder)

- [ ] **Step 1: README** — what it is, `./gradlew bootRun`, JDK 17 prerequisite, H2 data path

- [ ] **Step 2: JOURNAL.md** — template with Day 1 entry (Superpowers install, brainstorming, scaffold)

- [ ] **Step 3: offerflow-coach skill** — remind log application after submit, debrief within 24h

- [ ] **Step 4: Final manual checklist** (spec §11)

- [ ] **Step 5: Commit**

```powershell
git commit -am "day2: readme journal and claude config"
```

---

## Verification Commands

```powershell
cd C:\Users\Ray\offerflow
gradlew.bat test
gradlew.bat bootRun
# Browser: http://localhost:8080
```

## Rollback

Each task is one commit. `git revert` or `git reset` per task if needed. H2 data in `./data/` — delete folder to reset DB.

---

## Spec Coverage Check

| Spec Requirement | Task |
|------------------|------|
| Application CRUD + stages | Task 4 |
| Interview debrief | Task 5 |
| Dashboard overdue/weekly | Task 6 |
| A3/A4 text fields | Task 2 (entity fields), Task 4 (form) |
| Gradle bootRun | Task 1 |
| README/JOURNAL/.claude | Task 7 |
| Unit tests | Task 3, 6 |

All spec MVP requirements covered.
