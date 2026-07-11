# Phase 3 Interview Template Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship Java backend interview prep checklist + debrief form templates via classpath JSON, fill-on-click without overwriting user content.

**Architecture:** `InterviewTemplateService` loads `seeds/java-backend-interview.json` (mirrors `CompanySeedService`). `JobApplicationService.applyPrepChecklistIfEmpty` persists prep text. Controllers expose `POST /applications/{id}/apply-template` and `GET .../interviews/new?template=java-backend` to pre-fill `InterviewNoteForm`.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Thymeleaf, Jackson, JUnit 5, MockMvc

**Spec:** `docs/superpowers/specs/2026-07-12-interview-template-design.md`

**Prerequisite:** From repo root `C:\Users\Ray\offerflow`, set JDK before Gradle:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
```

---

## File Map

| File | Responsibility |
|------|----------------|
| `dto/DebriefTemplate.java` | JSON nested debrief sections (record) |
| `dto/InterviewTemplatePack.java` | Root seed object: id, title, prep, debrief |
| `dto/ApplyPrepResult.java` | `applied` flag for prep fill outcome |
| `service/UnknownInterviewTemplateException.java` | Unknown template id |
| `service/InterviewTemplateService.java` | Load pack, apply prep, enrich debrief form |
| `service/JobApplicationService.java` | Add `applyPrepChecklistIfEmpty` |
| `resources/seeds/java-backend-interview.json` | Curated Java backend template content |
| `controller/ApplicationController.java` | `POST /applications/{id}/apply-template` |
| `controller/InterviewController.java` | `?template=` on new interview GET |
| `templates/applications/detail.html` | Prep template card + templated interview link |
| `templates/interviews/form.html` | Template-loaded banner |
| `test/InterviewTemplateServiceTest.java` | Service integration tests |
| `test/ApplicationWebTest.java` | MockMvc prep template tests |
| `test/InterviewWebTest.java` | MockMvc debrief template tests |
| `README.md`, `JOURNAL.md` | Phase 3 docs (Task 16) |

---

### Task 14: Interview template service + seed pack

**Files:**
- Create: `src/main/java/com/offerflow/dto/DebriefTemplate.java`
- Create: `src/main/java/com/offerflow/dto/InterviewTemplatePack.java`
- Create: `src/main/java/com/offerflow/dto/ApplyPrepResult.java`
- Create: `src/main/java/com/offerflow/service/UnknownInterviewTemplateException.java`
- Create: `src/main/java/com/offerflow/service/InterviewTemplateService.java`
- Create: `src/main/resources/seeds/java-backend-interview.json`
- Create: `src/test/java/com/offerflow/InterviewTemplateServiceTest.java`
- Modify: `src/main/java/com/offerflow/service/JobApplicationService.java`

- [ ] **Step 1: Write the failing test file**

Create `src/test/java/com/offerflow/InterviewTemplateServiceTest.java`:

```java
package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.InterviewTemplateService;
import com.offerflow.service.JobApplicationService;
import com.offerflow.service.UnknownInterviewTemplateException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class InterviewTemplateServiceTest {

    @Autowired
    private InterviewTemplateService templateService;

    @Autowired
    private JobApplicationService applicationService;

    @Test
    void loadJavaBackendPack() {
        var pack = templateService.requirePack(InterviewTemplateService.JAVA_BACKEND);

        assertEquals("java-backend", pack.id());
        assertEquals("Java 后端", pack.title());
        assertTrue(pack.prepChecklist().contains("JVM"));
        assertTrue(pack.debrief().questionsAsked().contains("八股"));
    }

    @Test
    void applyPrepOnEmptyChecklist() {
        var app = applicationService.create(sampleApplication());

        var result = templateService.applyPrepChecklist(app.getId(), InterviewTemplateService.JAVA_BACKEND);

        assertTrue(result.applied());
        var reloaded = applicationService.requireApplication(app.getId());
        assertTrue(reloaded.getPrepChecklist().contains("JVM"));
    }

    @Test
    void applyPrepSkipsWhenChecklistExists() {
        ApplicationForm form = sampleApplication();
        form.setPrepChecklist("已有清单");
        var app = applicationService.create(form);

        var result = templateService.applyPrepChecklist(app.getId(), InterviewTemplateService.JAVA_BACKEND);

        assertFalse(result.applied());
        assertEquals("已有清单", applicationService.requireApplication(app.getId()).getPrepChecklist());
    }

    @Test
    void applyDebriefTemplateFillsBlankFormFields() {
        InterviewNoteForm form = new InterviewNoteForm();

        templateService.applyDebriefTemplate(form, InterviewTemplateService.JAVA_BACKEND);

        assertEquals("技术面", form.getRoundLabel());
        assertNotNull(form.getQuestionsAsked());
        assertTrue(form.getQuestionsAsked().contains("项目深挖"));
        assertNotNull(form.getSelfAssessment());
        assertNotNull(form.getImprovements());
    }

    @Test
    void applyDebriefTemplateDoesNotOverwriteExistingFields() {
        InterviewNoteForm form = new InterviewNoteForm();
        form.setQuestionsAsked("我的问题记录");

        templateService.applyDebriefTemplate(form, InterviewTemplateService.JAVA_BACKEND);

        assertEquals("我的问题记录", form.getQuestionsAsked());
        assertNotNull(form.getSelfAssessment());
    }

    @Test
    void rejectsUnknownTemplateId() {
        assertThrows(
                UnknownInterviewTemplateException.class,
                () -> templateService.requirePack("unknown-pack"));
    }

    private ApplicationForm sampleApplication() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("测试公司");
        form.setPositionTitle("Java 后端");
        form.setStage(ApplicationStage.APPLIED);
        form.setAppliedAt(LocalDate.now());
        return form;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
cd C:\Users\Ray\offerflow
.\gradlew.bat test --tests "com.offerflow.InterviewTemplateServiceTest" --no-daemon
```

Expected: FAIL — `InterviewTemplateService` / DTOs not found.

- [ ] **Step 3: Create DTO records**

`src/main/java/com/offerflow/dto/DebriefTemplate.java`:

```java
package com.offerflow.dto;

public record DebriefTemplate(
        String roundLabel,
        String questionsAsked,
        String selfAssessment,
        String improvements) {}
```

`src/main/java/com/offerflow/dto/InterviewTemplatePack.java`:

```java
package com.offerflow.dto;

public record InterviewTemplatePack(
        String id,
        String title,
        String prepChecklist,
        DebriefTemplate debrief) {}
```

`src/main/java/com/offerflow/dto/ApplyPrepResult.java`:

```java
package com.offerflow.dto;

public record ApplyPrepResult(boolean applied) {}
```

- [ ] **Step 4: Create exception**

`src/main/java/com/offerflow/service/UnknownInterviewTemplateException.java`:

```java
package com.offerflow.service;

public class UnknownInterviewTemplateException extends RuntimeException {

    public UnknownInterviewTemplateException(String templateId) {
        super("Unknown interview template: " + templateId);
    }
}
```

- [ ] **Step 5: Add `applyPrepChecklistIfEmpty` to JobApplicationService**

In `src/main/java/com/offerflow/service/JobApplicationService.java`, add before `delete`:

```java
    public boolean applyPrepChecklistIfEmpty(Long id, String checklist) {
        JobApplication application = requireApplication(id);
        String existing = application.getPrepChecklist();
        if (existing != null && !existing.isBlank()) {
            return false;
        }
        application.setPrepChecklist(checklist);
        repository.save(application);
        return true;
    }
```

- [ ] **Step 6: Create seed JSON**

Create `src/main/resources/seeds/java-backend-interview.json`:

```json
{
  "id": "java-backend",
  "title": "Java 后端",
  "prepChecklist": "□ JVM：内存模型、GC 算法、OOM 排查\n□ 并发：线程池、synchronized、volatile、AQS\n□ 集合：HashMap 原理、ConcurrentHashMap\n□ Spring：IOC/AOP、Bean 生命周期、事务传播\n□ 数据库：索引、事务隔离、慢 SQL\n□ 缓存：Redis 数据结构、穿透/击穿/雪崩\n□ 消息队列：幂等、顺序、积压（如简历涉及）\n□ 网络：TCP/HTTP、HTTPS\n□ 微服务：注册发现、限流熔断（如简历涉及）\n□ 项目：亮点 2 个 + 难点 2 个 + 数据指标\n□ 算法：数组/链表/二叉树各 1 道",
  "debrief": {
    "roundLabel": "技术面",
    "questionsAsked": "## 八股 / 基础\n- \n\n## 项目深挖\n- \n\n## 算法 / 手写\n- \n\n## 开放题 / 场景设计\n- ",
    "selfAssessment": "## 答得好的\n- \n\n## 卡壳 / 不会的\n- \n\n## 整体自评（1-5 分）\n",
    "improvements": "- 待补知识点：\n- 表达方式改进：\n- 下次模拟练习："
  }
}
```

- [ ] **Step 7: Implement InterviewTemplateService**

`src/main/java/com/offerflow/service/InterviewTemplateService.java`:

```java
package com.offerflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerflow.dto.ApplyPrepResult;
import com.offerflow.dto.DebriefTemplate;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.dto.InterviewTemplatePack;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InterviewTemplateService {

    public static final String JAVA_BACKEND = "java-backend";

    private static final Map<String, String> TEMPLATE_RESOURCES = Map.of(
            JAVA_BACKEND, "seeds/java-backend-interview.json");

    private final JobApplicationService applicationService;
    private final ObjectMapper objectMapper;

    public InterviewTemplateService(JobApplicationService applicationService, ObjectMapper objectMapper) {
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public InterviewTemplatePack requirePack(String templateId) {
        return loadPack(resolveResourcePath(templateId));
    }

    public ApplyPrepResult applyPrepChecklist(Long applicationId, String templateId) {
        InterviewTemplatePack pack = requirePack(templateId);
        boolean applied = applicationService.applyPrepChecklistIfEmpty(applicationId, pack.prepChecklist());
        return new ApplyPrepResult(applied);
    }

    public void applyDebriefTemplate(InterviewNoteForm form, String templateId) {
        DebriefTemplate debrief = requirePack(templateId).debrief();
        if (isBlank(form.getRoundLabel()) && debrief.roundLabel() != null) {
            form.setRoundLabel(debrief.roundLabel());
        }
        if (isBlank(form.getQuestionsAsked())) {
            form.setQuestionsAsked(debrief.questionsAsked());
        }
        if (isBlank(form.getSelfAssessment())) {
            form.setSelfAssessment(debrief.selfAssessment());
        }
        if (isBlank(form.getImprovements())) {
            form.setImprovements(debrief.improvements());
        }
    }

    private String resolveResourcePath(String templateId) {
        String resourcePath = TEMPLATE_RESOURCES.get(templateId);
        if (resourcePath == null) {
            throw new UnknownInterviewTemplateException(templateId);
        }
        return resourcePath;
    }

    private InterviewTemplatePack loadPack(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalStateException("Template resource missing: " + resourcePath);
        }
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, InterviewTemplatePack.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read template resource: " + resourcePath, ex);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

```powershell
cd C:\Users\Ray\offerflow
.\gradlew.bat test --tests "com.offerflow.InterviewTemplateServiceTest" --no-daemon
```

Expected: BUILD SUCCESSFUL, 6 tests passed.

- [ ] **Step 9: Run full suite**

```powershell
.\gradlew.bat test --no-daemon
```

Expected: all tests pass.

- [ ] **Step 10: Commit**

```powershell
git add src/main/java/com/offerflow/dto/DebriefTemplate.java `
        src/main/java/com/offerflow/dto/InterviewTemplatePack.java `
        src/main/java/com/offerflow/dto/ApplyPrepResult.java `
        src/main/java/com/offerflow/service/UnknownInterviewTemplateException.java `
        src/main/java/com/offerflow/service/InterviewTemplateService.java `
        src/main/java/com/offerflow/service/JobApplicationService.java `
        src/main/resources/seeds/java-backend-interview.json `
        src/test/java/com/offerflow/InterviewTemplateServiceTest.java
git commit -m "feat(interview): add interview template service and java-backend pack"
```

---

### Task 15: Prep checklist template on application detail

**Files:**
- Modify: `src/main/java/com/offerflow/controller/ApplicationController.java`
- Modify: `src/main/resources/templates/applications/detail.html`
- Create: `src/test/java/com/offerflow/ApplicationWebTest.java`

- [ ] **Step 1: Write the failing web test**

Create `src/test/java/com/offerflow/ApplicationWebTest.java`:

```java
package com.offerflow;

import com.offerflow.service.InterviewTemplateService;
import com.offerflow.web.FlashMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void detailShowsPrepTemplateCard() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        mockMvc.perform(get(redirectUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Java 后端 · 准备模板")))
                .andExpect(content().string(containsString("填充准备清单")));
    }

    @Test
    void applyTemplateFillsEmptyPrepChecklist() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(post("/applications/" + appId + "/apply-template")
                        .param("template", InterviewTemplateService.JAVA_BACKEND))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(FlashMessages.SUCCESS, containsString("已填充准备清单")));

        mockMvc.perform(get("/applications/" + appId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JVM")));
    }

    @Test
    void applyTemplateSkipsWhenPrepExists() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12")
                        .param("prepChecklist", "已有清单"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(post("/applications/" + appId + "/apply-template")
                        .param("template", InterviewTemplateService.JAVA_BACKEND))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(FlashMessages.SUCCESS, containsString("未覆盖")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
.\gradlew.bat test --tests "com.offerflow.ApplicationWebTest" --no-daemon
```

Expected: FAIL — route/UI not implemented.

- [ ] **Step 3: Add controller endpoint**

In `ApplicationController.java`:

1. Add imports:

```java
import com.offerflow.dto.ApplyPrepResult;
import com.offerflow.service.InterviewTemplateService;
import com.offerflow.service.UnknownInterviewTemplateException;
```

2. Add field + constructor param for `InterviewTemplateService`.

3. Add method (place before `@GetMapping("/new")` or after `detail`):

```java
    @PostMapping("/{id}/apply-template")
    public String applyTemplate(
            @PathVariable Long id,
            @RequestParam(defaultValue = InterviewTemplateService.JAVA_BACKEND) String template,
            RedirectAttributes redirectAttributes) {
        try {
            ApplyPrepResult result = interviewTemplateService.applyPrepChecklist(id, template);
            if (result.applied()) {
                redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "已填充准备清单。");
            } else {
                redirectAttributes.addFlashAttribute(
                        FlashMessages.SUCCESS, "准备清单已有内容，未覆盖。");
            }
        } catch (UnknownInterviewTemplateException ex) {
            redirectAttributes.addFlashAttribute(FlashMessages.ERROR, "模板不存在：" + template);
        }
        return "redirect:/applications/" + id;
    }
```

- [ ] **Step 4: Update application detail template**

In `src/main/resources/templates/applications/detail.html`, insert **before** the JD card block (before line `<div class="card" th:if="${jobApplication.jdContent...`):

```html
    <div class="card">
        <h2>Java 后端 · 准备模板</h2>
        <p class="muted">常见考点清单；已有内容不会覆盖。</p>
        <form method="post" th:action="@{/applications/{id}/apply-template(id=${jobApplication.id}, template='java-backend')}">
            <button class="btn btn-primary btn-sm" type="submit">填充准备清单</button>
        </form>
    </div>
```

In the interview section header (around line 87–89), replace the single link with:

```html
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
            <h2>面试复盘</h2>
            <div class="actions">
                <a class="btn btn-primary btn-sm" th:href="@{/applications/{id}/interviews/new(id=${jobApplication.id})}">+ 新增复盘</a>
                <a class="btn btn-sm" th:href="@{/applications/{id}/interviews/new(id=${jobApplication.id}, template='java-backend')}">+ 新增复盘（带模板）</a>
            </div>
        </div>
```

Remove the old single-line header that only had `+ 新增复盘`.

- [ ] **Step 5: Run ApplicationWebTest**

```powershell
.\gradlew.bat test --tests "com.offerflow.ApplicationWebTest" --no-daemon
```

Expected: 3 tests passed.

- [ ] **Step 6: Run full suite**

```powershell
.\gradlew.bat test --no-daemon
```

Expected: all tests pass.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/offerflow/controller/ApplicationController.java `
        src/main/resources/templates/applications/detail.html `
        src/test/java/com/offerflow/ApplicationWebTest.java
git commit -m "feat(application): add prep checklist template fill on detail page"
```

---

### Task 16: Debrief form prefill + web tests + docs

**Files:**
- Modify: `src/main/java/com/offerflow/controller/InterviewController.java`
- Modify: `src/main/resources/templates/interviews/form.html`
- Create: `src/test/java/com/offerflow/InterviewWebTest.java`
- Modify: `README.md`
- Modify: `JOURNAL.md`

- [ ] **Step 1: Write the failing web test**

Create `src/test/java/com/offerflow/InterviewWebTest.java`:

```java
package com.offerflow;

import com.offerflow.service.InterviewTemplateService;
import com.offerflow.web.FlashMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterviewWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void newInterviewWithTemplatePrefillsForm() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(get("/applications/" + appId + "/interviews/new")
                        .param("template", InterviewTemplateService.JAVA_BACKEND))
                .andExpect(status().isOk())
                .andExpect(view().name("interviews/form"))
                .andExpect(content().string(containsString("已加载 Java 后端复盘框架")))
                .andExpect(content().string(containsString("项目深挖")));
    }

    @Test
    void newInterviewWithUnknownTemplateRedirectsWithError() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(get("/applications/" + appId + "/interviews/new")
                        .param("template", "unknown-pack"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(FlashMessages.ERROR, containsString("模板不存在")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewWebTest" --no-daemon
```

Expected: FAIL — template param not handled.

- [ ] **Step 3: Update InterviewController**

1. Add imports:

```java
import com.offerflow.service.InterviewTemplateService;
import com.offerflow.service.UnknownInterviewTemplateException;
import org.springframework.web.bind.annotation.RequestParam;
```

2. Inject `InterviewTemplateService` in constructor.

3. Replace `createForm` method:

```java
    @GetMapping("/applications/{applicationId}/interviews/new")
    public String createForm(
            @PathVariable Long applicationId,
            @RequestParam(required = false) String template,
            Model model,
            RedirectAttributes redirectAttributes) {
        JobApplication application = applicationService.requireApplication(applicationId);
        InterviewNoteForm form = new InterviewNoteForm();
        form.setApplicationId(applicationId);
        if (template != null && !template.isBlank()) {
            try {
                interviewTemplateService.applyDebriefTemplate(form, template);
                model.addAttribute("templateLoaded", true);
            } catch (UnknownInterviewTemplateException ex) {
                redirectAttributes.addFlashAttribute(FlashMessages.ERROR, "模板不存在：" + template);
                return "redirect:/applications/" + applicationId;
            }
        }
        model.addAttribute("form", form);
        model.addAttribute("jobApplication", application);
        model.addAttribute("pageTitle", "新增面试复盘");
        return "interviews/form";
    }
```

- [ ] **Step 4: Update interview form template**

In `src/main/resources/templates/interviews/form.html`, after `<p class="muted" th:text=...>` add:

```html
    <p th:if="${templateLoaded}" class="flash flash-success" role="status">
        已加载 Java 后端复盘框架，请按实际面试填写。
    </p>
```

- [ ] **Step 5: Run InterviewWebTest**

```powershell
.\gradlew.bat test --tests "com.offerflow.InterviewWebTest" --no-daemon
```

Expected: 2 tests passed.

- [ ] **Step 6: Run full test suite**

```powershell
.\gradlew.bat test --no-daemon
```

Expected: all tests pass (including prior Company/Application tests).

- [ ] **Step 7: Update README.md**

1. In「解决了什么问题」table, add row:

```markdown
| **A3 准备无结构（Phase 3）** | 投递详情一键填充 Java 后端准备清单 |
| **A2 复盘框架（Phase 3）** | 新增复盘（带模板）预填问题/自评/改进结构 |
```

2. In「主要页面」table, update applications detail row:

```markdown
| `/applications/{id}` | 详情 + 公司档案 + 准备模板 + 面试复盘 |
```

3. Add new section after Phase 4 block:

```markdown
## Phase 3：面经 / 准备模板

Phase 3 补全 **A2 + A3**：面试前有结构化的准备清单，面试后有复盘框架可填。

| 能力 | 说明 |
|------|------|
| **准备清单模板** | 投递详情「填充准备清单」；仅空清单时写入 |
| **复盘框架** | 「新增复盘（带模板）」预填轮次、问题、自评、改进章节 |

### 相关 commit（Task 14–16）

\`\`\`
feat(interview): add interview template service and java-backend pack
feat(application): add prep checklist template fill on detail page
feat(interview): prefill debrief form from template + web tests
\`\`\`

设计背景见 [`docs/superpowers/specs/2026-07-12-interview-template-design.md`](docs/superpowers/specs/2026-07-12-interview-template-design.md)。
```

4. In「后续规划」, change Phase 3 line to:

```markdown
- 更多面试模板包（前端、Go 等）
```

- [ ] **Step 8: Update JOURNAL.md**

In Day 2「做了什么」bullet list, append:

```markdown
  - **Phase 3（面经/准备模板）**：`2026-07-12-interview-template-design.md`；Java 后端 prep + debrief seed；投递详情填充清单 + 带模板新增复盘（Task 14–16）
```

- [ ] **Step 9: Commit**

```powershell
git add src/main/java/com/offerflow/controller/InterviewController.java `
        src/main/resources/templates/interviews/form.html `
        src/test/java/com/offerflow/InterviewWebTest.java `
        README.md JOURNAL.md
git commit -m "feat(interview): prefill debrief form from template + web tests"
```

---

## Spec Coverage Checklist

| Spec requirement | Task |
|------------------|------|
| `java-backend-interview.json` with prep + debrief | Task 14 |
| Fill prep only when empty | Task 14 service + Task 15 |
| `POST apply-template` + flash messages | Task 15 |
| `GET interviews/new?template=` prefill | Task 16 |
| Unknown template error flash | Task 15 controller + Task 16 controller |
| No overwrite on edit interview | Task 16 (only `createForm`, not `editForm`) |
| Detail UI: prep card + two interview links | Task 15 |
| Form banner when template loaded | Task 16 |
| Service + web tests | Tasks 14–16 |
| README + JOURNAL | Task 16 |

## Manual QA (after Task 16)

1. `.\gradlew.bat bootRun` → open http://localhost:8080
2. Create application → detail → **填充准备清单** → see JVM items
3. Click again → flash「未覆盖」
4. **新增复盘（带模板）** → form has section headers
5. Save debrief → detail shows note
