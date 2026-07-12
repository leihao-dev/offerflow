# OfferFlow Phase 6 Design Spec — Knowledge Base + Portability

**Date:** 2026-07-12  
**Status:** Approved (2026-07-12)  
**Author:** Brainstorming session with Ray  
**Scope:** Phase 6 product evolution — debrief full-text search (独立页), bulk Markdown zip export, seed/template preview

## 1. Problem & Goal

### Problem

Phase 0–5 delivered a complete local job-search CRM (A1–A4), but three portfolio-visible gaps remain for daily deepening use:

| Gap | Symptom |
|-----|---------|
| A2 depth | Past interview questions live inside individual application detail pages; no way to search「美团三面问过 JVM 吗？」 |
| Data portability | Single-application Markdown export exists; no one-click backup of entire job hunt |
| Content discovery | Seed packs and interview templates are chosen from dropdown IDs without seeing contents first |

### Goal

**Phase 6** delivers **方案 A —「可带走的知识库」**:

1. **Debrief search** — independent page `/interviews/search?q=` across all interview notes
2. **Bulk export** — `GET /applications/export-all` downloads a zip of per-application Markdown files
3. **Preview** — read-only preview of company seed packs and interview templates before import/fill

### Success Criteria

- `GET /interviews/search?q=JVM` returns matching notes with company, position, date, snippet, link to application detail
- Empty `q` shows helpful empty state (not all notes — avoid noise)
- `GET /applications/export-all` downloads `offerflow-export-YYYYMMDD.zip` with one `.md` per application using existing `MarkdownExportService`
- Zip handles 0 applications gracefully (empty zip or 404 with message — see §3)
- Company list: preview button/modal shows first N company names + industries for selected seed pack
- Application detail: preview link per template shows prep checklist excerpt + debrief section headings
- Preview endpoints are read-only; no DB writes
- Nav includes link to interview search
- `.\gradlew.bat test` passes including new service and web tests
- README Phase 6 section updated

### Non-Goals (Phase 6)

- Spaced repetition / SRS scheduling (Phase 7 candidate)
- JD parsing or skill-gap analysis
- Question extraction / aggregation dashboard (方案 B deferred)
- User authentication, MySQL, Docker deploy
- New seed or template JSON packs (content expansion only via existing registry)
- Full-text search on JD or prep checklist (debrief fields only in v1)
- JSON import / restore

### Context constraints (from brainstorming)

- **Audience:** solo localhost user; portfolio / product evolution narrative
- **Timeline:** 3–5 days, incremental Task commits
- **Search UX:** independent page (not extending `/applications?q=`)

---

## 2. Chosen Approach

**方案 A (approved):** Search + bulk zip + preview as three loosely coupled features sharing existing services.

### Rejected alternatives

| Approach | Reason rejected |
|----------|-----------------|
| B: Interview intelligence (search + question bank + today follow-up) | Question aggregation scope creep; user chose A |
| C: Polish bundle (preview + export + dossier link + new seed) | Weaker portfolio depth story |
| Extend `/applications?q=` to search debriefs | User prefers independent `/interviews/search` page |

---

## 3. Feature A — Debrief Full-Text Search

### User story

After several interviews, user searches「线程池」to find which companies asked related questions and jumps back to the full debrief.

### Route

| Method | Path | Description |
|--------|------|-------------|
| GET | `/interviews/search` | Search form + results (`?q=` required for results) |

Nav label: **复盘搜索** (add to `fragments/nav.html`, `activePage = 'interviews-search'`).

### Search scope

Match case-insensitive substring (`LIKE %q%`) against:

- `InterviewNote.questionsAsked`
- `InterviewNote.selfAssessment`
- `InterviewNote.improvements`
- `InterviewNote.roundLabel`
- Related `JobApplication.companyName`
- Related `JobApplication.positionTitle`

Order: `interviewDate DESC`, then `createdAt DESC`.

### Result row (DTO)

`InterviewSearchHit` record:

| Field | Source |
|-------|--------|
| `noteId` | `InterviewNote.id` |
| `applicationId` | `JobApplication.id` |
| `companyName` | application |
| `positionTitle` | application |
| `interviewDate` | note |
| `roundLabel` | note |
| `snippet` | first ~120 chars around match from best-matching field |

Snippet: server-side truncate; highlight optional (plain text v1 — no HTML injection).

### Empty states

| Condition | UI |
|-----------|-----|
| No `q` | Search box + hint「输入关键词搜索全部复盘内容」 |
| `q` present, 0 hits |「没有匹配「{q}」的复盘」 |
| `q` blank after trim | treat as no query |

### Architecture

```
InterviewSearchController
  → InterviewSearchService.search(q)
    → InterviewNoteRepository.searchByQuery(q)
      → JOIN FETCH application
```

New files:

- `dto/InterviewSearchHit.java`
- `service/InterviewSearchService.java`
- `controller/InterviewSearchController.java` (`@RequestMapping("/interviews")`)
- `templates/interviews/search.html`
- Repository `@Query` on `InterviewNoteRepository`

### Repository query (sketch)

```java
@Query("""
    SELECT n FROM InterviewNote n JOIN FETCH n.application a
    WHERE LOWER(COALESCE(n.questionsAsked, '')) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(COALESCE(n.selfAssessment, '')) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(COALESCE(n.improvements, '')) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(COALESCE(n.roundLabel, '')) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(a.companyName) LIKE LOWER(CONCAT('%', :q, '%'))
       OR LOWER(a.positionTitle) LIKE LOWER(CONCAT('%', :q, '%'))
    ORDER BY n.interviewDate DESC, n.createdAt DESC""")
List<InterviewNote> searchByQuery(@Param("q") String q);
```

H2 file mode handles moderate data volumes; no Lucene in Phase 6.

### Error handling

- `q` longer than 200 chars → truncate or 400 flash; v1: truncate silently to 200
- SQL wildcard chars in `q` — `%` and `_` escaped if needed (v1: accept literal LIKE behavior)

### Tests

- `InterviewSearchServiceTest`: create note with「线程池」, search「线程」returns 1 hit
- `InterviewSearchWebTest`: GET `/interviews/search?q=线程池` → 200, contains company name

---

## 4. Feature B — Bulk Markdown Zip Export

### User story

User wants to archive entire job hunt before reinstalling OS or sharing portfolio samples.

### Route

| Method | Path | Description |
|--------|------|-------------|
| GET | `/applications/export-all` | Download zip |

UI: button on `/applications` list page header — **导出全部 Markdown**， beside existing per-row export on detail only.

### Behavior

1. Load all applications ordered by `updatedAt DESC` (or `companyName ASC` — pick `updatedAt DESC` for recency)
2. For each application, `markdownExportService.export(app)` + `buildFilename(app)`
3. Write entries into zip in memory (`ByteArrayOutputStream` + `ZipOutputStream`)
4. Filename: `offerflow-export-YYYYMMDD.zip`
5. **Duplicate filenames:** append `-{id}` before `.md` if collision

### Limits

- **Max applications:** 500 (config constant `ExportLimits.MAX_BULK_EXPORT`). If exceeded, export first 500 + log warning header comment file `README.txt` inside zip — v1 simpler: return 413 or flash「超过 500 条，请分批导出」on list page if count > 500 before download
- **0 applications:** return HTTP 404 with plain text body or redirect to list with flash「暂无投递可导出」— prefer **redirect + flash** for Thymeleaf consistency

### Architecture

Extend `MarkdownExportService` or add `BulkExportService`:

```java
byte[] exportAllAsZip(List<JobApplication> apps);
```

Controller method on `ApplicationController` (same as single export).

### Tests

- `BulkExportServiceTest` or `MarkdownExportServiceTest`: 2 apps → zip contains 2 entries, first entry readable UTF-8 Markdown
- `ApplicationWebTest`: GET export-all with seeded data → 200, `Content-Type: application/zip`

---

## 5. Feature C — Seed & Template Preview

### User story

Before importing `finance-tech` or filling `go-backend` template, user sees what will be loaded.

### C1 — Company seed preview

**UI:** On `/companies` seed import form, add **预览** button next to **导入 seed**.

**Interaction (v1 — no JS framework):**

- `GET /companies?previewSeed=finance-tech` (same list page, query param)
- Controller loads pack via `CompanySeedService` — new method `previewSeed(String seedId)` returning `SeedPreview` (title, entryCount, List of first 5 `{name, industry}`)
- Template section below dropdown shows preview panel when `previewSeed` set
- Link「关闭预览」clears param

Alternative rejected: separate modal API — unnecessary for server-rendered app.

**New DTO:** `SeedPreviewView` (title, packId, totalCount, sampleEntries)

**New service method:** read JSON entries, return first 5 names (no DB write).

### C2 — Interview template preview

**UI:** On application detail, beside template `<select>` for prep fill, add **预览** per pack or single preview for selected option.

**Simplest v1:** Link per template:

```
GET /applications/{id}/preview-template?template=java-backend
```

Returns same detail page with `templatePreview` model attribute **or** dedicated minimal template `applications/template-preview.html` (fragment).

**Preview content:**

| Field | Display |
|-------|---------|
| Title | pack title |
| Prep | first 8 lines of `prepChecklist` (truncate with `…`) |
| Debrief | `roundLabel` + first line of each debrief section |

Read-only; `InterviewTemplateService.requirePack(templateId)`.

Invalid template id → 404 or flash on redirect.

### Tests

- `CompanySeedServiceTest`: preview returns 5 samples from finance-tech
- `InterviewTemplateServiceTest`: preview content non-empty for java-backend
- Web smoke: companies list with `previewSeed` shows 蚂蚁集团; detail preview shows checklist text

---

## 6. UI & Navigation

### Nav update

Add between「投递列表」and「目标公司」:

```html
<a th:href="@{/interviews/search}" ...>复盘搜索</a>
```

### Applications list

Add bulk export button (top actions row).

### CSS

Reuse existing `.card`, `.muted`, `.actions` — no new CSS file unless preview panel needs `.preview-panel` border block.

---

## 7. Testing Strategy

| Area | Tests |
|------|-------|
| Search service | 2–3 unit tests (match, no match, company name match) |
| Search web | 1 MockMvc |
| Bulk zip | 1 service + 1 web |
| Seed preview | 1 service |
| Template preview | 1 service + optional web |
| Regression | full `.\gradlew.bat test` |

Manual acceptance:

- [ ] Search「算法」finds note across different applications
- [ ] Export-all zip opens; each md readable in VS Code
- [ ] Seed preview shows 5 companies before import
- [ ] Template preview shows checklist lines before fill

---

## 8. Documentation

Update `README.md`:

- Feature matrix A2/A1/A4 rows
- Routes table: `/interviews/search`, `/applications/export-all`, preview query params
- Phase 6 row in演进历史
- Remove or move completed items from「后续规划」

Update `JOURNAL.md` when Phase 6 ships (optional, user-driven).

---

## 9. Implementation Tasks (outline for plan)

| Task | Scope |
|------|-------|
| 26 | Interview search (repo, service, controller, template, nav, tests) |
| 27 | Bulk zip export (service, controller, list UI, tests) |
| 28 | Company seed preview (service method, companies list UI, tests) |
| 29 | Template preview (controller, detail UI, tests) |
| 30 | README + JOURNAL note |

Each task: one commit, conventional message.

---

## 10. Rollback & Risks

| Risk | Mitigation |
|------|------------|
| LIKE on LOB slow at scale | 500-app cap on bulk export; search acceptable for solo user (<1000 notes) |
| Zip OOM | Stream to temp file if >50 apps in future; v1 in-memory OK for <500 small md files |
| Duplicate md filenames | Append application id |
| Preview shows stale JSON | Preview reads same classpath JSON as import/fill |

Rollback: each feature independently revertable; no schema migration required.

---

## 11. Phase 7 Backlog (explicit defer)

- Spaced repetition review queue
- Question bank / frequency view (方案 B)
- Today follow-up dashboard widget
- Search prep checklist + JD fields
- JSON backup import
