# Phase 7b Debrief Hub + Tech Debt Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `/interviews/search` into a debrief hub (recent 50 + full search), add 详情/编辑 row actions with application anchors, rename nav to「复盘记录」.

**Architecture:** `DebriefLimits.RECENT_DEBRIEF_LIMIT`; repository `Pageable` for browse; existing in-memory `search(q)` for full corpus; template aligned with application list; `:target` CSS on detail debrief cards.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Thymeleaf, Spring Data JPA, JUnit 5, MockMvc

**Spec:** `docs/superpowers/specs/2026-07-12-phase7b-debrief-hub-design.md`

**Prerequisite:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cd C:\Users\Ray\offerflow
```

---

## File Map

| File | Responsibility |
|------|----------------|
| `support/DebriefLimits.java` | `RECENT_DEBRIEF_LIMIT = 50` |
| `repository/InterviewNoteRepository.java` | +`findRecentWithApplication(Pageable)` |
| `service/InterviewSearchService.java` | +`listRecent()`, browse snippet helper |
| `controller/InterviewSearchController.java` | dual mode: recent vs search |
| `templates/interviews/search.html` | hub UI like application list |
| `templates/applications/detail.html` | `id="note-{id}"` on debrief cards |
| `static/css/components.css` | `.debrief-card:target` highlight |
| `templates/fragments/nav.html` |「复盘记录」label |
| `test/InterviewSearchServiceTest.java` | listRecent limit |
| `test/InterviewSearchWebTest.java` | browse + anchor tests |

---

### Task 41: DebriefLimits + listRecent service

**Files:**
- Create: `src/main/java/com/offerflow/support/DebriefLimits.java`
- Modify: `src/main/java/com/offerflow/repository/InterviewNoteRepository.java`
- Modify: `src/main/java/com/offerflow/service/InterviewSearchService.java`
- Create: `src/test/java/com/offerflow/InterviewSearchServiceTest.java`

- [ ] **Step 1: Write failing service test**

Create `src/test/java/com/offerflow/InterviewSearchServiceTest.java`:

```java
package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.dto.InterviewSearchHit;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.InterviewNoteService;
import com.offerflow.service.InterviewSearchService;
import com.offerflow.service.JobApplicationService;
import com.offerflow.support.DebriefLimits;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
class InterviewSearchServiceTest {

    @Autowired
    private InterviewSearchService searchService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private InterviewNoteService interviewNoteService;

    @Test
    void listRecentRespectsLimit() {
        var app = applicationService.create(sampleApp("LimitCo", "Java"));
        for (int i = 0; i < DebriefLimits.RECENT_DEBRIEF_LIMIT + 1; i++) {
            InterviewNoteForm note = new InterviewNoteForm();
            note.setApplicationId(app.getId());
            note.setInterviewDate(LocalDate.of(2026, 7, 1).plusDays(i));
            note.setQuestionsAsked("note-" + i);
            interviewNoteService.create(note);
        }

        List<InterviewSearchHit> recent = searchService.listRecent();

        assertEquals(DebriefLimits.RECENT_DEBRIEF_LIMIT, recent.size());
    }

    @Test
    void listRecentOrdersByInterviewDateDesc() {
        var app = applicationService.create(sampleApp("OrderCo", "Go"));
        createNote(app.getId(), LocalDate.of(2026, 7, 1), "older");
        createNote(app.getId(), LocalDate.of(2026, 7, 15), "newer");

        List<InterviewSearchHit> recent = searchService.listRecent();

        assertFalse(recent.isEmpty());
        assertEquals(LocalDate.of(2026, 7, 15), recent.get(0).interviewDate());
    }

    @Test
    void searchStillFindsAcrossAllNotes() {
        var app = applicationService.create(sampleApp("SearchCo", "Java"));
        createNote(app.getId(), LocalDate.of(2026, 1, 1), "UniquePhase7bMarker");

        List<InterviewSearchHit> hits = searchService.search("UniquePhase7bMarker");

        assertEquals(1, hits.size());
        assertEquals("SearchCo", hits.get(0).companyName());
    }

    private void createNote(Long appId, LocalDate date, String questions) {
        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(appId);
        note.setInterviewDate(date);
        note.setQuestionsAsked(questions);
        interviewNoteService.create(note);
    }

    private static ApplicationForm sampleApp(String company, String title) {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName(company);
        form.setPositionTitle(title);
        form.setStage(ApplicationStage.APPLIED);
        form.setAppliedAt(LocalDate.of(2026, 7, 12));
        return form;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewSearchServiceTest" --no-daemon
```

Expected: FAIL — `DebriefLimits` or `listRecent()` not found

- [ ] **Step 3: Create `DebriefLimits.java`**

```java
package com.offerflow.support;

public final class DebriefLimits {

    public static final int RECENT_DEBRIEF_LIMIT = 50;

    private DebriefLimits() {}
}
```

- [ ] **Step 4: Add repository method**

In `InterviewNoteRepository.java` add imports `org.springframework.data.domain.Pageable` and:

```java
@Query("""
        SELECT n FROM InterviewNote n JOIN FETCH n.application a
        ORDER BY n.interviewDate DESC, n.createdAt DESC""")
List<InterviewNote> findRecentWithApplication(Pageable pageable);
```

- [ ] **Step 5: Implement `listRecent()` in `InterviewSearchService`**

Add imports:

```java
import com.offerflow.support.DebriefLimits;
import org.springframework.data.domain.PageRequest;
```

Add method:

```java
@Transactional(readOnly = true)
public List<InterviewSearchHit> listRecent() {
    return interviewNoteRepository
            .findRecentWithApplication(PageRequest.of(0, DebriefLimits.RECENT_DEBRIEF_LIMIT))
            .stream()
            .map(this::toBrowseHit)
            .toList();
}

private InterviewSearchHit toBrowseHit(InterviewNote note) {
    JobApplication app = note.getApplication();
    String snippet = truncate(
            coalesce(note.getQuestionsAsked(), note.getSelfAssessment(), note.getImprovements()),
            SNIPPET_LENGTH);
    return new InterviewSearchHit(
            note.getId(),
            app.getId(),
            app.getCompanyName(),
            app.getPositionTitle(),
            note.getInterviewDate(),
            note.getRoundLabel(),
            snippet != null ? snippet : "");
}
```

Adjust `truncate` / `coalesce` to handle null coalesce result (return `""` if all fields blank).

- [ ] **Step 6: Run test to verify it passes**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewSearchServiceTest" --no-daemon
```

Expected: PASS

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/offerflow/support/DebriefLimits.java src/main/java/com/offerflow/repository/InterviewNoteRepository.java src/main/java/com/offerflow/service/InterviewSearchService.java src/test/java/com/offerflow/InterviewSearchServiceTest.java
git commit -m "feat(interview): add recent debrief list service"
```

---

### Task 42: Controller + search template hub UI

**Files:**
- Modify: `src/main/java/com/offerflow/controller/InterviewSearchController.java`
- Modify: `src/main/resources/templates/interviews/search.html`

- [ ] **Step 1: Write failing web test**

Add to `InterviewSearchWebTest.java`:

```java
@Test
void recentDebriefsShownWithoutQuery() throws Exception {
    String redirectUrl = mockMvc.perform(post("/applications")
                    .param("companyName", "BrowseVisibleCo")
                    .param("positionTitle", "Java")
                    .param("stage", "APPLIED")
                    .param("appliedAt", "2026-07-12"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getRedirectedUrl();
    String appId = redirectUrl.replace("/applications/", "");

    mockMvc.perform(post("/applications/" + appId + "/interviews")
                    .param("interviewDate", "2026-07-10")
                    .param("questionsAsked", "browse test content"))
            .andExpect(status().is3xxRedirection());

    mockMvc.perform(get("/interviews/search"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("BrowseVisibleCo")))
            .andExpect(content().string(containsString("显示最近 50 条")));
}

@Test
void detailLinkContainsNoteAnchor() throws Exception {
    String redirectUrl = mockMvc.perform(post("/applications")
                    .param("companyName", "AnchorCo")
                    .param("positionTitle", "Java")
                    .param("stage", "APPLIED")
                    .param("appliedAt", "2026-07-12"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getRedirectedUrl();
    String appId = redirectUrl.replace("/applications/", "");

    mockMvc.perform(post("/applications/" + appId + "/interviews")
                    .param("interviewDate", "2026-07-10")
                    .param("questionsAsked", "anchor test"))
            .andExpect(status().is3xxRedirection());

    mockMvc.perform(get("/interviews/search"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("#note-")))
            .andExpect(content().string(containsString("/interviews/")))
            .andExpect(content().string(containsString("/edit")));
}
```

- [ ] **Step 2: Run tests to verify they fail**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewSearchWebTest.recentDebriefsShownWithoutQuery" --tests "com.offerflow.InterviewSearchWebTest.detailLinkContainsNoteAnchor" --no-daemon
```

Expected: FAIL

- [ ] **Step 3: Update controller**

```java
@GetMapping("/search")
public String search(@RequestParam(required = false) String q, Model model) {
    model.addAttribute("searchQuery", q);
    boolean hasQuery = q != null && !q.trim().isEmpty();
    model.addAttribute("hasQuery", hasQuery);
    if (hasQuery) {
        model.addAttribute("hits", searchService.search(q));
        model.addAttribute("showingRecent", false);
    } else {
        model.addAttribute("hits", searchService.listRecent());
        model.addAttribute("showingRecent", true);
    }
    return "interviews/search";
}
```

- [ ] **Step 4: Rewrite `interviews/search.html`**

Replace `#pageBody` content with hub layout (key structure):

```html
<th:block id="pageBody">
    <div class="filter-banner" th:if="${hasQuery}">
        <span>正在搜索「<span th:text="${searchQuery}"></span>」</span>
        <a class="btn btn-text btn-sm" th:href="@{/interviews/search}">查看全部</a>
    </div>

    <div class="card">
        <form class="toolbar" method="get" th:action="@{/interviews/search}">
            <input class="search-input" name="q" type="search" placeholder="例如：线程池、HashMap、项目深挖…"
                   th:value="${searchQuery}"/>
            <button class="btn btn-primary btn-sm" type="submit">搜索</button>
            <a class="btn btn-text btn-sm" th:if="${hasQuery}" th:href="@{/interviews/search}">清除</a>
        </form>
    </div>

    <p class="muted" th:if="${showingRecent and !#lists.isEmpty(hits)}">
        显示最近 50 条复盘，输入关键词可搜索全部记录。
    </p>

    <div class="card" th:if="${!#lists.isEmpty(hits)}">
        <table class="data-table">
            <thead>
            <tr><th>日期</th><th>公司 · 岗位</th><th>轮次</th><th>摘要</th><th></th></tr>
            </thead>
            <tbody>
            <tr th:each="hit : ${hits}">
                <td th:text="${hit.interviewDate()}">日期</td>
                <td>
                    <strong th:text="${hit.companyName()}">公司</strong>
                    <span class="muted" th:text="' · ' + ${hit.positionTitle()}">岗位</span>
                </td>
                <td th:text="${hit.roundLabel() != null ? hit.roundLabel() : '-'}">轮次</td>
                <td class="muted" th:text="${hit.snippet()}">摘要</td>
                <td class="actions">
                    <a class="btn btn-text btn-sm"
                       th:href="@{/applications/{id}(id=${hit.applicationId()})} + '#note-' + ${hit.noteId()}">详情</a>
                    <a class="btn btn-text btn-sm"
                       th:href="@{/interviews/{id}/edit(id=${hit.noteId()})}">编辑</a>
                </td>
            </tr>
            </tbody>
        </table>
    </div>

    <div th:if="${#lists.isEmpty(hits) and hasQuery}" class="card empty-state">
        <p>没有匹配「<span th:text="${searchQuery}"></span>」的复盘</p>
        <a class="btn btn-text" th:href="@{/interviews/search}">查看全部</a>
    </div>

    <div th:if="${#lists.isEmpty(hits) and !hasQuery}" class="card empty-state">
        <p>还没有面试复盘</p>
        <a class="btn btn-primary" th:href="@{/applications}">去投递列表添加</a>
    </div>
</th:block>
```

Update layout shell title/subtitle in the `<html th:replace=...>` line:

```html
th:replace="~{fragments/layout :: shell('interviews-search', '复盘记录', '浏览与搜索全部面试复盘。', null, ~{::#pageBody})}"
```

- [ ] **Step 5: Run web tests**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewSearchWebTest" --no-daemon
```

Expected: PASS

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/com/offerflow/controller/InterviewSearchController.java src/main/resources/templates/interviews/search.html src/test/java/com/offerflow/InterviewSearchWebTest.java
git commit -m "feat(interview): redesign debrief hub with browse and row actions"
```

---

### Task 43: Detail anchors + nav rename

**Files:**
- Modify: `src/main/resources/templates/applications/detail.html`
- Modify: `src/main/resources/static/css/components.css`
- Modify: `src/main/resources/templates/fragments/nav.html`
- Modify: `src/test/java/com/offerflow/CompanyWebTest.java`

- [ ] **Step 1: Add anchor id to debrief cards**

In `applications/detail.html`, change debrief card wrapper:

```html
<div th:each="note : ${jobApplication.interviewNotes}"
     th:id="'note-' + ${note.id}"
     class="card nested-card debrief-card">
```

- [ ] **Step 2: Add `:target` CSS**

In `components.css` after `.nested-card`:

```css
.debrief-card:target {
    box-shadow: 0 0 0 2px var(--color-primary);
    background: var(--color-primary-light);
}
```

- [ ] **Step 3: Rename nav label**

In `nav.html` line 12: `复盘搜索` → `复盘记录`

- [ ] **Step 4: Update nav test**

In `CompanyWebTest.navIncludesCompaniesLink`, add or change assertion:

```java
.andExpect(content().string(containsString("复盘记录")));
```

(Keep「目标公司」assertion.)

- [ ] **Step 5: Run tests**

```powershell
.\gradlew.bat test --tests "com.offerflow.CompanyWebTest.navIncludesCompaniesLink" --no-daemon
```

Expected: PASS

- [ ] **Step 6: Commit**

```powershell
git add src/main/resources/templates/applications/detail.html src/main/resources/static/css/components.css src/main/resources/templates/fragments/nav.html src/test/java/com/offerflow/CompanyWebTest.java
git commit -m "feat(ui): add debrief anchors and rename nav to 复盘记录"
```

---

### Task 44: Documentation

**Files:**
- Modify: `README.md`
- Modify: `JOURNAL.md`

- [ ] **Step 1: Update README**

- Intro or Phase 7b bullet: debrief hub at `/interviews/search`
- Routes table: `/interviews/search` — 复盘记录（默认最近 50 条，`?q=` 搜索全部）
- Evolution table row:

```markdown
| Phase 7b | A2 + 债务 | 复盘记录 hub、详情锚点、侧栏改名 |
```

- Design doc link:

```markdown
| [`2026-07-12-phase7b-debrief-hub-design.md`](docs/superpowers/specs/2026-07-12-phase7b-debrief-hub-design.md) | Phase 7b 复盘 hub |
```

- Test count: 16 → 19 if `InterviewSearchServiceTest` is new (18 + 1 = 19)

- [ ] **Step 2: Update JOURNAL**

Add under Day 3:

```markdown
- **Phase 7b（复盘 hub + 技术债，Task 41–44）：**
  - 复盘记录页默认最近 50 条 + 搜索；详情锚点；侧栏改名
```

- [ ] **Step 3: Run full tests**

```powershell
.\gradlew.bat test --no-daemon
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```powershell
git add README.md JOURNAL.md
git commit -m "docs: document Phase 7b debrief hub"
```

---

## Spec Coverage Checklist

| Spec § | Task |
|--------|------|
| DebriefLimits + listRecent | 41 |
| search(q) unchanged | 41 |
| Controller dual mode | 42 |
| Hub template | 42 |
| 详情/编辑 links | 42 |
| Detail anchor + CSS | 43 |
| Nav rename | 43 |
| Tests | 41, 42, 43 |
| README/JOURNAL | 44 |

## Manual QA (post-Task 44)

1. `/interviews/search` — table shows recent debriefs without typing
2. Search「线程池」— filters results;「查看全部」clears
3. Click「详情」— lands on application detail with debrief card highlighted
4. Click「编辑」— opens interview form
5. Sidebar shows「复盘记录」

---

**Plan complete.** Two execution options:

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks  
2. **Inline Execution** — run tasks sequentially in this session with checkpoints

Which approach?
