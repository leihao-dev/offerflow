# OfferFlow Phase 7b Design Spec — Debrief Hub + Pre-Phase 8 Tech Debt

**Date:** 2026-07-12  
**Status:** Approved (2026-07-12)  
**Author:** Brainstorming session with Ray  
**Scope:** Enhance `/interviews/search` into a debrief hub (browse + search); fix related UX/tech debt before Phase 8 kanban

## 1. Problem & Goal

### Problem

After Phase 6–7, two gaps block daily A2 use and Phase 8 readiness:

| Gap | Symptom |
|-----|---------|
| Debrief discovery | `/interviews/search` shows nothing without `?q=` — user cannot browse existing debriefs |
| Action parity | Search results only link to application detail; no「详情 / 编辑」like application list |
| Spec drift | Phase 6 intentionally hid all notes on empty query; product intent now changed |
| Minor debt | Nav label「复盘搜索」misleading; application detail lacks anchor for deep-link to one debrief |

Search implementation loads all notes in memory (Phase 6 workaround for `@Lob` + JPQL `LOWER`). Acceptable for personal scale; browse mode adds a bounded `Pageable` path.

### Goal

**Phase 7b** delivers **方案 A — Service 分层 + 浏览/搜索双模式**:

1. **Debrief hub** — default show **recent 50** debriefs; search still scans full corpus
2. **Table actions** —「详情」→ `/applications/{id}#note-{noteId}`；「编辑」→ `/interviews/{id}/edit`
3. **Anchors** — application detail debrief cards get `id="note-{id}"` + optional `:target` highlight
4. **Nav** — sidebar「复盘记录」; URL stays `/interviews/search`
5. **Tech debt bundle** — unified empty states, tests, README/JOURNAL

### Success Criteria

- `GET /interviews/search` (no `q`) shows up to 50 most recent debriefs in `.data-table`
- Hint text:「显示最近 50 条复盘，输入关键词可搜索全部」
- `GET /interviews/search?q=线程池` filters all notes (not limited to 50)
- Each row: 详情 (anchor link) + 编辑
- `GET /applications/{id}#note-{noteId}` scrolls to correct card with visible highlight
- Sidebar label「复盘记录」
- `.\gradlew.bat test` green including new/updated web tests
- README Phase 7b row + route description updated

### Non-Goals (Phase 7b)

- Phase 8 kanban / drag-and-drop
- Native SQL / DB full-text search refactor
- New read-only debrief detail page
- URL change from `/interviews/search` to `/interviews`
- Pagination UI (beyond fixed 50 recent cap)
- JSON import, SRS, auth, MySQL

### Brainstorming decisions

- **Scope:** B — debrief hub + selected tech debt
- **Default browse:** recent 50 (not all, not paginated UI)
- **Row actions:** 详情 (anchor) + 编辑
- **Nav:** label only「复盘记录」, URL unchanged

---

## 2. Chosen Approach

**方案 A (approved):** `InterviewSearchService.listRecent()` with `Pageable`; existing in-memory `search(q)` for full corpus; reuse `InterviewSearchHit` DTO.

### Rejected alternatives

| Approach | Reason rejected |
|----------|-----------------|
| B: H2 native SQL LIKE on CLOB | DB dialect coupling; high test cost for marginal gain at personal scale |
| C: Template-only, no Service | Logic untestable; breaks conventions |

---

## 3. Backend

### Constants

`com.offerflow.support.DebriefLimits`:

```java
public static final int RECENT_DEBRIEF_LIMIT = 50;
```

(Separate from `ExportLimits` — different domain.)

### Repository

Add to `InterviewNoteRepository`:

```java
@Query("""
        SELECT n FROM InterviewNote n JOIN FETCH n.application a
        ORDER BY n.interviewDate DESC, n.createdAt DESC""")
List<InterviewNote> findRecentWithApplication(Pageable pageable);
```

Spring Data applies `Pageable` limit — no duplicate query logic.

### Service

`InterviewSearchService`:

| Method | Behavior |
|--------|----------|
| `listRecent()` | `findRecentWithApplication(PageRequest.of(0, RECENT_DEBRIEF_LIMIT))` → map to `InterviewSearchHit` with browse snippet |
| `search(q)` | unchanged full-list filter; empty q returns empty list |

**Browse snippet:** first non-blank of `questionsAsked`, `selfAssessment`, `improvements` — truncate 120 chars (reuse `truncate` logic, no match keyword needed).

### Controller

`InterviewSearchController.search(q)`:

| Condition | Model |
|-----------|-------|
| No `q` | `hits = listRecent()`, `hasQuery=false`, `showingRecent=true` |
| Has `q` | `hits = search(q)`, `hasQuery=true`, `showingRecent=false` |

Always `searchQuery=q` (may be null).

### DTO

`InterviewSearchHit` unchanged — already has `noteId`, `applicationId`, fields for table.

---

## 4. UI — `interviews/search.html`

Align with `applications/list.html` patterns:

```
page-header (via layout): 复盘记录 / 跨全部投递浏览与搜索面试复盘。
toolbar card: search + submit + clear
muted hint (showingRecent): 显示最近 50 条…
filter-banner (hasQuery): 正在搜索「{q}」+ 查看全部
data-table: 日期 | 公司·岗位 | 轮次 | 摘要 | 操作(详情/编辑)
empty states:
  - no debriefs at all (recent empty, no query)
  - no search hits (has query)
```

**Links:**

- 详情: `@{/applications/{id}(id=${hit.applicationId()})} + '#note-' + ${hit.noteId()}`
- 编辑: `@{/interviews/{id}/edit(id=${hit.noteId()})}`

Remove old single「查看投递」button.

---

## 5. Application Detail Anchor

`applications/detail.html`:

```html
<div th:id="'note-' + ${note.id}" class="card nested-card debrief-card">...</div>
```

`components.css`:

```css
.debrief-card:target {
    box-shadow: 0 0 0 2px var(--color-primary);
    background: var(--color-primary-light);
}
```

---

## 6. Navigation

`fragments/nav.html`: label「复盘搜索」→「复盘记录」  
`interviews/search.html` layout title/subtitle updated to match.

---

## 7. Testing

| Test | Assertion |
|------|-----------|
| `InterviewSearchServiceTest.listRecentRespectsLimit` | 51 notes → listRecent size 50 |
| `InterviewSearchWebTest.recentDebriefsShownWithoutQuery` | GET `/interviews/search` contains company name |
| `InterviewSearchWebTest.searchPageFindsDebriefContent` | existing, keep |
| `InterviewSearchWebTest.detailLinkContainsNoteAnchor` | response contains `#note-` |
| `CompanyWebTest` or nav test | contains「复盘记录」 |

---

## 8. Documentation

- README: Phase 7b evolution row; `/interviews/search` description (browse + search); test count if new test class/methods only
- JOURNAL: Phase 7b bullet under Day 3

---

## 9. File Manifest

**New:** `support/DebriefLimits.java`, `InterviewSearchServiceTest.java` (if not exists)

**Modify:** `InterviewNoteRepository`, `InterviewSearchService`, `InterviewSearchController`, `interviews/search.html`, `applications/detail.html`, `components.css`, `nav.html`, tests, README, JOURNAL

---

## 10. Open Questions

None — approved in brainstorming 2026-07-12.
