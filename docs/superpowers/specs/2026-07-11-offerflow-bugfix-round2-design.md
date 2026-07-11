# OfferFlow Bugfix Round 2 Design Spec

**Date:** 2026-07-11  
**Status:** Draft — pending Ray review  
**Author:** Brainstorming session with Ray  
**Scope:** P0 + P1 (stage labels, detail page stability, web smoke tests, 500 error page)

## 1. Problem Summary

Manual testing found four broken flows after the MVP and four polish steps:

| ID | Symptom | User impact |
|----|---------|-------------|
| B1 | Dashboard “最近更新” company link → Whitelabel Error Page | Cannot open application detail from dashboard |
| B2 | Application list stage filter buttons show no text (except “全部”) | Stage filtering unusable / confusing |
| B3 | Application list “详情” button → Whitelabel Error Page | Cannot open application detail from list |
| B4 | New application form stage dropdown options are blank | User cannot see or pick stage labels |

Root-cause analysis:

- **B2, B4, and detail stage quick-action buttons** share one defect: Thymeleaf `${stageLabels[s]}` fails when `s` comes from `th:each` over `ApplicationStage.values()`, because `stageLabels` is a `Map<ApplicationStage, String>` and bracket lookup with the loop variable does not resolve. Entity property access `${stageLabels[app.stage]}` works, which is why table badges on dashboard/list may still show Chinese labels.
- **B1, B3** use correct URL syntax (`@{/applications/{id}(id=${app.id})}`). Whitelabel indicates an unhandled HTTP 500 during detail rendering, not a missing route. `GlobalExceptionHandler` only maps 404-style cases; other exceptions fall through to Spring Boot’s default error page.

## 2. Goal

Restore core navigation and form usability without expanding MVP scope.

### Success Criteria

- Stage filter buttons, form dropdown, and detail quick-stage buttons display Chinese labels for all seven stages.
- `GET /applications/{id}` returns HTTP 200 and renders `applications/detail` for existing records.
- Dashboard company links and list “详情” links open the detail page successfully.
- `SmokeWebTest` covers create → detail, list labels, and new-form options; `.\gradlew.bat test` passes.
- Unhandled server errors show a friendly `error/500.html` instead of a raw Whitelabel page.

### Non-Goals (this round)

- UX polish: clickable company names in list / overdue table
- `JOURNAL.md` Day 2–7 content
- Authentication, new features, or refactors unrelated to the bugs above

## 3. Chosen Approach

**方案 A (approved):** Replace fragile Map bracket access in templates with the existing static helper:

```html
th:text="${T(com.offerflow.web.StageLabels).label(s)}"
```

For entity-backed stage fields:

```html
th:text="${T(com.offerflow.web.StageLabels).label(app.stage)}"
```

### Why not alternatives

| Approach | Reason rejected |
|----------|-----------------|
| B: `List<StageOption>` in every controller | Larger diff; unnecessary for a template-expression bug |
| C: `Map<String, String>` keyed by `enum.name()` | Still depends on Thymeleaf key coercion; more moving parts |

`StageLabels.label()` already exists and is the single source of truth for Chinese labels.

## 4. Component Changes

### 4.1 Thymeleaf templates

| File | Change |
|------|--------|
| `applications/list.html` | Replace `${stageLabels[s]}` and `${stageLabels[app.stage]}` with `StageLabels.label(...)` |
| `applications/form.html` | Replace `${stageLabels[s]}` in `<option>` |
| `applications/detail.html` | Replace `${stageLabels[application.stage]}` and `${stageLabels[s]}` in stage buttons |
| `dashboard.html` | Replace `${stageLabels[app.stage]}` in badges |

Keep `stageLabels` model attribute in controllers for now (no breaking change if any template still references it); optional cleanup in a later pass.

Stage filter highlight expression `${selectedStage == s}` is unchanged; fixing label rendering should make the correct button readable. If highlight still fails after fix, switch comparison to `${selectedStage != null and selectedStage.name() == s.name()}` in the same edit.

### 4.2 Error handling

| File | Change |
|------|--------|
| `error/500.html` | New page matching `error/404.html` layout and nav fragment |
| `GlobalExceptionHandler.java` | Add handlers for `LazyInitializationException` and a generic `Exception` fallback returning `error/500` with HTTP 500 |

Log the exception message (and stack trace at WARN) in the generic handler for local debugging.

### 4.3 Tests

| File | Change |
|------|--------|
| `SmokeWebTest.java` | New `@SpringBootTest` + `MockMvc` tests |

Test cases:

1. `listPageShowsStageFilterLabels` — `GET /applications` contains “已投递” and “简历筛选”
2. `newFormShowsStageOptions` — `GET /applications/new` contains “已投递” and “技术面试”
3. `detailPageRendersAfterCreate` — `POST /applications` then `GET` redirect URL returns 200 and view `applications/detail`

Existing `GlobalExceptionHandlerWebTest` remains unchanged.

### 4.4 Service layer (verify only)

No code change expected. Confirm `JobApplicationService.requireApplication()` continues to use `findByIdWithNotes` JOIN FETCH so `application.interviewNotes.isEmpty()` in the detail template does not trigger lazy-load errors.

## 5. Data Flow (unchanged)

```
GET /applications/{id}
  → ApplicationController.detail()
    → JobApplicationService.requireApplication(id)
      → JobApplicationRepository.findByIdWithNotes(id)
  ← applications/detail.html
```

Fix ensures the template renders without expression failures or unhandled lazy-init errors.

## 6. Error Handling Matrix (after fix)

| Condition | HTTP | View |
|-----------|------|------|
| Invalid ID format | 404 | `error/404` |
| Missing application / interview note | 404 | `error/404` |
| LazyInitializationException | 500 | `error/500` |
| Any other unhandled exception | 500 | `error/500` |

## 7. Verification Plan

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cd C:\Users\Ray\offerflow
.\gradlew.bat test
.\gradlew.bat bootRun
```

Manual checks:

1. `/applications` — seven labeled filter buttons + “全部”
2. `/applications/new` — dropdown shows all stage labels
3. Create a record → dashboard company link and list “详情” both open detail
4. Detail page — seven labeled quick-stage buttons

## 8. Implementation Notes

- One conventional commit for this round, e.g. `fix(web): resolve stage labels and detail page errors`
- Do not amend prior commits
- Push to `origin/master` after local verification (optional, user-driven)

## 9. Risks

| Risk | Mitigation |
|------|------------|
| Detail Whitelabel caused by something other than stage labels / lazy load | SmokeWebTest + 500 handler; inspect server log if test passes but manual test fails |
| Long `T(...)` expressions in templates | Acceptable for minimal fix; can extract Thymeleaf utility later |
