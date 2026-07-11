# OfferFlow Phase 5 Design Spec — Polish + Expansion Packs

**Date:** 2026-07-12  
**Status:** Approved (2026-07-12)  
**Author:** Brainstorming session with Ray  
**Scope:** A1 polish (dashboard interviews list, application search, Markdown export) + A4/C expansion (3 company seeds, 3 interview templates, selectors, company name hint)

## 1. Problem & Goal

### Problem

After Phase 2–4 (company dossier + seed import + search) and Phase 3 (interview prep/debrief templates), OfferFlow is feature-complete for a Java-backend job hunt demo — but daily-use friction remains:

| Gap | Symptom |
|-----|---------|
| A1 polish | Dashboard shows interview *count* only; cannot click into this week's interviews |
| A1 polish | Application list has stage filter but no text search when volume grows |
| Backup | No export; data trapped in local H2 |
| A4/C expansion | Only one company seed pack and one interview template (Java backend) |
| Data quality | Manual company name on application form may duplicate existing dossier |

### Goal

**Phase 5** delivers:

1. **B — Polish:** week interview list, application search, per-application Markdown export
2. **C — Expansion:** two new company seed packs + two new interview template packs + dropdown selectors
3. **C — Hint:** non-blocking flash when manual company name matches existing dossier

### Success Criteria

- Dashboard lists this week's interviews with links to application detail
- `GET /applications?q=美团` filters by company name or position title; combinable with `?stage=`
- `GET /applications/{id}/export` downloads a `.md` file with application + notes content
- `finance-tech` and `foreign-tech` seeds import idempotently via selector on `/companies`
- `frontend-react` and `go-backend` templates fill prep / prefill debrief via selector on application detail
- Manual company name matching existing dossier shows informational flash; save still succeeds
- `.\gradlew.bat test` passes including new service and web tests

### Non-Goals (Phase 5)

- User authentication / multi-tenant
- Bulk export (all applications)
- Template or seed CRUD UI
- Auto-linking manual company name to dossier without user selecting dropdown
- Email/calendar reminders
- Scraping or AI-generated content

## 2. Chosen Approach

**方案 A (approved):** Extend existing `CompanySeedService` and `InterviewTemplateService` registries (`Map<String, String>` pack id → classpath JSON). Replace single-button UI with `<select>` + one action button on list/detail pages.

### Rejected alternatives

| Approach | Reason rejected |
|----------|-----------------|
| B: One button per pack | 7+ buttons clutter company list and application detail |
| C: Dedicated `/resources` page | Extra navigation step; worse Demo flow |

## 3. B — Polish Features

### B1 Dashboard — This Week's Interviews List

**DTO:** `InterviewWeekItem` record

| Field | Type | Notes |
|-------|------|-------|
| `noteId` | Long | Interview note id |
| `applicationId` | Long | Parent application |
| `interviewDate` | LocalDate | |
| `roundLabel` | String | nullable |
| `companyName` | String | from application |
| `positionTitle` | String | from application |

**DashboardView** adds: `List<InterviewWeekItem> interviewsThisWeek`

**Service:** `DashboardService.build(today)` — query `InterviewNoteRepository.findByInterviewDateBetween(weekStart, weekEnd)`, map to items sorted by `interviewDate` asc then company name.

**UI:** `dashboard.html` — card「本周面试」with table (date, company, position, round, 查看 link). Hidden when list empty.

### B2 Application Search

**Endpoint:** `GET /applications?q=...&stage=...` (both optional)

**Repository method:**

```
findByCompanyNameContainingIgnoreCaseOrPositionTitleContainingIgnoreCaseOrderByUpdatedAtDesc(String q)
```

When `stage` also present, filter in service: load by stage then filter by q, OR add combined repository query — implementation choice; behavior must match: case-insensitive substring on `companyName` OR `positionTitle`.

**UI:** Search form above stage filters; preserve `q` in stage links; empty state when no matches.

### B3 Markdown Export

**Endpoint:** `GET /applications/{id}/export`

**Response:**
- `Content-Type: text/markdown; charset=UTF-8`
- `Content-Disposition: attachment; filename="offerflow-{sanitizedCompany}-{sanitizedPosition}.md"`

**Service:** `MarkdownExportService.export(JobApplication app)` returns String:

```markdown
# {company} — {position}

- 阶段：{stage}
- 投递日：{appliedAt}
- 下次跟进：{nextFollowUpAt}

## 公司档案
(若关联：官网/招聘页/内推摘要)

## JD
...

## 准备清单
...

## 面试复盘
### {date} · {round}
**问题：** ...
```

**UI:** Button on `applications/detail.html` —「导出 Markdown」

**Filename sanitization:** Replace non-alphanumeric (keep CJK) with `-`, max 50 chars per segment.

## 4. C — Expansion Packs

### 4.1 Company Seeds

| Pack ID | Resource file | Count | Industry constant |
|---------|---------------|-------|-------------------|
| `java-backend-internet` | `seeds/java-backend-internet.json` | 18 | 互联网 |
| `finance-tech` | `seeds/finance-tech.json` | ~10 | 金融科技 |
| `foreign-tech` | `seeds/foreign-tech.json` | ~10 | 外企科技 |

**finance-tech candidates (10):** 蚂蚁集团, 微众银行, 京东科技, 平安科技, 东方财富, 同花顺, 恒生电子, 富途控股, 雪球, 陆金所

**foreign-tech candidates (10):** Microsoft, Google, Amazon, Apple, Meta, NVIDIA, SAP, Intel, Shopee, LinkedIn

URLs: implementation verifies or omits `careersUrl` if unverified — same rule as Phase 4.

**CompanySeedService changes:**
- Extend `SEED_RESOURCES` map with `finance-tech`, `foreign-tech`
- Constants: `FINANCE_TECH = "finance-tech"`, `FOREIGN_TECH = "foreign-tech"`
- `listAvailableSeeds()` → `List<SeedPackInfo>` (id, title, count) for UI dropdown

**Import:** Existing `POST /companies/import-seed?seed={id}` — no route change.

### 4.2 Interview Templates

| Pack ID | Resource file | Title |
|---------|---------------|-------|
| `java-backend` | `seeds/java-backend-interview.json` | Java 后端 |
| `frontend-react` | `seeds/frontend-react-interview.json` | 前端 React |
| `go-backend` | `seeds/go-backend-interview.json` | Go 后端 |

**frontend-react prep topics:** React 生命周期/Hooks, 虚拟 DOM, 状态管理, 浏览器渲染/性能, CSS 布局, TypeScript 基础, 工程化/Webpack/Vite, 手写题, 项目亮点

**go-backend prep topics:** goroutine/channel, GC, interface, 并发安全, gRPC/微服务, MySQL/Redis, 项目亮点

Debrief structure: same 4-field schema as Phase 3 (`roundLabel`, `questionsAsked`, `selfAssessment`, `improvements`).

**InterviewTemplateService changes:**
- Extend `TEMPLATE_RESOURCES` map
- `listAvailableTemplates()` → `List<TemplatePackInfo>` for UI

**Apply prep:** `POST /applications/{id}/apply-template?template={id}` — already exists; extend to accept new ids.

**Apply debrief:** `GET .../interviews/new?template={id}` — already exists.

### 4.3 Selectors UI

**`companies/list.html`:**
- Replace single import card with:
  - `<select name="seed">` options from `seedPacks` model attribute
  - Button「导入 seed」→ POST `import-seed?seed={selected}`
- Subtitle: 「已存在的公司将跳过」

**`applications/detail.html`:**
- Prep card: `<select>` for template id + POST apply-template
- Interview header: keep「+ 新增复盘」; add compact links or small select for「带模板」variants (3 template ids)

### 4.4 Manual Company Name Hint (C6)

**On POST create/update** when `companyId == null` and `companyName` trimmed non-empty:

```
if companyRepository.findByNameIgnoreCase(trimmedName).isPresent():
  redirectAttributes.addFlashAttribute(INFO or SUCCESS variant,
    "已存在公司档案「{name}」，下次可从下拉选择关联。")
```

Use `FlashMessages.SUCCESS` or add `FlashMessages.INFO` if needed — v1 may reuse SUCCESS with neutral wording.

**Non-blocking:** Application saves normally; no auto-set `companyId`.

**Optional v1:** Yellow hint on form when exact match detected server-side on validation error re-render — skip if scope tight; flash on save is required.

## 5. Architecture

### New / modified components

| Unit | Responsibility |
|------|----------------|
| `dto/InterviewWeekItem.java` | Dashboard row |
| `dto/SeedPackInfo.java` | id, title, entryCount for dropdown |
| `dto/TemplatePackInfo.java` | id, title for dropdown |
| `service/MarkdownExportService.java` | Build markdown string |
| `service/DashboardService.java` | Add week interviews list |
| `service/JobApplicationService.java` | `search(q, stage)` |
| `service/CompanySeedService.java` | +2 seeds, `listAvailableSeeds()` |
| `service/InterviewTemplateService.java` | +2 templates, `listAvailableTemplates()` |
| `repository/JobApplicationRepository.java` | Search query |
| `controller/ApplicationController.java` | search param, export endpoint, template list model |
| `controller/CompanyController.java` | pass seedPacks to list |
| `controller/DashboardController.java` | unchanged signature |
| `web/FlashMessages.java` | optional `INFO` constant |

### Request flows

```
GET /dashboard
  → DashboardService.build()
  ← dashboard.html (+ interviewsThisWeek table)

GET /applications?q=字节&stage=INTERVIEW
  → JobApplicationService.search()
  ← applications/list.html

GET /applications/42/export
  → MarkdownExportService.export(requireApplication(42))
  ← text/markdown attachment

POST /companies/import-seed?seed=finance-tech
  → CompanySeedService.importSeed()
  ← redirect + flash (unchanged algorithm)

POST /applications/42/apply-template?template=frontend-react
  → InterviewTemplateService.applyPrepChecklist()
  ← redirect (unchanged fill rules)
```

## 6. Error Handling

| Condition | Behavior |
|-----------|----------|
| Unknown seed id on import | Flash error「seed 包不存在」 |
| Unknown template id | Flash error「模板不存在」 |
| Export application not found | 404 |
| Empty search results | Empty state message on list |
| No interviews this week | Hide week interviews card |

## 7. Testing

| Test class | Cases |
|------------|-------|
| `JobApplicationServiceTest` | search by company partial, by position, no match |
| `DashboardServiceTest` | interviewsThisWeek populated for note in range |
| `MarkdownExportServiceTest` | output contains company, position, note section |
| `CompanySeedServiceTest` | finance-tech count, foreign-tech import idempotent |
| `InterviewTemplateServiceTest` | load frontend-react, go-backend; apply prep |
| `ApplicationWebTest` | search q param; export 200 + content-disposition; company hint flash |
| `CompanyWebTest` | import finance-tech via seed param; list shows selector |

## 8. Implementation Tasks

| Task | Commit message |
|------|----------------|
| **17** | `feat(application): add application search on list page` |
| **18** | `feat(dashboard): add this week interviews list` |
| **19** | `feat(application): add markdown export for application detail` |
| **20** | `feat(company): add finance and foreign tech seed packs` |
| **21** | `feat(company): add seed pack selector on company list` |
| **22** | `feat(interview): add frontend and go interview template packs` |
| **23** | `feat(application): add interview template selector on detail page` |
| **24** | `feat(application): suggest linking manual company name to dossier` |
| **25** | `docs: document Phase 5 polish and expansion packs` |

Tasks 17–19 (polish) and 20–24 (expansion) are independently mergeable groups. Task 21 depends on 20; Task 23 depends on 22.

## 9. Documentation (Task 25)

- `README.md`: Phase 5 section, updated page table, seed/template pack table
- `JOURNAL.md`: Phase 5 completion note; update assignment checklist if applicable

## 10. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Large spec (9 tasks) | Clear task ordering; polish first for Demo value |
| Unverified foreign company URLs | Omit or placeholder; README notes user should verify |
| Export filename encoding on Windows | ASCII-safe fallback filename if sanitization yields empty |
| Selector UX regression | Keep default selection on most-used pack (`java-backend-internet` / `java-backend`) |

## 11. Future (out of scope)

- Bulk Markdown export (zip)
- Seed/template preview modal before import
- Fuzzy company match picker (「是否关联美团？」confirm dialog)
- Full-text search across interview note bodies
- Phase 6: auth + cloud sync
