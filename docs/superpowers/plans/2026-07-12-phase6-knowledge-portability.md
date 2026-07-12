# Phase 6 Knowledge Base + Portability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add debrief full-text search page, bulk Markdown zip export, and read-only seed/template previews — Phase 6「可带走的知识库」.

**Architecture:** Three independent features on existing entities/services: `InterviewNoteRepository` LIKE query + `InterviewSearchService`; `BulkExportService` wrapping `MarkdownExportService` + `ZipOutputStream`; preview DTOs fed from existing seed/template JSON registries. No schema changes.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Thymeleaf, Spring Data JPA, JUnit 5, MockMvc

**Spec:** `docs/superpowers/specs/2026-07-12-phase6-knowledge-portability-design.md`

**Prerequisite:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cd C:\Users\Ray\offerflow
```

---

## File Map

| File | Responsibility |
|------|----------------|
| `dto/InterviewSearchHit.java` | Search result row |
| `dto/SeedPreviewView.java` | Seed pack preview (title, samples) |
| `dto/SeedPreviewEntry.java` | One sample company `{name, industry}` |
| `dto/TemplatePreviewView.java` | Template preview (prep excerpt, debrief summary) |
| `support/ExportLimits.java` | `MAX_BULK_EXPORT = 500` |
| `service/InterviewSearchService.java` | Query + snippet building |
| `service/BulkExportService.java` | Zip all applications as Markdown |
| `service/CompanySeedService.java` | +`previewSeed(String seedId)` |
| `service/InterviewTemplateService.java` | +`previewTemplate(String templateId)` |
| `repository/InterviewNoteRepository.java` | +`searchByQuery` |
| `controller/InterviewSearchController.java` | `GET /interviews/search` |
| `controller/ApplicationController.java` | +`export-all`, +`preview-template` |
| `controller/CompanyController.java` | +`previewSeed` param on list |
| `templates/interviews/search.html` | Search UI |
| `templates/applications/template-preview.html` | Template preview page |
| `templates/applications/list.html` | Bulk export button |
| `templates/applications/detail.html` | Preview links per template |
| `templates/companies/list.html` | Seed preview panel + GET preview form |
| `templates/fragments/nav.html` |「复盘搜索」nav link |

---

### Task 26: Debrief full-text search

**Files:**
- Create: `src/main/java/com/offerflow/dto/InterviewSearchHit.java`
- Create: `src/main/java/com/offerflow/service/InterviewSearchService.java`
- Create: `src/main/java/com/offerflow/controller/InterviewSearchController.java`
- Create: `src/main/resources/templates/interviews/search.html`
- Create: `src/test/java/com/offerflow/InterviewSearchServiceTest.java`
- Create: `src/test/java/com/offerflow/InterviewSearchWebTest.java`
- Modify: `src/main/java/com/offerflow/repository/InterviewNoteRepository.java`
- Modify: `src/main/resources/templates/fragments/nav.html`

- [ ] **Step 1: Write failing repository/service test**

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
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void searchMatchesQuestionsAsked() {
        var app = applicationService.create(sampleApp("美团", "Java 后端"));
        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(app.getId());
        note.setInterviewDate(LocalDate.of(2026, 7, 10));
        note.setQuestionsAsked("线程池原理与拒绝策略");
        interviewNoteService.create(note);

        List<InterviewSearchHit> hits = searchService.search("线程池");

        assertEquals(1, hits.size());
        assertEquals("美团", hits.get(0).companyName());
        assertTrue(hits.get(0).snippet().contains("线程"));
    }

    @Test
    void searchMatchesCompanyName() {
        var app = applicationService.create(sampleApp("字节跳动", "Go 后端"));
        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(app.getId());
        note.setInterviewDate(LocalDate.now());
        note.setQuestionsAsked("基础问题");
        interviewNoteService.create(note);

        List<InterviewSearchHit> hits = searchService.search("字节");

        assertEquals(1, hits.size());
        assertEquals("字节跳动", hits.get(0).companyName());
    }

    @Test
    void searchReturnsEmptyForBlankQuery() {
        assertTrue(searchService.search("   ").isEmpty());
        assertTrue(searchService.search(null).isEmpty());
    }

    private ApplicationForm sampleApp(String company, String position) {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName(company);
        form.setPositionTitle(position);
        form.setStage(ApplicationStage.TECH_INTERVIEW);
        form.setAppliedAt(LocalDate.now());
        return form;
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewSearchServiceTest" --no-daemon
```

Expected: compilation failure — classes not found.

- [ ] **Step 3: Add DTO and repository query**

Create `src/main/java/com/offerflow/dto/InterviewSearchHit.java`:

```java
package com.offerflow.dto;

import java.time.LocalDate;

public record InterviewSearchHit(
        Long noteId,
        Long applicationId,
        String companyName,
        String positionTitle,
        LocalDate interviewDate,
        String roundLabel,
        String snippet) {}
```

Add to `InterviewNoteRepository.java`:

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

- [ ] **Step 4: Implement InterviewSearchService**

Create `src/main/java/com/offerflow/service/InterviewSearchService.java`:

```java
package com.offerflow.service;

import com.offerflow.dto.InterviewSearchHit;
import com.offerflow.model.InterviewNote;
import com.offerflow.model.JobApplication;
import com.offerflow.repository.InterviewNoteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InterviewSearchService {

    private static final int MAX_QUERY_LENGTH = 200;
    private static final int SNIPPET_LENGTH = 120;

    private final InterviewNoteRepository interviewNoteRepository;

    public InterviewSearchService(InterviewNoteRepository interviewNoteRepository) {
        this.interviewNoteRepository = interviewNoteRepository;
    }

    public List<InterviewSearchHit> search(String rawQuery) {
        String q = normalizeQuery(rawQuery);
        if (q.isEmpty()) {
            return List.of();
        }
        return interviewNoteRepository.searchByQuery(q).stream()
                .map(note -> toHit(note, q))
                .toList();
    }

    private static String normalizeQuery(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        String trimmed = rawQuery.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            return trimmed.substring(0, MAX_QUERY_LENGTH);
        }
        return trimmed;
    }

    private InterviewSearchHit toHit(InterviewNote note, String q) {
        JobApplication app = note.getApplication();
        return new InterviewSearchHit(
                note.getId(),
                app.getId(),
                app.getCompanyName(),
                app.getPositionTitle(),
                note.getInterviewDate(),
                note.getRoundLabel(),
                buildSnippet(note, q));
    }

    private String buildSnippet(InterviewNote note, String q) {
        String source = firstMatchingField(note, q);
        if (source == null || source.isBlank()) {
            source = coalesce(note.getQuestionsAsked(), note.getSelfAssessment(), note.getImprovements());
        }
        if (source == null) {
            return "";
        }
        String flat = source.replace('\n', ' ').trim();
        int idx = flat.toLowerCase().indexOf(q.toLowerCase());
        if (idx < 0) {
            return truncate(flat, SNIPPET_LENGTH);
        }
        int start = Math.max(0, idx - 40);
        int end = Math.min(flat.length(), idx + q.length() + 60);
        String slice = flat.substring(start, end);
        if (start > 0) {
            slice = "…" + slice;
        }
        if (end < flat.length()) {
            slice = slice + "…";
        }
        return slice;
    }

    private static String firstMatchingField(InterviewNote note, String q) {
        String lower = q.toLowerCase();
        for (String field : List.of(
                note.getQuestionsAsked(),
                note.getSelfAssessment(),
                note.getImprovements(),
                note.getRoundLabel())) {
            if (field != null && field.toLowerCase().contains(lower)) {
                return field;
            }
        }
        return null;
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "…";
    }
}
```

- [ ] **Step 5: Run service test — expect PASS**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewSearchServiceTest" --no-daemon
```

- [ ] **Step 6: Add controller and template**

Create `src/main/java/com/offerflow/controller/InterviewSearchController.java`:

```java
package com.offerflow.controller;

import com.offerflow.service.InterviewSearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/interviews")
public class InterviewSearchController {

    private final InterviewSearchService searchService;

    public InterviewSearchController(InterviewSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("searchQuery", q);
        boolean hasQuery = q != null && !q.trim().isEmpty();
        model.addAttribute("hasQuery", hasQuery);
        if (hasQuery) {
            model.addAttribute("hits", searchService.search(q));
        }
        return "interviews/search";
    }
}
```

Create `src/main/resources/templates/interviews/search.html`:

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>复盘搜索 · OfferFlow</title>
    <link rel="stylesheet" th:href="@{/css/app.css}"/>
</head>
<body>
<header th:replace="~{fragments/nav :: nav('interviews-search')}"></header>
<main class="container">
    <h1>复盘搜索</h1>
    <p class="muted" th:if="${!hasQuery}">输入关键词搜索全部面试复盘（问题、自评、改进、公司名、岗位）。</p>

    <div class="card" style="margin-bottom:16px;">
        <form class="actions" method="get" th:action="@{/interviews/search}">
            <input name="q" type="search" placeholder="例如：线程池、HashMap、项目深挖…"
                   th:value="${searchQuery}"
                   style="flex:1;min-width:180px;padding:8px 10px;border:1px solid var(--border);border-radius:8px;font:inherit;"/>
            <button class="btn btn-primary btn-sm" type="submit">搜索</button>
            <a class="btn btn-sm" th:if="${hasQuery}" th:href="@{/interviews/search}">清除</a>
        </form>
    </div>

    <div th:if="${hasQuery and #lists.isEmpty(hits)}" class="card muted">
        没有匹配「<span th:text="${searchQuery}"></span>」的复盘。
    </div>

    <div th:if="${hasQuery and !#lists.isEmpty(hits)}" class="card">
        <table>
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
                <td><a class="btn btn-sm" th:href="@{/applications/{id}(id=${hit.applicationId()})}">查看投递</a></td>
            </tr>
            </tbody>
        </table>
    </div>
</main>
</body>
</html>
```

Update `src/main/resources/templates/fragments/nav.html` — insert after 投递列表 link:

```html
            <a th:href="@{/interviews/search}"
               th:classappend="${activePage == 'interviews-search'} ? ' nav-active' : ''">复盘搜索</a>
```

- [ ] **Step 7: Write web test**

Create `src/test/java/com/offerflow/InterviewSearchWebTest.java`:

```java
package com.offerflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterviewSearchWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void searchPageFindsDebriefContent() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "搜索测试公司")
                        .param("positionTitle", "Java")
                        .param("stage", "TECH_INTERVIEW")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(post("/applications/" + appId + "/interviews")
                        .param("interviewDate", "2026-07-12")
                        .param("questionsAsked", "UniqueKeyword线程池XYZ"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/interviews/search").param("q", "UniqueKeyword线程池"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("搜索测试公司")));
    }
}
```

Check interview create route — it's `POST /applications/{applicationId}/interviews` from InterviewController.

- [ ] **Step 8: Run all Task 26 tests**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewSearch*" --no-daemon
```

- [ ] **Step 9: Commit**

```powershell
git add src/main/java/com/offerflow/dto/InterviewSearchHit.java `
        src/main/java/com/offerflow/service/InterviewSearchService.java `
        src/main/java/com/offerflow/controller/InterviewSearchController.java `
        src/main/java/com/offerflow/repository/InterviewNoteRepository.java `
        src/main/resources/templates/interviews/search.html `
        src/main/resources/templates/fragments/nav.html `
        src/test/java/com/offerflow/InterviewSearchServiceTest.java `
        src/test/java/com/offerflow/InterviewSearchWebTest.java
git commit -m "feat(interview): add debrief full-text search page"
```

---

### Task 27: Bulk Markdown zip export

**Files:**
- Create: `src/main/java/com/offerflow/support/ExportLimits.java`
- Create: `src/main/java/com/offerflow/service/BulkExportService.java`
- Create: `src/test/java/com/offerflow/BulkExportServiceTest.java`
- Modify: `src/main/java/com/offerflow/controller/ApplicationController.java`
- Modify: `src/main/resources/templates/applications/list.html`
- Modify: `src/test/java/com/offerflow/ApplicationWebTest.java`

- [ ] **Step 1: Write failing BulkExportServiceTest**

Create `src/main/java/com/offerflow/support/ExportLimits.java`:

```java
package com.offerflow.support;

public final class ExportLimits {

    public static final int MAX_BULK_EXPORT = 500;

    private ExportLimits() {}
}
```

Create `src/test/java/com/offerflow/BulkExportServiceTest.java`:

```java
package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.BulkExportService;
import com.offerflow.service.JobApplicationService;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class BulkExportServiceTest {

    @Autowired
    private BulkExportService bulkExportService;

    @Autowired
    private JobApplicationService applicationService;

    @Test
    void exportAllCreatesZipWithMarkdownEntries() throws Exception {
        ApplicationForm a = new ApplicationForm();
        a.setCompanyName("公司A");
        a.setPositionTitle("岗位A");
        a.setStage(ApplicationStage.APPLIED);
        a.setAppliedAt(LocalDate.now());
        applicationService.create(a);

        ApplicationForm b = new ApplicationForm();
        b.setCompanyName("公司B");
        b.setPositionTitle("岗位B");
        b.setStage(ApplicationStage.APPLIED);
        b.setAppliedAt(LocalDate.now());
        applicationService.create(b);

        byte[] zip = bulkExportService.exportAllAsZip();

        int entries = 0;
        boolean foundMarkdown = false;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entry.getName().endsWith(".md")) {
                    foundMarkdown = true;
                    byte[] buf = zis.readAllBytes();
                    String content = new String(buf, java.nio.charset.StandardCharsets.UTF_8);
                    assertTrue(content.contains("# "));
                }
            }
        }
        assertTrue(entries >= 2);
        assertTrue(foundMarkdown);
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```powershell
.\gradlew.bat test --tests "com.offerflow.BulkExportServiceTest" --no-daemon
```

- [ ] **Step 3: Implement BulkExportService**

Create `src/main/java/com/offerflow/service/BulkExportService.java`:

```java
package com.offerflow.service;

import com.offerflow.model.JobApplication;
import com.offerflow.repository.JobApplicationRepository;
import com.offerflow.support.ExportLimits;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BulkExportService {

    private final JobApplicationRepository applicationRepository;
    private final MarkdownExportService markdownExportService;

    public BulkExportService(
            JobApplicationRepository applicationRepository, MarkdownExportService markdownExportService) {
        this.applicationRepository = applicationRepository;
        this.markdownExportService = markdownExportService;
    }

    public long countApplications() {
        return applicationRepository.count();
    }

    public boolean exceedsBulkLimit() {
        return countApplications() > ExportLimits.MAX_BULK_EXPORT;
    }

    public byte[] exportAllAsZip() {
        List<JobApplication> apps = applicationRepository.findAllByOrderByUpdatedAtDesc();
        if (apps.size() > ExportLimits.MAX_BULK_EXPORT) {
            apps = apps.subList(0, ExportLimits.MAX_BULK_EXPORT);
        }
        Set<String> usedNames = new HashSet<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (JobApplication app : apps) {
                String entryName = uniqueEntryName(app, usedNames);
                zos.putNextEntry(new ZipEntry(entryName));
                String markdown = markdownExportService.export(app);
                zos.write(markdown.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build export zip", ex);
        }
        return baos.toByteArray();
    }

    private String uniqueEntryName(JobApplication app, Set<String> usedNames) {
        String base = markdownExportService.buildFilename(app);
        if (usedNames.add(base)) {
            return base;
        }
        String withId = base.replace(".md", "-" + app.getId() + ".md");
        usedNames.add(withId);
        return withId;
    }
}
```

- [ ] **Step 4: Run service test — expect PASS**

```powershell
.\gradlew.bat test --tests "com.offerflow.BulkExportServiceTest" --no-daemon
```

- [ ] **Step 5: Add controller endpoint**

In `ApplicationController.java`:

1. Add field + constructor param `BulkExportService bulkExportService`
2. Add method **before** `@GetMapping("/{id}")`:

```java
    @GetMapping("/export-all")
    public Object exportAll(RedirectAttributes redirectAttributes) {
        if (bulkExportService.countApplications() == 0) {
            redirectAttributes.addFlashAttribute(FlashMessages.ERROR, "暂无投递可导出。");
            return "redirect:/applications";
        }
        if (bulkExportService.exceedsBulkLimit()) {
            redirectAttributes.addFlashAttribute(
                    FlashMessages.ERROR,
                    "投递超过 " + com.offerflow.support.ExportLimits.MAX_BULK_EXPORT + " 条，请分批导出。");
            return "redirect:/applications";
        }
        byte[] zip = bulkExportService.exportAllAsZip();
        String filename = "offerflow-export-" + java.time.LocalDate.now() + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }
```

Add imports: `BulkExportService`, ensure `ResponseEntity` already imported.

- [ ] **Step 6: Add list UI button**

In `applications/list.html`, after `<h1>投递列表</h1>`:

```html
    <div class="actions" style="margin-bottom:12px;">
        <a class="btn btn-sm" th:href="@{/applications/export-all}">导出全部 Markdown (zip)</a>
    </div>
```

- [ ] **Step 7: Add web test to ApplicationWebTest**

```java
    @Test
    void exportAllReturnsZipWhenApplicationsExist() throws Exception {
        mockMvc.perform(post("/applications")
                        .param("companyName", "ZipCo")
                        .param("positionTitle", "Dev")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/applications/export-all"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".zip")));
    }
```

- [ ] **Step 8: Run tests**

```powershell
.\gradlew.bat test --tests "com.offerflow.BulkExportServiceTest" --tests "com.offerflow.ApplicationWebTest.exportAllReturnsZipWhenApplicationsExist" --no-daemon
```

- [ ] **Step 9: Commit**

```powershell
git add src/main/java/com/offerflow/support/ExportLimits.java `
        src/main/java/com/offerflow/service/BulkExportService.java `
        src/main/java/com/offerflow/controller/ApplicationController.java `
        src/main/resources/templates/applications/list.html `
        src/test/java/com/offerflow/BulkExportServiceTest.java `
        src/test/java/com/offerflow/ApplicationWebTest.java
git commit -m "feat(application): add bulk markdown zip export"
```

---

### Task 28: Company seed preview

**Files:**
- Create: `src/main/java/com/offerflow/dto/SeedPreviewEntry.java`
- Create: `src/main/java/com/offerflow/dto/SeedPreviewView.java`
- Modify: `src/main/java/com/offerflow/service/CompanySeedService.java`
- Modify: `src/main/java/com/offerflow/controller/CompanyController.java`
- Modify: `src/main/resources/templates/companies/list.html`
- Modify: `src/test/java/com/offerflow/CompanySeedServiceTest.java`
- Modify: `src/test/java/com/offerflow/CompanyWebTest.java`

- [ ] **Step 1: Write failing test in CompanySeedServiceTest**

Add DTOs:

`SeedPreviewEntry.java`:
```java
package com.offerflow.dto;

public record SeedPreviewEntry(String name, String industry) {}
```

`SeedPreviewView.java`:
```java
package com.offerflow.dto;

import java.util.List;

public record SeedPreviewView(String packId, String title, int totalCount, List<SeedPreviewEntry> samples) {}
```

Add test:

```java
    @Test
    void previewSeedReturnsFirstFiveSamples() {
        SeedPreviewView preview = companySeedService.previewSeed(CompanySeedService.FINANCE_TECH);

        assertEquals(CompanySeedService.FINANCE_TECH, preview.packId());
        assertEquals(10, preview.totalCount());
        assertEquals(5, preview.samples().size());
        assertTrue(preview.samples().stream().anyMatch(e -> e.name().contains("蚂蚁")));
    }
```

- [ ] **Step 2: Run test — expect FAIL**

```powershell
.\gradlew.bat test --tests "com.offerflow.CompanySeedServiceTest.previewSeedReturnsFirstFiveSamples" --no-daemon
```

- [ ] **Step 3: Implement previewSeed in CompanySeedService**

```java
    private static final int PREVIEW_SAMPLE_SIZE = 5;

    @Transactional(readOnly = true)
    public SeedPreviewView previewSeed(String seedId) {
        List<CompanySeedEntry> entries = loadEntries(resolveSeedPath(seedId));
        String title = SEED_TITLES.getOrDefault(seedId, seedId);
        List<SeedPreviewEntry> samples = entries.stream()
                .limit(PREVIEW_SAMPLE_SIZE)
                .map(e -> new SeedPreviewEntry(e.name(), e.industry()))
                .toList();
        return new SeedPreviewView(seedId, title, entries.size(), samples);
    }
```

Add imports for `SeedPreviewEntry`, `SeedPreviewView`.

- [ ] **Step 4: Update CompanyController.list**

Add param `@RequestParam(required = false) String previewSeed` and:

```java
        if (previewSeed != null && !previewSeed.isBlank()) {
            try {
                model.addAttribute("seedPreview", companySeedService.previewSeed(previewSeed));
                model.addAttribute("previewSeedId", previewSeed);
            } catch (UnknownCompanySeedException ex) {
                model.addAttribute("previewSeedError", "seed 包不存在：" + previewSeed);
            }
        }
```

- [ ] **Step 5: Update companies/list.html**

After existing seed import POST form, add GET preview form:

```html
        <form class="actions" method="get" th:action="@{/companies}" style="margin-top:8px;">
            <input type="hidden" name="q" th:if="${searchQuery != null and !searchQuery.isBlank()}" th:value="${searchQuery}"/>
            <input type="hidden" name="industry" th:if="${selectedIndustry != null and !selectedIndustry.isBlank()}" th:value="${selectedIndustry}"/>
            <select name="previewSeed" style="padding:8px 10px;border:1px solid var(--border);border-radius:8px;font:inherit;">
                <option th:each="pack : ${seedPacks}"
                        th:value="${pack.id()}"
                        th:selected="${pack.id() == previewSeedId}"
                        th:text="${pack.title() + ' (' + pack.entryCount() + ')'}">seed</option>
            </select>
            <button class="btn btn-sm" type="submit">预览 seed</button>
        </form>
```

Preview panel below forms:

```html
    <div class="card" th:if="${seedPreview != null}" style="margin-bottom:16px;border-left:4px solid var(--primary, #2563eb);">
        <h2 style="margin:0 0 8px;font-size:1rem;" th:text="${seedPreview.title() + '（共 ' + seedPreview.totalCount() + ' 家，预览前 ' + seedPreview.samples().size() + ' 家）'}">预览</h2>
        <ul>
            <li th:each="entry : ${seedPreview.samples()}"
                th:text="${entry.name() + (entry.industry() != null ? ' · ' + entry.industry() : '')}">公司</li>
        </ul>
        <a class="btn btn-sm" th:href="@{/companies(q=${searchQuery}, industry=${selectedIndustry})}">关闭预览</a>
    </div>
    <p th:if="${previewSeedError != null}" class="muted" th:text="${previewSeedError}"></p>
```

- [ ] **Step 6: Web test in CompanyWebTest**

```java
    @Test
    void listShowsSeedPreview() throws Exception {
        mockMvc.perform(get("/companies").param("previewSeed", "finance-tech"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("蚂蚁集团")));
    }
```

- [ ] **Step 7: Run tests and commit**

```powershell
.\gradlew.bat test --tests "com.offerflow.CompanySeedServiceTest" --tests "com.offerflow.CompanyWebTest" --no-daemon
git add src/main/java/com/offerflow/dto/SeedPreviewEntry.java `
        src/main/java/com/offerflow/dto/SeedPreviewView.java `
        src/main/java/com/offerflow/service/CompanySeedService.java `
        src/main/java/com/offerflow/controller/CompanyController.java `
        src/main/resources/templates/companies/list.html `
        src/test/java/com/offerflow/CompanySeedServiceTest.java `
        src/test/java/com/offerflow/CompanyWebTest.java
git commit -m "feat(company): add seed pack preview on list page"
```

---

### Task 29: Interview template preview

**Files:**
- Create: `src/main/java/com/offerflow/dto/TemplatePreviewView.java`
- Modify: `src/main/java/com/offerflow/service/InterviewTemplateService.java`
- Modify: `src/main/java/com/offerflow/controller/ApplicationController.java`
- Create: `src/main/resources/templates/applications/template-preview.html`
- Modify: `src/main/resources/templates/applications/detail.html`
- Modify: `src/test/java/com/offerflow/InterviewTemplateServiceTest.java`
- Modify: `src/test/java/com/offerflow/ApplicationWebTest.java`

- [ ] **Step 1: Write failing test**

Create `TemplatePreviewView.java`:

```java
package com.offerflow.dto;

public record TemplatePreviewView(
        String templateId,
        String title,
        String prepExcerpt,
        String debriefRoundLabel,
        String debriefSummary) {}
```

Add to `InterviewTemplateServiceTest.java`:

```java
    @Test
    void previewTemplateReturnsExcerpt() {
        TemplatePreviewView preview = templateService.previewTemplate(InterviewTemplateService.JAVA_BACKEND);

        assertEquals("Java 后端", preview.title());
        assertTrue(preview.prepExcerpt().contains("JVM"));
        assertTrue(preview.debriefSummary().contains("八股"));
    }
```

- [ ] **Step 2: Implement previewTemplate in InterviewTemplateService**

```java
    private static final int PREP_PREVIEW_LINES = 8;

    @Transactional(readOnly = true)
    public TemplatePreviewView previewTemplate(String templateId) {
        InterviewTemplatePack pack = requirePack(templateId);
        return new TemplatePreviewView(
                pack.id(),
                pack.title(),
                excerptPrep(pack.prepChecklist()),
                pack.debrief().roundLabel(),
                summarizeDebrief(pack.debrief()));
    }

    private static String excerptPrep(String checklist) {
        if (checklist == null || checklist.isBlank()) {
            return "";
        }
        String[] lines = checklist.split("\\R");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            sb.append(line).append('\n');
            count++;
            if (count >= PREP_PREVIEW_LINES) {
                break;
            }
        }
        if (lines.length > PREP_PREVIEW_LINES) {
            sb.append("…");
        }
        return sb.toString().trim();
    }

    private static String summarizeDebrief(DebriefTemplate debrief) {
        StringBuilder sb = new StringBuilder();
        if (debrief.roundLabel() != null) {
            sb.append("轮次：").append(debrief.roundLabel()).append('\n');
        }
        appendFirstLine(sb, "问题框架", debrief.questionsAsked());
        appendFirstLine(sb, "自评框架", debrief.selfAssessment());
        appendFirstLine(sb, "改进框架", debrief.improvements());
        return sb.toString().trim();
    }

    private static void appendFirstLine(StringBuilder sb, String label, String block) {
        if (block == null || block.isBlank()) {
            return;
        }
        String first = block.lines().filter(l -> !l.isBlank()).findFirst().orElse("");
        sb.append(label).append("：").append(first).append('\n');
    }
```

- [ ] **Step 3: Add controller route**

In `ApplicationController.java`:

```java
    @GetMapping("/{id}/preview-template")
    public String previewTemplate(
            @PathVariable Long id,
            @RequestParam(defaultValue = InterviewTemplateService.JAVA_BACKEND) String template,
            Model model) {
        JobApplication application = applicationService.requireApplication(id);
        try {
            model.addAttribute("jobApplication", application);
            model.addAttribute("templatePreview", interviewTemplateService.previewTemplate(template));
            model.addAttribute("selectedTemplateId", template);
            model.addAttribute("templatePacks", interviewTemplateService.listAvailableTemplates());
        } catch (UnknownInterviewTemplateException ex) {
            throw new com.offerflow.service.ApplicationNotFoundException(id);
        }
        return "applications/template-preview";
    }
```

Prefer redirect with flash on unknown template — adjust to:

```java
        } catch (UnknownInterviewTemplateException ex) {
            return "redirect:/applications/" + id;
        }
```

And use flash — or throw 404. Spec says invalid → 404 or flash. Use redirect without flash for v1 simplicity.

- [ ] **Step 4: Create template-preview.html**

Minimal page with back link, prep `<pre>`, debrief summary, template selector links for other packs.

- [ ] **Step 5: Add preview links on detail.html**

In准备模板 card, after fill button, add:

```html
        <div class="actions" style="margin-top:8px;">
            <a class="btn btn-sm" th:each="pack : ${templatePacks}"
               th:href="@{/applications/{id}/preview-template(id=${jobApplication.id}, template=${pack.id()})}"
               th:text="'预览 ' + ${pack.title()}">预览</a>
        </div>
```

- [ ] **Step 6: Web test**

```java
    @Test
    void previewTemplatePageShowsPrepExcerpt() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "预览公司")
                        .param("positionTitle", "Java")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(get("/applications/" + appId + "/preview-template")
                        .param("template", InterviewTemplateService.JAVA_BACKEND))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JVM")));
    }
```

- [ ] **Step 7: Run tests and commit**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewTemplateServiceTest" --tests "com.offerflow.ApplicationWebTest.previewTemplatePageShowsPrepExcerpt" --no-daemon
git add src/main/java/com/offerflow/dto/TemplatePreviewView.java `
        src/main/java/com/offerflow/service/InterviewTemplateService.java `
        src/main/java/com/offerflow/controller/ApplicationController.java `
        src/main/resources/templates/applications/template-preview.html `
        src/main/resources/templates/applications/detail.html `
        src/test/java/com/offerflow/InterviewTemplateServiceTest.java `
        src/test/java/com/offerflow/ApplicationWebTest.java
git commit -m "feat(interview): add template preview page on application detail"
```

---

### Task 30: README and documentation

**Files:**
- Modify: `README.md`
- Modify: `JOURNAL.md` (optional one-liner under Day 3 or 后续产品方向)

- [ ] **Step 1: Update README.md**

Add to功能矩阵 / 功能一览:

- A2: 复盘全文搜索（`/interviews/search`）
- A1: 批量 Markdown zip（`/applications/export-all`）
- A4: seed / 模板预览

Add routes to页面与 API 速查:

| GET | `/interviews/search` | 复盘搜索 `?q=` |
| GET | `/applications/export-all` | 下载全部 Markdown zip |
| GET | `/companies?previewSeed=` | seed 预览 |
| GET | `/applications/{id}/preview-template?template=` | 模板预览 |

Add Phase 6 row to演进历史.

Remove from后续规划 (now done): 批量 Markdown 导出、seed/模板预览弹窗.

- [ ] **Step 2: Optional JOURNAL note**

Under Day 3 or new bullet: Phase 6 启动 — knowledge portability spec.

- [ ] **Step 3: Full test suite**

```powershell
.\gradlew.bat test --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```powershell
git add README.md JOURNAL.md
git commit -m "docs: document Phase 6 knowledge base and portability"
```

---

## Task Summary

| Task | Commit message |
|------|----------------|
| 26 | `feat(interview): add debrief full-text search page` |
| 27 | `feat(application): add bulk markdown zip export` |
| 28 | `feat(company): add seed pack preview on list page` |
| 29 | `feat(interview): add template preview page on application detail` |
| 30 | `docs: document Phase 6 knowledge base and portability` |

---

## Spec Coverage Self-Review

| Spec section | Task |
|--------------|------|
| §3 Debrief search independent page | 26 |
| §4 Bulk zip export | 27 |
| §5 C1 Seed preview | 28 |
| §5 C2 Template preview | 29 |
| §6 Nav + list UI | 26, 27 |
| §8 README | 30 |
| §9 Task outline 26–30 | All |

No TBD/TODO placeholders in steps.

---

## Done Definition

- [ ] `/interviews/search?q=` works with snippet + link
- [ ] `/applications/export-all` downloads valid zip
- [ ] Seed preview on `/companies?previewSeed=`
- [ ] Template preview on `/applications/{id}/preview-template`
- [ ] Nav shows 复盘搜索
- [ ] `.\gradlew.bat test` green
- [ ] README updated
