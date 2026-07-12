# Phase 7 UX Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade OfferFlow to a Feishu-inspired design system with responsive sidebar shell, dashboard + application list hero pages, and `?overdue=1` filter.

**Architecture:** Split CSS into `tokens.css` / `layout.css` / `components.css`; Thymeleaf `layout` + `page-header` + `sidebar` fragments; `StageStyles` for stage dots; filter overdue in `ApplicationController` via `FollowUpRules`; vanilla `app-shell.js` for mobile drawer. No new build toolchain.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Thymeleaf, hand-written CSS, vanilla JS, JUnit 5, MockMvc

**Spec:** `docs/superpowers/specs/2026-07-12-phase7-ux-enhancement-design.md`

**Prerequisite:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
cd C:\Users\Ray\offerflow
```

---

## File Map

| File | Responsibility |
|------|----------------|
| `static/css/tokens.css` | Feishu color/spacing/radius variables |
| `static/css/layout.css` | `.app-shell`, sidebar, main-wrap, responsive drawer |
| `static/css/components.css` | Buttons, cards, tables, badges, toolbar, empty-state |
| `static/css/app.css` | Entry: `@import` the three files above |
| `static/js/app-shell.js` | Mobile sidebar toggle + overlay |
| `web/StageStyles.java` | Stage → CSS dot color class |
| `templates/fragments/layout.html` | `shell(activePage, title, subtitle, actions, main)` |
| `templates/fragments/page-header.html` | Title row + optional actions slot |
| `templates/fragments/nav.html` | Left sidebar (replaces top nav) |
| `templates/dashboard.html` | Hero page: stat cards + tables |
| `templates/applications/list.html` | Hero page: toolbar + data-table |
| `controller/ApplicationController.java` | +`overdue` request param |
| Remaining `templates/**/*.html` | Shell migration only |
| `test/.../StageStylesTest.java` | Stage dot mapping |
| `test/.../ApplicationWebTest.java` | +overdue filter smoke test |

---

### Task 31: Design tokens

**Files:**
- Create: `src/main/resources/static/css/tokens.css`
- Modify: `src/main/resources/static/css/app.css`

- [ ] **Step 1: Create `tokens.css`**

```css
:root {
    --color-primary: #3370ff;
    --color-primary-hover: #2860e1;
    --color-primary-light: #e8f3ff;
    --color-bg: #f5f6f7;
    --color-surface: #ffffff;
    --color-text: #1f2329;
    --color-text-secondary: #646a73;
    --color-border: #dee0e3;
    --color-danger: #f54a45;
    --color-success: #34c724;
    --color-warning-bg: #fff5eb;
    --color-row-hover: #f0f1f2;
    --color-table-header: #f5f6f7;
    --font-family: "PingFang SC", "Microsoft YaHei", "Segoe UI", system-ui, sans-serif;
    --font-size-title: 20px;
    --font-size-section: 16px;
    --font-size-body: 14px;
    --font-size-caption: 12px;
    --space-1: 4px;
    --space-2: 8px;
    --space-3: 12px;
    --space-4: 16px;
    --space-5: 24px;
    --space-6: 32px;
    --radius-card: 8px;
    --radius-btn: 6px;
    --radius-input: 6px;
    --radius-badge: 4px;
    --sidebar-width: 240px;
    --content-max-width: 1200px;
    --shadow-hover: 0 2px 8px rgba(0, 0, 0, 0.06);
}
```

- [ ] **Step 2: Replace `app.css` body with imports (keep file as entry)**

Replace entire `src/main/resources/static/css/app.css` with:

```css
@import url("tokens.css");
@import url("layout.css");
@import url("components.css");
```

(Tasks 32–33 will create `layout.css` and `components.css`; until then create empty placeholder files so imports resolve.)

- [ ] **Step 3: Create empty placeholders**

Create `src/main/resources/static/css/layout.css` and `components.css` each containing:

```css
/* populated in Task 32 */
```

- [ ] **Step 4: Run tests**

```powershell
.\gradlew.bat test --no-daemon
```

Expected: BUILD SUCCESSFUL (CSS-only change)

- [ ] **Step 5: Commit**

```powershell
git add src/main/resources/static/css/
git commit -m "feat(ui): add Feishu-style design tokens"
```

---

### Task 32: Layout + components CSS

**Files:**
- Modify: `src/main/resources/static/css/layout.css`
- Modify: `src/main/resources/static/css/components.css`

- [ ] **Step 1: Write `layout.css`**

```css
* { box-sizing: border-box; }

body {
    margin: 0;
    font-family: var(--font-family);
    font-size: var(--font-size-body);
    background: var(--color-bg);
    color: var(--color-text);
    line-height: 1.5;
}

.app-shell {
    display: flex;
    min-height: 100vh;
}

.sidebar {
    width: var(--sidebar-width);
    flex-shrink: 0;
    background: var(--color-surface);
    border-right: 1px solid var(--color-border);
    display: flex;
    flex-direction: column;
    padding: var(--space-4) 0;
}

.sidebar-brand {
    padding: 0 var(--space-4) var(--space-4);
    font-size: var(--font-size-section);
    font-weight: 700;
    color: var(--color-primary);
    text-decoration: none;
}

.sidebar-nav { flex: 1; }

.sidebar-link {
    display: flex;
    align-items: center;
    padding: var(--space-2) var(--space-4);
    color: var(--color-text);
    text-decoration: none;
    font-weight: 500;
    border-left: 3px solid transparent;
}

.sidebar-link:hover { background: var(--color-row-hover); }

.sidebar-link.is-active {
    color: var(--color-primary);
    background: var(--color-primary-light);
    border-left-color: var(--color-primary);
}

.sidebar-cta {
    margin: var(--space-4);
    margin-top: auto;
}

.main-wrap {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.mobile-topbar {
    display: none;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    background: var(--color-surface);
    border-bottom: 1px solid var(--color-border);
}

.main-content {
    flex: 1;
    max-width: var(--content-max-width);
    width: 100%;
    margin: 0 auto;
    padding: var(--space-5) var(--space-4) var(--space-6);
}

.sidebar-overlay {
    display: none;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 90;
}

.sidebar-overlay.is-visible { display: block; }

@media (max-width: 768px) {
    .mobile-topbar { display: flex; }

    .sidebar {
        position: fixed;
        top: 0;
        left: 0;
        bottom: 0;
        z-index: 100;
        transform: translateX(-100%);
        transition: transform 0.2s ease;
    }

    .sidebar.is-open { transform: translateX(0); }

    .sidebar-brand { display: none; }
}
```

- [ ] **Step 2: Write `components.css`** (migrate + enhance from old `app.css`)

Include at minimum:

```css
/* page-header */
.page-header {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-3);
    margin-bottom: var(--space-5);
}
.page-header h1 {
    margin: 0;
    font-size: var(--font-size-title);
    font-weight: 600;
}
.page-header .subtitle {
    margin: var(--space-1) 0 0;
    color: var(--color-text-secondary);
    font-size: var(--font-size-body);
}
.page-header-actions {
    display: flex;
    gap: var(--space-2);
    flex-wrap: wrap;
}

/* buttons */
.btn {
    display: inline-block;
    padding: var(--space-2) var(--space-3);
    border-radius: var(--radius-btn);
    border: 1px solid var(--color-border);
    background: var(--color-surface);
    color: var(--color-text);
    text-decoration: none;
    cursor: pointer;
    font-size: var(--font-size-body);
    line-height: 1.4;
}
.btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.btn-primary {
    background: var(--color-primary);
    border-color: var(--color-primary);
    color: #fff;
}
.btn-primary:hover { background: var(--color-primary-hover); color: #fff; }
.btn-text { border-color: transparent; background: transparent; }
.btn-text:hover { background: var(--color-row-hover); }
.btn-sm { padding: var(--space-1) var(--space-2); font-size: var(--font-size-caption); }
.btn-danger { background: var(--color-danger); border-color: var(--color-danger); color: #fff; }

/* cards */
.card {
    background: var(--color-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-card);
    padding: var(--space-4);
    margin-bottom: var(--space-4);
}
.card h2, .card h3 { margin-top: 0; font-size: var(--font-size-section); }
.card-alert { border-left: 3px solid var(--color-danger); background: var(--color-warning-bg); }

/* stat cards */
.stat-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: var(--space-4);
    margin-bottom: var(--space-4);
}
.stat-card {
    display: block;
    background: var(--color-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-card);
    padding: var(--space-4);
    text-decoration: none;
    color: inherit;
    transition: box-shadow 0.15s ease;
}
.stat-card:hover { box-shadow: var(--shadow-hover); }
.stat-value { font-size: 28px; font-weight: 600; line-height: 1.2; }
.stat-value.stat-alert { color: var(--color-danger); }
.stat-label { color: var(--color-text-secondary); font-size: var(--font-size-caption); margin-top: var(--space-1); }

/* tables */
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td {
    padding: var(--space-2) var(--space-2);
    border-bottom: 1px solid var(--color-border);
    text-align: left;
}
.data-table th {
    position: sticky;
    top: 0;
    background: var(--color-table-header);
    color: var(--color-text-secondary);
    font-size: var(--font-size-caption);
    font-weight: 600;
    z-index: 1;
}
.data-table tbody tr:hover { background: var(--color-row-hover); }
.data-table tbody tr.row-overdue { background: var(--color-warning-bg); }

/* badges */
.badge {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
    padding: 2px var(--space-2);
    border-radius: var(--radius-badge);
    font-size: var(--font-size-caption);
    background: var(--color-table-header);
    color: var(--color-text);
}
.stage-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
}
.stage-dot--blue { background: #3370ff; }
.stage-dot--orange { background: #ff8800; }
.stage-dot--green { background: #34c724; }
.stage-dot--gray { background: #8f959e; }

/* toolbar, chips, empty-state, flash, forms — port remaining rules from old app.css */
.toolbar { display: flex; flex-wrap: wrap; gap: var(--space-2); align-items: center; }
.chip-row { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-top: var(--space-3); }
.chip.is-active { background: var(--color-primary-light); border-color: var(--color-primary); color: var(--color-primary); }
.filter-banner {
    padding: var(--space-3) var(--space-4);
    background: var(--color-primary-light);
    border-radius: var(--radius-card);
    margin-bottom: var(--space-4);
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--space-2);
}
.empty-state { text-align: center; padding: var(--space-6) var(--space-4); color: var(--color-text-secondary); }
label { display: block; margin: var(--space-3) 0 var(--space-1); font-weight: 600; }
input, select, textarea {
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-input);
    font: inherit;
}
textarea { min-height: 100px; resize: vertical; }
.error { color: var(--color-danger); font-size: var(--font-size-caption); }
.flash { padding: var(--space-3) var(--space-4); border-radius: var(--radius-card); margin-bottom: var(--space-4); font-weight: 600; }
.flash-success { background: #e8ffea; border: 1px solid #b7edb9; color: #1a7f37; }
.flash-error { background: #ffece8; border: 1px solid #ffc8c2; color: var(--color-danger); }
.actions { display: flex; gap: var(--space-2); flex-wrap: wrap; }
.muted { color: var(--color-text-secondary); }
.overdue-tag {
    display: inline-block;
    margin-left: var(--space-1);
    padding: 1px 6px;
    border-radius: 999px;
    font-size: var(--font-size-caption);
    background: #ffece8;
    color: var(--color-danger);
    font-weight: 600;
}
.stage-actions { display: flex; gap: var(--space-2); flex-wrap: wrap; margin: var(--space-3) 0; }
td.actions { white-space: nowrap; }

@media (max-width: 640px) {
    .chip-row { overflow-x: auto; flex-wrap: nowrap; padding-bottom: var(--space-1); }
    .data-table .col-applied-at { display: none; }
}
```

- [ ] **Step 3: Run tests**

```powershell
.\gradlew.bat test --no-daemon
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```powershell
git add src/main/resources/static/css/layout.css src/main/resources/static/css/components.css
git commit -m "feat(ui): add layout and component styles"
```

---

### Task 33: Sidebar nav + app-shell.js

**Files:**
- Create: `src/main/resources/static/js/app-shell.js`
- Modify: `src/main/resources/templates/fragments/nav.html`

- [ ] **Step 1: Create `app-shell.js`**

```javascript
(function () {
    var toggle = document.querySelector('[data-sidebar-toggle]');
    var sidebar = document.querySelector('.sidebar');
    var overlay = document.querySelector('.sidebar-overlay');
    if (!toggle || !sidebar) {
        return;
    }
    function setOpen(open) {
        sidebar.classList.toggle('is-open', open);
        if (overlay) {
            overlay.classList.toggle('is-visible', open);
        }
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        toggle.setAttribute('aria-label', open ? '关闭菜单' : '打开菜单');
    }
    toggle.addEventListener('click', function () {
        setOpen(!sidebar.classList.contains('is-open'));
    });
    if (overlay) {
        overlay.addEventListener('click', function () { setOpen(false); });
    }
    sidebar.querySelectorAll('a').forEach(function (link) {
        link.addEventListener('click', function () { setOpen(false); });
    });
})();
```

- [ ] **Step 2: Rewrite `fragments/nav.html` as sidebar**

Replace content with sidebar fragment (keep `th:fragment="sidebar(activePage)"`):

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
<aside class="sidebar" th:fragment="sidebar(activePage)">
    <a class="sidebar-brand" th:href="@{/}">OfferFlow</a>
    <nav class="sidebar-nav" aria-label="主导航">
        <a class="sidebar-link" th:href="@{/dashboard}"
           th:classappend="${activePage == 'dashboard'} ? ' is-active' : ''">仪表盘</a>
        <a class="sidebar-link" th:href="@{/applications}"
           th:classappend="${activePage == 'applications'} ? ' is-active' : ''">投递列表</a>
        <a class="sidebar-link" th:href="@{/interviews/search}"
           th:classappend="${activePage == 'interviews-search'} ? ' is-active' : ''">复盘搜索</a>
        <a class="sidebar-link" th:href="@{/companies}"
           th:classappend="${activePage == 'companies' or activePage == 'companies-new'} ? ' is-active' : ''">目标公司</a>
    </nav>
    <a class="btn btn-primary sidebar-cta" th:href="@{/applications/new}">+ 新增投递</a>
</aside>
</body>
</html>
```

- [ ] **Step 3: Run tests** (nav text still present on pages using old header until Task 34)

```powershell
.\gradlew.bat test --no-daemon
```

- [ ] **Step 4: Commit**

```powershell
git add src/main/resources/static/js/app-shell.js src/main/resources/templates/fragments/nav.html
git commit -m "feat(ui): add sidebar navigation and mobile drawer script"
```

---

### Task 34: Layout + page-header fragments

**Files:**
- Create: `src/main/resources/templates/fragments/layout.html`
- Create: `src/main/resources/templates/fragments/page-header.html`

- [ ] **Step 1: Create `fragments/layout.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org"
      th:fragment="shell (activePage, title, subtitle, actions, main)">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title th:text="${title} + ' · OfferFlow'">OfferFlow</title>
    <link rel="stylesheet" th:href="@{/css/app.css}"/>
</head>
<body class="app-shell">
<div class="mobile-topbar">
    <button type="button" class="btn btn-sm" data-sidebar-toggle
            aria-expanded="false" aria-controls="app-sidebar" aria-label="打开菜单">☰</button>
    <a class="sidebar-brand" th:href="@{/}">OfferFlow</a>
</div>
<div class="sidebar-overlay"></div>
<aside id="app-sidebar" th:replace="~{fragments/nav :: sidebar(${activePage})}"></aside>
<div class="main-wrap">
    <div class="main-content">
        <div th:replace="~{fragments/messages :: flash}"></div>
        <div th:replace="~{fragments/page-header :: header(${title}, ${subtitle}, ${actions})}"></div>
        <th:block th:insert="${main}"></th:block>
    </div>
</div>
<script th:src="@{/js/app-shell.js}"></script>
</body>
</html>
```

- [ ] **Step 2: Create `fragments/page-header.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
<header class="page-header" th:fragment="header(title, subtitle, actions)">
    <div>
        <h1 th:text="${title}">标题</h1>
        <p class="subtitle" th:if="${subtitle != null and !subtitle.isBlank()}" th:text="${subtitle}"></p>
    </div>
    <div class="page-header-actions" th:if="${actions != null}" th:insert="${actions}"></div>
</header>
</body>
</html>
```

- [ ] **Step 3: Commit**

```powershell
git add src/main/resources/templates/fragments/layout.html src/main/resources/templates/fragments/page-header.html
git commit -m "feat(ui): add layout and page-header fragments"
```

---

### Task 35: StageStyles helper

**Files:**
- Create: `src/main/java/com/offerflow/web/StageStyles.java`
- Create: `src/test/java/com/offerflow/StageStylesTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.offerflow;

import com.offerflow.model.ApplicationStage;
import com.offerflow.web.StageStyles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StageStylesTest {

    @Test
    void appliedUsesBlueDot() {
        assertEquals("stage-dot--blue", StageStyles.dotClass(ApplicationStage.APPLIED));
    }

    @Test
    void techInterviewUsesOrangeDot() {
        assertEquals("stage-dot--orange", StageStyles.dotClass(ApplicationStage.TECH_INTERVIEW));
    }

    @Test
    void offerUsesGreenDot() {
        assertEquals("stage-dot--green", StageStyles.dotClass(ApplicationStage.OFFER));
    }

    @Test
    void rejectedUsesGrayDot() {
        assertEquals("stage-dot--gray", StageStyles.dotClass(ApplicationStage.REJECTED));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
.\gradlew.bat test --tests "com.offerflow.StageStylesTest" --no-daemon
```

Expected: FAIL — `StageStyles` not found

- [ ] **Step 3: Implement `StageStyles.java`**

```java
package com.offerflow.web;

import com.offerflow.model.ApplicationStage;

public final class StageStyles {

    private StageStyles() {}

    public static String dotClass(ApplicationStage stage) {
        if (stage == null) {
            return "stage-dot--gray";
        }
        return switch (stage) {
            case APPLIED, SCREENING -> "stage-dot--blue";
            case TECH_INTERVIEW, FINAL_INTERVIEW -> "stage-dot--orange";
            case OFFER -> "stage-dot--green";
            case REJECTED, WITHDRAWN -> "stage-dot--gray";
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
.\gradlew.bat test --tests "com.offerflow.StageStylesTest" --no-daemon
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/offerflow/web/StageStyles.java src/test/java/com/offerflow/StageStylesTest.java
git commit -m "feat(ui): add stage color dot helper"
```

---

### Task 36: Overdue list filter

**Files:**
- Modify: `src/main/java/com/offerflow/controller/ApplicationController.java`
- Modify: `src/test/java/com/offerflow/ApplicationWebTest.java`
- Modify: `src/main/resources/templates/applications/list.html` (minimal banner placeholder — full UI in Task 38)

- [ ] **Step 1: Write failing web test**

Add to `ApplicationWebTest.java`:

```java
@Test
void listOverdueFilterReturns200() throws Exception {
    mockMvc.perform(post("/applications")
                    .param("companyName", "逾期公司")
                    .param("positionTitle", "Java")
                    .param("stage", "APPLIED")
                    .param("appliedAt", "2026-07-01")
                    .param("nextFollowUpAt", "2026-07-01"))
            .andExpect(status().is3xxRedirection());

    mockMvc.perform(get("/applications").param("overdue", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("逾期公司")));
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
.\gradlew.bat test --tests "com.offerflow.ApplicationWebTest.listOverdueFilterReturns200" --no-daemon
```

Expected: FAIL — overdue company not shown (filter not implemented)

- [ ] **Step 3: Update `ApplicationController#list`**

```java
@GetMapping
public String list(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) ApplicationStage stage,
        @RequestParam(required = false, defaultValue = "false") boolean overdue,
        Model model) {
    LocalDate today = LocalDate.now();
    var applications = applicationService.search(Optional.ofNullable(q), Optional.ofNullable(stage));
    if (overdue) {
        applications = applications.stream()
                .filter(app -> FollowUpRules.isOverdue(app, today))
                .toList();
    }
    model.addAttribute("applications", applications);
    model.addAttribute("searchQuery", q);
    model.addAttribute("selectedStage", stage);
    model.addAttribute("overdueFilter", overdue);
    model.addAttribute("stages", ApplicationStage.values());
    model.addAttribute("stageLabels", StageLabels.all());
    model.addAttribute("today", today);
    return "applications/list";
}
```

Add import: `import com.offerflow.support.FollowUpRules;`

- [ ] **Step 4: Run test to verify it passes**

```powershell
.\gradlew.bat test --tests "com.offerflow.ApplicationWebTest.listOverdueFilterReturns200" --no-daemon
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/offerflow/controller/ApplicationController.java src/test/java/com/offerflow/ApplicationWebTest.java
git commit -m "feat(application): add overdue filter query param"
```

---

### Task 37: Dashboard hero page

**Files:**
- Modify: `src/main/resources/templates/dashboard.html`
- Modify: `src/test/java/com/offerflow/CompanyWebTest.java` (nav test still valid)

- [ ] **Step 1: Rewrite `dashboard.html` using layout shell**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: shell('dashboard', '求职仪表盘', '一眼看清投递进度、本周面试与待跟进事项。', ~{::#headerActions}, ~{::#pageBody})}">
<body>
<th:block id="headerActions">
    <a class="btn btn-primary" th:href="@{/applications/new}">+ 新增投递</a>
</th:block>
<th:block id="pageBody">
    <div class="stat-grid">
        <a class="stat-card" th:href="@{/applications}">
            <div class="stat-value" th:text="${dashboard.activeCount()}">0</div>
            <div class="stat-label">进行中投递</div>
        </a>
        <a class="stat-card" th:href="@{/applications}">
            <div class="stat-value" th:text="${dashboard.interviewsThisWeek()}">0</div>
            <div class="stat-label">本周面试</div>
        </a>
        <a class="stat-card" th:href="@{/applications(overdue=1)}"
           th:classappend="${dashboard.overdueCount() > 0} ? ' card-alert' : ''">
            <div class="stat-value" th:classappend="${dashboard.overdueCount() > 0} ? 'stat-alert' : ''"
                 th:text="${dashboard.overdueCount()}">0</div>
            <div class="stat-label">逾期未跟进</div>
        </a>
    </div>

    <div class="card" th:unless="${dashboard.weekInterviews().isEmpty()}">
        <h2>本周面试</h2>
        <table class="data-table">
            <thead>
            <tr><th>日期</th><th>公司</th><th>岗位</th><th>轮次</th><th></th></tr>
            </thead>
            <tbody>
            <tr th:each="item : ${dashboard.weekInterviews()}">
                <td th:text="${#temporals.format(item.interviewDate(), 'MM-dd')}">日期</td>
                <td th:text="${item.companyName()}">公司</td>
                <td th:text="${item.positionTitle()}">岗位</td>
                <td th:text="${item.roundLabel() != null ? item.roundLabel() : '-'}">轮次</td>
                <td><a class="btn btn-text btn-sm" th:href="@{/applications/{id}(id=${item.applicationId()})}">查看</a></td>
            </tr>
            </tbody>
        </table>
    </div>

    <div class="card" th:unless="${dashboard.overdueApplications().isEmpty()}">
        <h2>待跟进</h2>
        <table class="data-table">
            <thead>
            <tr><th>公司</th><th>岗位</th><th>阶段</th><th>跟进日</th><th></th></tr>
            </thead>
            <tbody>
            <tr th:each="app : ${dashboard.overdueApplications()}" class="row-overdue">
                <td th:text="${app.companyName}">公司</td>
                <td th:text="${app.positionTitle}">岗位</td>
                <td>
                    <span class="badge">
                        <span class="stage-dot" th:classappend="${T(com.offerflow.web.StageStyles).dotClass(app.stage)}"></span>
                        <span th:text="${T(com.offerflow.web.StageLabels).label(app.stage)}">阶段</span>
                    </span>
                </td>
                <td th:text="${app.nextFollowUpAt}">日期</td>
                <td><a class="btn btn-text btn-sm" th:href="@{/applications/{id}(id=${app.id})}">查看</a></td>
            </tr>
            </tbody>
        </table>
    </div>

    <div class="card">
        <h2>最近更新</h2>
        <table class="data-table" th:if="${!dashboard.recentApplications().isEmpty()}">
            <thead>
            <tr><th>公司</th><th>岗位</th><th>阶段</th><th>更新于</th></tr>
            </thead>
            <tbody>
            <tr th:each="app : ${dashboard.recentApplications()}">
                <td><a th:href="@{/applications/{id}(id=${app.id})}" th:text="${app.companyName}">公司</a></td>
                <td th:text="${app.positionTitle}">岗位</td>
                <td>
                    <span class="badge">
                        <span class="stage-dot" th:classappend="${T(com.offerflow.web.StageStyles).dotClass(app.stage)}"></span>
                        <span th:text="${T(com.offerflow.web.StageLabels).label(app.stage)}">阶段</span>
                    </span>
                </td>
                <td th:text="${#temporals.format(app.updatedAt, 'yyyy-MM-dd HH:mm')}">时间</td>
            </tr>
            </tbody>
        </table>
        <div th:if="${dashboard.recentApplications().isEmpty()}" class="empty-state">
            <p>还没有投递记录</p>
            <a class="btn btn-primary" th:href="@{/applications/new}">添加第一条</a>
        </div>
    </div>
</th:block>
</body>
</html>
```

- [ ] **Step 2: Add dashboard stat link test**

Create `src/test/java/com/offerflow/DashboardWebTest.java`:

```java
package com.offerflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void dashboardShowsOverdueStatLink() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/applications?overdue=1")));
    }

    @Test
    void dashboardShowsSidebar() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sidebar-link")));
    }
}
```

- [ ] **Step 3: Run tests**

```powershell
.\gradlew.bat test --tests "com.offerflow.DashboardWebTest" --no-daemon
```

Expected: PASS

- [ ] **Step 4: Commit**

```powershell
git add src/main/resources/templates/dashboard.html src/test/java/com/offerflow/DashboardWebTest.java
git commit -m "feat(ui): redesign dashboard with stat cards and sidebar layout"
```

---

### Task 38: Application list hero page

**Files:**
- Modify: `src/main/resources/templates/applications/list.html`

- [ ] **Step 1: Rewrite `applications/list.html`**

Key elements:
- `th:replace="~{fragments/layout :: shell('applications', '投递列表', '管理全部投递，按阶段筛选或搜索。', ~{::#headerActions}, ~{::#pageBody})}"`
- `#headerActions`: export-all button
- `filter-banner` when `overdueFilter`
- `.toolbar` card with search + stage chips (preserve existing `q` / `stage` URL params)
- `.data-table` with `stage-dot`, `row-overdue`, `col-applied-at` on 投递日 column
- empty states per spec §6

Stage chip example:

```html
<a class="btn btn-sm chip" th:classappend="${selectedStage == null} ? ' is-active' : ''"
   th:href="@{/applications(q=${searchQuery}, overdue=${overdueFilter ? 1 : null})}">全部</a>
```

Search form preserves `overdue`:

```html
<input type="hidden" name="overdue" th:if="${overdueFilter}" value="1"/>
```

- [ ] **Step 2: Extend `ApplicationWebTest`**

```java
@Test
void listOverdueFilterShowsBanner() throws Exception {
    mockMvc.perform(get("/applications").param("overdue", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("逾期未跟进")));
}
```

- [ ] **Step 3: Run tests**

```powershell
.\gradlew.bat test --tests "com.offerflow.ApplicationWebTest" --no-daemon
```

Expected: PASS

- [ ] **Step 4: Commit**

```powershell
git add src/main/resources/templates/applications/list.html src/test/java/com/offerflow/ApplicationWebTest.java
git commit -m "feat(ui): redesign application list with toolbar and data-table"
```

---

### Task 39: Shell migration for remaining templates

**Files:**
- Modify: `templates/applications/detail.html`
- Modify: `templates/applications/form.html`
- Modify: `templates/applications/template-preview.html`
- Modify: `templates/interviews/form.html`
- Modify: `templates/interviews/search.html`
- Modify: `templates/companies/list.html`
- Modify: `templates/companies/detail.html`
- Modify: `templates/companies/form.html`
- Modify: `templates/error/404.html`
- Modify: `templates/error/500.html`

- [ ] **Step 1: Migrate each template to layout shell**

Pattern for each page:

```html
<html th:replace="~{fragments/layout :: shell('PAGE_KEY', '页面标题', '副标题或空字符串', ~{::#headerActions}, ~{::#pageBody})}">
```

Use `~{::#headerActions}` only when page has header actions; otherwise pass `null`:

```html
th:replace="~{fragments/layout :: shell('companies', '目标公司', '管理目标公司档案。', null, ~{::#pageBody})}"
```

**`activePage` keys:**

| Template | `activePage` |
|----------|--------------|
| `applications/detail.html` | `applications` |
| `applications/form.html` | `applications-new` (if new) or `applications` |
| `applications/template-preview.html` | `applications` |
| `interviews/form.html` | `applications` |
| `interviews/search.html` | `interviews-search` |
| `companies/list.html` | `companies` |
| `companies/detail.html` | `companies` |
| `companies/form.html` | `companies-new` |

**Error pages** — minimal standalone (no sidebar):

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title>页面未找到 · OfferFlow</title>
    <link rel="stylesheet" th:href="@{/css/app.css}"/>
</head>
<body style="display:flex;align-items:center;justify-content:center;min-height:100vh;">
    <div class="empty-state">
        <h1>404</h1>
        <p>页面不存在</p>
        <a class="btn btn-primary" th:href="@{/dashboard}">回仪表盘</a>
    </div>
</body>
</html>
```

- [ ] **Step 2: Remove inline `style="..."` from migrated templates**

- [ ] **Step 3: Run full test suite**

```powershell
.\gradlew.bat test --no-daemon
```

Expected: BUILD SUCCESSFUL — fix any assertions that referenced old nav markup (`site-nav` → `sidebar-link`)

- [ ] **Step 4: Commit**

```powershell
git add src/main/resources/templates/
git commit -m "feat(ui): migrate remaining pages to sidebar layout shell"
```

---

### Task 40: Documentation

**Files:**
- Modify: `README.md`
- Modify: `JOURNAL.md` (optional one-liner)

- [ ] **Step 1: Update README**

Add to evolution table:

```markdown
| Phase 7 | UX | 飞书风设计系统、侧栏布局、仪表盘/列表改版、`?overdue=1` |
```

Add to routes table:

```markdown
| GET | `/applications` | 投递列表（`?q=` `?stage=` `?overdue=1`） |
```

Update test count if new test classes added (16 → 18).

- [ ] **Step 2: Run full tests**

```powershell
.\gradlew.bat test --no-daemon
```

- [ ] **Step 3: Commit**

```powershell
git add README.md JOURNAL.md
git commit -m "docs: document Phase 7 UX enhancement"
```

---

## Spec Coverage Checklist

| Spec § | Task |
|--------|------|
| Design tokens | 31 |
| Layout shell + sidebar | 32, 33, 34 |
| Stage color dots | 35 |
| `?overdue=1` | 36 |
| Dashboard hero | 37 |
| List hero | 38 |
| Other pages shell | 39 |
| README / docs | 40 |
| `app-shell.js` | 33 |
| Accessibility aria | 33, 34 |
| Non-goals deferred | — |

## Manual QA (post-Task 40)

1. `.\gradlew.bat bootRun` → http://localhost:8080/dashboard — sidebar visible desktop, stat cards clickable
2. Resize to <768px — hamburger opens drawer
3. `/applications?overdue=1` — banner + filtered rows
4. Spot-check detail/form pages — forms usable, no layout break
