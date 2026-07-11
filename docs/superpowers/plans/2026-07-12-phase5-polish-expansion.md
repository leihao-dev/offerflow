# Phase 5 Polish + Expansion Packs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add application search, dashboard week-interview list, Markdown export, two company seed packs, two interview template packs, dropdown selectors, and manual company-name dossier hint.

**Architecture:** Extend existing seed/template registries (`Map<String,String>`). Add `MarkdownExportService` and `JobApplicationService.search`. Dashboard maps `InterviewNote` rows to `InterviewWeekItem`. UI uses `<select>` + action button pattern from Phase 4.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Thymeleaf, Spring Data JPA, Jackson, JUnit 5, MockMvc

**Spec:** `docs/superpowers/specs/2026-07-12-phase5-polish-expansion-design.md`

**Prerequisite:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cd C:\Users\Ray\offerflow
```

---

## File Map

| File | Responsibility |
|------|----------------|
| `dto/InterviewWeekItem.java` | Dashboard interview row |
| `dto/SeedPackInfo.java` | Seed dropdown entry (id, title, entryCount) |
| `dto/TemplatePackInfo.java` | Template dropdown entry (id, title) |
| `dto/DashboardView.java` | Add `interviewsThisWeek` field |
| `service/MarkdownExportService.java` | Build `.md` content |
| `service/DashboardService.java` | Map week notes to items |
| `service/JobApplicationService.java` | `search(q, stage)` |
| `service/CompanySeedService.java` | +2 seeds, `listAvailableSeeds()` |
| `service/InterviewTemplateService.java` | +2 templates, `listAvailableTemplates()` |
| `repository/JobApplicationRepository.java` | Search queries |
| `resources/seeds/finance-tech.json` | 10 finance companies |
| `resources/seeds/foreign-tech.json` | 10 foreign tech companies |
| `resources/seeds/frontend-react-interview.json` | Frontend template pack |
| `resources/seeds/go-backend-interview.json` | Go template pack |
| `controller/ApplicationController.java` | search, export, templatePacks on detail |
| `controller/CompanyController.java` | seedPacks on list |
| `templates/dashboard.html` | Week interviews table |
| `templates/applications/list.html` | Search form |
| `templates/applications/detail.html` | Export btn, template selector |
| `templates/companies/list.html` | Seed selector |

---

### Task 17: Application search on list page

**Files:**
- Modify: `src/main/java/com/offerflow/repository/JobApplicationRepository.java`
- Modify: `src/main/java/com/offerflow/service/JobApplicationService.java`
- Modify: `src/main/java/com/offerflow/controller/ApplicationController.java`
- Modify: `src/main/resources/templates/applications/list.html`
- Modify: `src/test/java/com/offerflow/JobApplicationServiceTest.java`
- Modify: `src/test/java/com/offerflow/ApplicationWebTest.java`

- [ ] **Step 1: Write failing service test**

Add to `JobApplicationServiceTest.java`:

```java
    @Test
    void searchByCompanyNamePartialMatch() {
        ApplicationForm meituan = sampleForm("美团", "Java 后端");
        applicationService.create(meituan);
        applicationService.create(sampleForm("腾讯", "Go 后端"));

        var results = applicationService.search(
                java.util.Optional.of("美团"), java.util.Optional.empty());

        assertEquals(1, results.size());
        assertEquals("美团", results.get(0).getCompanyName());
    }

    @Test
    void searchByPositionTitle() {
        ApplicationForm form = sampleForm("某公司", "高级 Java 工程师");
        applicationService.create(form);

        var results = applicationService.search(
                java.util.Optional.of("Java"), java.util.Optional.empty());

        assertEquals(1, results.size());
    }

    private ApplicationForm sampleForm(String company, String position) {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName(company);
        form.setPositionTitle(position);
        form.setStage(ApplicationStage.APPLIED);
        form.setAppliedAt(LocalDate.now());
        return form;
    }
```

- [ ] **Step 2: Run test — expect FAIL**

```powershell
.\gradlew.bat test --tests "com.offerflow.JobApplicationServiceTest.searchByCompanyNamePartialMatch" --no-daemon
```

- [ ] **Step 3: Add repository methods**

In `JobApplicationRepository.java`:

```java
    List<JobApplication> findByCompanyNameContainingIgnoreCaseOrPositionTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
            String companyName, String positionTitle);

    @Query("""
            SELECT a FROM JobApplication a
            WHERE a.stage = :stage
              AND (LOWER(a.companyName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.positionTitle) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY a.updatedAt DESC""")
    List<JobApplication> searchByStageAndQuery(
            @Param("stage") ApplicationStage stage, @Param("q") String q);
```

- [ ] **Step 4: Add `search` to JobApplicationService**

```java
    @Transactional(readOnly = true)
    public List<JobApplication> search(Optional<String> query, Optional<ApplicationStage> stage) {
        String q = query.map(String::trim).filter(value -> !value.isBlank()).orElse(null);
        if (q == null) {
            return findAll(stage);
        }
        return stage.map(s -> repository.searchByStageAndQuery(s, q))
                .orElseGet(() -> repository
                        .findByCompanyNameContainingIgnoreCaseOrPositionTitleContainingIgnoreCaseOrderByUpdatedAtDesc(
                                q, q));
    }
```

Change `findAll` to delegate: `return search(Optional.empty(), stage);` if not already — currently `findAll` is separate; keep both, `list` controller calls `search`.

- [ ] **Step 5: Update ApplicationController.list**

```java
    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ApplicationStage stage,
            Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute(
                "applications",
                applicationService.search(Optional.ofNullable(q), Optional.ofNullable(stage)));
        model.addAttribute("searchQuery", q);
        model.addAttribute("selectedStage", stage);
        // ... rest unchanged
```

- [ ] **Step 6: Update applications/list.html**

Add search card before stage filters (mirror companies/list pattern):

```html
    <div class="card" style="margin-bottom:16px;">
        <form class="actions" method="get" th:action="@{/applications}">
            <input type="hidden" name="stage" th:if="${selectedStage != null}" th:value="${selectedStage}"/>
            <input name="q" type="search" placeholder="按公司或岗位搜索…" th:value="${searchQuery}"
                   style="flex:1;min-width:180px;padding:8px 10px;border:1px solid var(--border);border-radius:8px;font:inherit;"/>
            <button class="btn btn-primary btn-sm" type="submit">搜索</button>
            <a class="btn btn-sm" th:if="${searchQuery != null and !searchQuery.isBlank()}"
               th:href="@{/applications(stage=${selectedStage})}">清除搜索</a>
        </form>
    </div>
```

Update stage links to preserve q: `th:href="@{/applications(stage=${s}, q=${searchQuery})}"`

Add empty search state:

```html
        <p th:if="${applications.isEmpty() and searchQuery != null and !searchQuery.isBlank()}" class="muted">
            没有匹配「<span th:text="${searchQuery}"></span>」的投递。
        </p>
```

- [ ] **Step 7: Add web test to ApplicationWebTest**

```java
    @Test
    void listSearchByCompanyName() throws Exception {
        mockMvc.perform(post("/applications")
                        .param("companyName", "美团")
                        .param("positionTitle", "Java")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/applications").param("q", "美团"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("美团")))
                .andExpect(content().string(containsString("type=\"search\"")));
    }
```

- [ ] **Step 8: Run tests**

```powershell
.\gradlew.bat test --no-daemon
```

- [ ] **Step 9: Commit**

```powershell
git commit -am "feat(application): add application search on list page"
```

---

### Task 18: Dashboard this-week interviews list

**Files:**
- Create: `src/main/java/com/offerflow/dto/InterviewWeekItem.java`
- Modify: `src/main/java/com/offerflow/dto/DashboardView.java`
- Modify: `src/main/java/com/offerflow/service/DashboardService.java`
- Modify: `src/main/resources/templates/dashboard.html`
- Modify: `src/test/java/com/offerflow/DashboardServiceTest.java`

- [ ] **Step 1: Write failing test**

Add to `DashboardServiceTest.java`:

```java
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.service.InterviewNoteService;

    @Autowired
    private InterviewNoteService interviewNoteService;

    @Test
    void listsInterviewsThisWeek() {
        var app = applicationService.create(sampleApp());
        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(app.getId());
        note.setInterviewDate(LocalDate.now());
        note.setRoundLabel("一面");
        interviewNoteService.create(note);

        var view = dashboardService.build(LocalDate.now());

        assertEquals(1, view.interviewsThisWeek().size());
        assertEquals("一面", view.interviewsThisWeek().get(0).roundLabel());
        assertEquals(app.getId(), view.interviewsThisWeek().get(0).applicationId());
    }

    private ApplicationForm sampleApp() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("面试公司");
        form.setPositionTitle("工程师");
        form.setStage(ApplicationStage.TECH_INTERVIEW);
        form.setAppliedAt(LocalDate.now());
        return form;
    }
```

- [ ] **Step 2: Create InterviewWeekItem**

```java
package com.offerflow.dto;

import java.time.LocalDate;

public record InterviewWeekItem(
        Long noteId,
        Long applicationId,
        LocalDate interviewDate,
        String roundLabel,
        String companyName,
        String positionTitle) {}
```

- [ ] **Step 3: Extend DashboardView**

```java
public record DashboardView(
        long activeCount,
        long interviewsThisWeek,
        long overdueCount,
        List<JobApplication> overdueApplications,
        List<JobApplication> recentApplications,
        List<InterviewWeekItem> interviewsThisWeek) {}
```

- [ ] **Step 4: Update DashboardService.build**

```java
        List<InterviewNote> weekNotes =
                interviewNoteRepository.findByInterviewDateBetween(weekStart, weekEnd);
        List<InterviewWeekItem> weekItems = weekNotes.stream()
                .sorted(Comparator.comparing(InterviewNote::getInterviewDate)
                        .thenComparing(n -> n.getApplication().getCompanyName()))
                .map(n -> new InterviewWeekItem(
                        n.getId(),
                        n.getApplication().getId(),
                        n.getInterviewDate(),
                        n.getRoundLabel(),
                        n.getApplication().getCompanyName(),
                        n.getApplication().getPositionTitle()))
                .toList();

        return new DashboardView(
                applicationService.countActive(),
                weekNotes.size(),
                overdue.size(),
                overdue,
                applicationRepository.findTop5ByOrderByUpdatedAtDesc(),
                weekItems);
```

- [ ] **Step 5: Update dashboard.html**

Insert after `grid-3` stats, before overdue card:

```html
    <div class="card" th:unless="${dashboard.interviewsThisWeek().isEmpty()}">
        <h2>本周面试</h2>
        <table>
            <thead>
            <tr><th>日期</th><th>公司</th><th>岗位</th><th>轮次</th><th></th></tr>
            </thead>
            <tbody>
            <tr th:each="item : ${dashboard.interviewsThisWeek()}">
                <td th:text="${item.interviewDate()}">日期</td>
                <td th:text="${item.companyName()}">公司</td>
                <td th:text="${item.positionTitle()}">岗位</td>
                <td th:text="${item.roundLabel() != null ? item.roundLabel() : '-'}">轮次</td>
                <td><a class="btn btn-sm" th:href="@{/applications/{id}(id=${item.applicationId()})}">查看</a></td>
            </tr>
            </tbody>
        </table>
    </div>
```

- [ ] **Step 6: Run tests and commit**

```powershell
.\gradlew.bat test --no-daemon
git add src/main/java/com/offerflow/dto/InterviewWeekItem.java `
        src/main/java/com/offerflow/dto/DashboardView.java `
        src/main/java/com/offerflow/service/DashboardService.java `
        src/main/resources/templates/dashboard.html `
        src/test/java/com/offerflow/DashboardServiceTest.java
git commit -m "feat(dashboard): add this week interviews list"
```

---

### Task 19: Markdown export

**Files:**
- Create: `src/main/java/com/offerflow/service/MarkdownExportService.java`
- Modify: `src/main/java/com/offerflow/controller/ApplicationController.java`
- Modify: `src/main/resources/templates/applications/detail.html`
- Create: `src/test/java/com/offerflow/MarkdownExportServiceTest.java`
- Modify: `src/test/java/com/offerflow/ApplicationWebTest.java`

- [ ] **Step 1: Write failing test**

Create `MarkdownExportServiceTest.java`:

```java
package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.InterviewNoteService;
import com.offerflow.service.JobApplicationService;
import com.offerflow.service.MarkdownExportService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MarkdownExportServiceTest {

    @Autowired
    private MarkdownExportService exportService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private InterviewNoteService interviewNoteService;

    @Test
    void exportContainsApplicationAndInterviewSections() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("美团");
        form.setPositionTitle("Java 后端");
        form.setStage(ApplicationStage.TECH_INTERVIEW);
        form.setAppliedAt(LocalDate.of(2026, 7, 1));
        form.setPrepChecklist("□ JVM");
        var app = applicationService.create(form);

        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(app.getId());
        note.setInterviewDate(LocalDate.of(2026, 7, 10));
        note.setQuestionsAsked("HashMap 原理");
        interviewNoteService.create(note);

        String md = exportService.export(applicationService.requireApplication(app.getId()));

        assertTrue(md.contains("# 美团"));
        assertTrue(md.contains("Java 后端"));
        assertTrue(md.contains("准备清单"));
        assertTrue(md.contains("HashMap 原理"));
        assertTrue(md.contains("面试复盘"));
    }
}
```

- [ ] **Step 2: Implement MarkdownExportService**

```java
package com.offerflow.service;

import com.offerflow.model.Company;
import com.offerflow.model.InterviewNote;
import com.offerflow.model.JobApplication;
import com.offerflow.web.StageLabels;
import org.springframework.stereotype.Service;

@Service
public class MarkdownExportService {

    public String export(JobApplication app) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(app.getCompanyName()).append(" — ").append(app.getPositionTitle()).append("\n\n");
        sb.append("- 阶段：").append(StageLabels.label(app.getStage())).append("\n");
        sb.append("- 投递日：").append(app.getAppliedAt()).append("\n");
        sb.append("- 下次跟进：")
                .append(app.getNextFollowUpAt() != null ? app.getNextFollowUpAt() : "未设置")
                .append("\n");
        if (app.getSource() != null) {
            sb.append("- 渠道：").append(app.getSource()).append("\n");
        }
        sb.append("\n");

        Company company = app.getCompany();
        if (company != null) {
            sb.append("## 公司档案\n\n");
            sb.append("- 公司：").append(company.getName()).append("\n");
            if (company.getCareersUrl() != null) {
                sb.append("- 招聘页：").append(company.getCareersUrl()).append("\n");
            }
            if (company.getReferralCode() != null) {
                sb.append("- 内推码：").append(company.getReferralCode()).append("\n");
            }
            sb.append("\n");
        }

        appendSection(sb, "JD", app.getJdContent());
        appendSection(sb, "本岗位调研笔记", app.getCompanyNotes());
        appendSection(sb, "准备清单", app.getPrepChecklist());

        sb.append("## 面试复盘\n\n");
        if (app.getInterviewNotes().isEmpty()) {
            sb.append("（暂无）\n");
        } else {
            for (InterviewNote note : app.getInterviewNotes()) {
                sb.append("### ")
                        .append(note.getInterviewDate())
                        .append(note.getRoundLabel() != null ? " · " + note.getRoundLabel() : "")
                        .append("\n\n");
                if (note.getQuestionsAsked() != null) {
                    sb.append("**问题：**\n").append(note.getQuestionsAsked()).append("\n\n");
                }
                if (note.getSelfAssessment() != null) {
                    sb.append("**自评：**\n").append(note.getSelfAssessment()).append("\n\n");
                }
                if (note.getImprovements() != null) {
                    sb.append("**改进：**\n").append(note.getImprovements()).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    public String buildFilename(JobApplication app) {
        String company = sanitize(app.getCompanyName());
        String position = sanitize(app.getPositionTitle());
        if (company.isBlank() && position.isBlank()) {
            return "offerflow-export.md";
        }
        return "offerflow-" + company + "-" + position + ".md";
    }

    private static void appendSection(StringBuilder sb, String title, String content) {
        if (content != null && !content.isBlank()) {
            sb.append("## ").append(title).append("\n\n").append(content).append("\n\n");
        }
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
    }
}
```

- [ ] **Step 3: Add export endpoint to ApplicationController**

```java
import com.offerflow.service.MarkdownExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

    private final MarkdownExportService markdownExportService;

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportMarkdown(@PathVariable Long id) {
        JobApplication application = applicationService.requireApplication(id);
        String markdown = markdownExportService.export(application);
        String filename = markdownExportService.buildFilename(application);
        byte[] bytes = markdown.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "markdown", java.nio.charset.StandardCharsets.UTF_8))
                .body(bytes);
    }
```

- [ ] **Step 4: Add button on detail.html**

In基本信息 card actions:

```html
            <a class="btn" th:href="@{/applications/{id}/export(id=${jobApplication.id})}">导出 Markdown</a>
```

- [ ] **Step 5: Web test**

```java
    @Test
    void exportMarkdownReturnsAttachment() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "美团")
                        .param("positionTitle", "Java")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        mockMvc.perform(get(redirectUrl + "/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("# 美团")));
    }
```

Add import: `import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;`

- [ ] **Step 6: Run tests and commit**

```powershell
.\gradlew.bat test --no-daemon
git add src/main/java/com/offerflow/service/MarkdownExportService.java `
        src/main/java/com/offerflow/controller/ApplicationController.java `
        src/main/resources/templates/applications/detail.html `
        src/test/java/com/offerflow/MarkdownExportServiceTest.java `
        src/test/java/com/offerflow/ApplicationWebTest.java
git commit -m "feat(application): add markdown export for application detail"
```

---

### Task 20: Finance and foreign tech seed packs

**Files:**
- Create: `src/main/resources/seeds/finance-tech.json`
- Create: `src/main/resources/seeds/foreign-tech.json`
- Create: `src/main/java/com/offerflow/dto/SeedPackInfo.java`
- Modify: `src/main/java/com/offerflow/service/CompanySeedService.java`
- Modify: `src/test/java/com/offerflow/CompanySeedServiceTest.java`

- [ ] **Step 1: Create finance-tech.json** (10 entries, industry `"金融科技"`)

```json
[
  {
    "name": "蚂蚁集团",
    "industry": "金融科技",
    "websiteUrl": "https://www.antgroup.com",
    "careersUrl": "https://talent.antgroup.com",
    "referralNotes": "官网社招/校招；内推信息请在导入后自行填写"
  },
  {
    "name": "微众银行",
    "industry": "金融科技",
    "websiteUrl": "https://www.webank.com",
    "careersUrl": "https://hr.webank.com",
    "referralNotes": "微众银行招聘官网"
  }
]
```

(Fill remaining 8: 京东科技, 平安科技, 东方财富, 同花顺, 恒生电子, 富途控股, 雪球, 陆金所 — verify URLs at implementation.)

- [ ] **Step 2: Create foreign-tech.json** (10 entries, industry `"外企科技"`)

Companies: Microsoft, Google, Amazon, Apple, Meta, NVIDIA, SAP, Intel, Shopee, LinkedIn — use official careers URLs where known.

- [ ] **Step 3: Create SeedPackInfo**

```java
package com.offerflow.dto;

public record SeedPackInfo(String id, String title, int entryCount) {}
```

- [ ] **Step 4: Extend CompanySeedService**

Replace `Map.of` single entry with:

```java
    public static final String FINANCE_TECH = "finance-tech";
    public static final String FOREIGN_TECH = "foreign-tech";

    private static final Map<String, String> SEED_RESOURCES = Map.of(
            JAVA_BACKEND_INTERNET, "seeds/java-backend-internet.json",
            FINANCE_TECH, "seeds/finance-tech.json",
            FOREIGN_TECH, "seeds/foreign-tech.json");

    private static final Map<String, String> SEED_TITLES = Map.of(
            JAVA_BACKEND_INTERNET, "Java 后端 · 互联网",
            FINANCE_TECH, "金融 / 金融科技",
            FOREIGN_TECH, "外企科技");

    @Transactional(readOnly = true)
    public List<SeedPackInfo> listAvailableSeeds() {
        return SEED_RESOURCES.keySet().stream()
                .sorted()
                .map(id -> new SeedPackInfo(id, SEED_TITLES.get(id), countSeedEntries(id)))
                .toList();
    }
```

- [ ] **Step 5: Add tests**

```java
    @Test
    void financeTechSeedContainsTenCompanies() {
        assertEquals(10, seedService.countSeedEntries(CompanySeedService.FINANCE_TECH));
    }

    @Test
    void foreignTechSeedImportsIdempotently() {
        SeedImportResult first = seedService.importSeed(CompanySeedService.FOREIGN_TECH);
        assertEquals(10, first.imported());
        SeedImportResult second = seedService.importSeed(CompanySeedService.FOREIGN_TECH);
        assertEquals(0, second.imported());
        assertEquals(10, second.skipped());
    }
```

- [ ] **Step 6: Run tests and commit**

```powershell
.\gradlew.bat test --tests "com.offerflow.CompanySeedServiceTest" --no-daemon
git add src/main/resources/seeds/finance-tech.json `
        src/main/resources/seeds/foreign-tech.json `
        src/main/java/com/offerflow/dto/SeedPackInfo.java `
        src/main/java/com/offerflow/service/CompanySeedService.java `
        src/test/java/com/offerflow/CompanySeedServiceTest.java
git commit -m "feat(company): add finance and foreign tech seed packs"
```

---

### Task 21: Seed pack selector on company list

**Files:**
- Modify: `src/main/java/com/offerflow/controller/CompanyController.java`
- Modify: `src/main/resources/templates/companies/list.html`
- Modify: `src/test/java/com/offerflow/CompanyWebTest.java`

- [ ] **Step 1: Pass seedPacks in CompanyController.list**

```java
        model.addAttribute("seedPacks", companySeedService.listAvailableSeeds());
```

Inject `CompanySeedService` if not already present.

- [ ] **Step 2: Replace import card in list.html**

```html
    <div class="card" style="margin-bottom:16px;">
        <h2 style="margin:0 0 6px;font-size:1rem;">导入公司 seed</h2>
        <p class="muted" style="margin:0 0 12px;">已存在的公司将跳过，不覆盖已有档案。</p>
        <form class="actions" method="post" th:action="@{/companies/import-seed}">
            <select name="seed" style="padding:8px 10px;border:1px solid var(--border);border-radius:8px;font:inherit;">
                <option th:each="pack : ${seedPacks}"
                        th:value="${pack.id()}"
                        th:text="${pack.title() + ' (' + pack.entryCount() + ')'}">seed</option>
            </select>
            <button class="btn btn-primary btn-sm" type="submit">导入 seed</button>
        </form>
    </div>
```

- [ ] **Step 3: Web test**

```java
    @Test
    void listShowsSeedSelector() throws Exception {
        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("导入公司 seed")))
                .andExpect(content().string(containsString("金融 / 金融科技")));
    }

    @Test
    void importFinanceTechSeed() throws Exception {
        mockMvc.perform(post("/companies/import-seed").param("seed", CompanySeedService.FINANCE_TECH))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(FlashMessages.SUCCESS, containsString("导入")));
    }
```

- [ ] **Step 4: Run tests and commit**

```powershell
.\gradlew.bat test --no-daemon
git commit -am "feat(company): add seed pack selector on company list"
```

---

### Task 22: Frontend and Go interview template packs

**Files:**
- Create: `src/main/resources/seeds/frontend-react-interview.json`
- Create: `src/main/resources/seeds/go-backend-interview.json`
- Create: `src/main/java/com/offerflow/dto/TemplatePackInfo.java`
- Modify: `src/main/java/com/offerflow/service/InterviewTemplateService.java`
- Modify: `src/test/java/com/offerflow/InterviewTemplateServiceTest.java`

- [ ] **Step 1: Create frontend-react-interview.json**

```json
{
  "id": "frontend-react",
  "title": "前端 React",
  "prepChecklist": "□ React：生命周期、Hooks、受控/非受控组件\n□ 虚拟 DOM 与 Diff\n□ 状态管理：Context / Redux 思路\n□ 性能：memo、useMemo、懒加载\n□ 浏览器：事件循环、渲染流程\n□ CSS：Flex/Grid、BFC\n□ TypeScript：泛型、类型收窄\n□ 工程化：Vite/Webpack、ESLint\n□ 手写：防抖节流、深拷贝\n□ 项目：亮点 + 难点 + 指标",
  "debrief": {
    "roundLabel": "技术面",
    "questionsAsked": "## 基础 / React\n- \n\n## 手写 / 算法\n- \n\n## 项目 / 工程化\n- ",
    "selfAssessment": "## 答得好的\n- \n\n## 卡壳的\n- \n\n## 整体自评（1-5 分）\n",
    "improvements": "- 待补知识点：\n- 代码表达改进：\n- 下次练习："
  }
}
```

- [ ] **Step 2: Create go-backend-interview.json**

```json
{
  "id": "go-backend",
  "title": "Go 后端",
  "prepChecklist": "□ goroutine / channel / select\n□ GC 与内存逃逸\n□ interface 与反射\n□ 并发安全：mutex、atomic\n□ HTTP/gRPC 服务设计\n□ MySQL 索引与事务\n□ Redis 缓存策略\n□ 微服务：注册发现、限流\n□ 项目：亮点 + 难点 + QPS/延迟数据\n□ 算法：1 道中等题",
  "debrief": {
    "roundLabel": "技术面",
    "questionsAsked": "## Go 基础 / 并发\n- \n\n## 项目 / 系统设计\n- \n\n## 算法\n- ",
    "selfAssessment": "## 答得好的\n- \n\n## 卡壳的\n- \n\n## 整体自评（1-5 分）\n",
    "improvements": "- 待补知识点：\n- 表达方式改进：\n- 下次模拟："
  }
}
```

- [ ] **Step 3: Create TemplatePackInfo + extend service**

```java
public record TemplatePackInfo(String id, String title) {}

    public static final String FRONTEND_REACT = "frontend-react";
    public static final String GO_BACKEND = "go-backend";

    private static final Map<String, String> TEMPLATE_RESOURCES = Map.of(
            JAVA_BACKEND, "seeds/java-backend-interview.json",
            FRONTEND_REACT, "seeds/frontend-react-interview.json",
            GO_BACKEND, "seeds/go-backend-interview.json");

    @Transactional(readOnly = true)
    public List<TemplatePackInfo> listAvailableTemplates() {
        return TEMPLATE_RESOURCES.keySet().stream()
                .sorted()
                .map(id -> {
                    InterviewTemplatePack pack = requirePack(id);
                    return new TemplatePackInfo(id, pack.title());
                })
                .toList();
    }
```

- [ ] **Step 4: Add tests**

```java
    @Test
    void loadFrontendReactPack() {
        var pack = templateService.requirePack(InterviewTemplateService.FRONTEND_REACT);
        assertEquals("前端 React", pack.title());
        assertTrue(pack.prepChecklist().contains("Hooks"));
    }

    @Test
    void loadGoBackendPack() {
        var pack = templateService.requirePack(InterviewTemplateService.GO_BACKEND);
        assertTrue(pack.prepChecklist().contains("goroutine"));
    }
```

- [ ] **Step 5: Run tests and commit**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewTemplateServiceTest" --no-daemon
git add src/main/resources/seeds/frontend-react-interview.json `
        src/main/resources/seeds/go-backend-interview.json `
        src/main/java/com/offerflow/dto/TemplatePackInfo.java `
        src/main/java/com/offerflow/service/InterviewTemplateService.java `
        src/test/java/com/offerflow/InterviewTemplateServiceTest.java
git commit -m "feat(interview): add frontend and go interview template packs"
```

---

### Task 23: Interview template selector on detail page

**Files:**
- Modify: `src/main/java/com/offerflow/controller/ApplicationController.java`
- Modify: `src/main/resources/templates/applications/detail.html`
- Modify: `src/test/java/com/offerflow/ApplicationWebTest.java`

- [ ] **Step 1: Pass templatePacks in detail()**

```java
        model.addAttribute("templatePacks", interviewTemplateService.listAvailableTemplates());
```

- [ ] **Step 2: Update prep template card in detail.html**

Replace hardcoded Java card:

```html
    <div class="card">
        <h2>准备模板</h2>
        <p class="muted">常见考点清单；已有内容不会覆盖。</p>
        <form class="actions" method="post" th:action="@{/applications/{id}/apply-template(id=${jobApplication.id})}">
            <select name="template" style="padding:8px 10px;border:1px solid var(--border);border-radius:8px;font:inherit;">
                <option th:each="pack : ${templatePacks}"
                        th:value="${pack.id()}"
                        th:text="${pack.title()}">模板</option>
            </select>
            <button class="btn btn-primary btn-sm" type="submit">填充准备清单</button>
        </form>
    </div>
```

Update interview links:

```html
                <a class="btn btn-sm" th:each="pack : ${templatePacks}"
                   th:href="@{/applications/{id}/interviews/new(id=${jobApplication.id}, template=${pack.id()})}"
                   th:text="'+ 复盘（' + ${pack.title()} + '）'">模板</a>
```

- [ ] **Step 3: Web test**

```java
    @Test
    void detailShowsTemplateSelector() throws Exception {
        // create app, get detail
        // expect contains "准备模板" and "前端 React" and "Go 后端"
    }
```

- [ ] **Step 4: Run tests and commit**

```powershell
.\gradlew.bat test --no-daemon
git commit -am "feat(application): add interview template selector on detail page"
```

---

### Task 24: Manual company name dossier hint

**Files:**
- Modify: `src/main/java/com/offerflow/controller/ApplicationController.java`
- Modify: `src/test/java/com/offerflow/ApplicationWebTest.java`

- [ ] **Step 1: Add helper and call on create/update**

```java
    private void maybeAddCompanyDossierHint(ApplicationForm form, RedirectAttributes redirectAttributes) {
        if (form.getCompanyId() != null) {
            return;
        }
        String name = form.getCompanyName() != null ? form.getCompanyName().trim() : "";
        if (name.isEmpty()) {
            return;
        }
        if (companyService.existsByName(name)) {
            redirectAttributes.addFlashAttribute(
                    FlashMessages.SUCCESS,
                    "投递已保存。已存在公司档案「" + name + "」，下次可从下拉选择关联。");
        }
    }
```

Call after successful create/update (may append to existing success flash — v1: replace success message when hint applies, or use two flash keys; simplest: combine in one message).

- [ ] **Step 2: Web test**

```java
    @Test
    void createWithManualNameMatchingDossierShowsHint() throws Exception {
        mockMvc.perform(post("/companies")
                        .param("name", "美团")
                        .param("industry", "互联网"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/applications")
                        .param("companyName", "美团")
                        .param("positionTitle", "Java")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(FlashMessages.SUCCESS, containsString("已存在公司档案")));
    }
```

- [ ] **Step 3: Run tests and commit**

```powershell
.\gradlew.bat test --no-daemon
git commit -am "feat(application): suggest linking manual company name to dossier"
```

---

### Task 25: Documentation

**Files:**
- Modify: `README.md`
- Modify: `JOURNAL.md`

- [ ] **Step 1: Add README Phase 5 section**

Include: search, dashboard list, export, 3 seeds, 3 templates, selectors, company hint. Update「后续规划」.

- [ ] **Step 2: Update JOURNAL Day 2 bullets + assignment checklist**

Mark push complete; add Phase 5 note.

- [ ] **Step 3: Commit**

```powershell
git add README.md JOURNAL.md
git commit -m "docs: document Phase 5 polish and expansion packs"
```

---

## Spec Coverage Checklist

| Requirement | Task |
|-------------|------|
| Application search | 17 |
| Week interviews list | 18 |
| Markdown export | 19 |
| finance-tech + foreign-tech seeds | 20 |
| Seed selector UI | 21 |
| frontend-react + go-backend templates | 22 |
| Template selector UI | 23 |
| Company name hint | 24 |
| README + JOURNAL | 25 |

## Manual QA (after Task 25)

1. Dashboard → 本周面试 table links work
2. Applications → search「美团」+ stage filter combined
3. Detail → 导出 Markdown downloads `.md`
4. Companies → select finance-tech → import → 10 new companies
5. Detail → select Go template → fill prep → 新增复盘（Go 后端）
6. Create application with manual「美团」when dossier exists → hint flash
