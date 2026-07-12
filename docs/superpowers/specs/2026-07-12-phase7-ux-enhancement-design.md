# OfferFlow Phase 7 Design Spec — UX Enhancement (Feishu-style)

**Date:** 2026-07-12  
**Status:** Draft — pending user review  
**Author:** Brainstorming session with Ray  
**Scope:** Visual design system, responsive sidebar shell, dashboard + application list hero pages, light interactions

## 1. Problem & Goal

### Problem

Phase 0–6 delivered full job-search CRM functionality (A1–A4 + knowledge portability), but the UI remains a minimal hand-rolled stylesheet (~220 lines) with table-heavy layouts. Compared to mature SaaS products (Feishu, DingTalk, Huntr, Linear), OfferFlow feels like a functional admin panel rather than a polished product:

| Gap | Symptom |
|-----|---------|
| Visual polish | No design tokens, inconsistent spacing, generic purple badges, no icons |
| Layout | Top nav only; no sidebar workspace pattern familiar to Chinese enterprise users |
| Information density | Dashboard stats are static; list page actions scattered mid-page |
| Micro-interactions | No hover states, sticky headers, or clickable stat cards |

### Goal

**Phase 7** delivers **方案 B — design-system layering + layout fragments**, phased as user chose **D → C → B**:

1. **Design system** — Feishu/DingTalk-inspired tokens, typography, components
2. **App shell** — responsive left sidebar (desktop) + drawer (mobile ≤768px)
3. **Hero pages** — dashboard + application list deep visual + light interaction refresh
4. **Shell migration** — all other pages inherit new chrome without IA redesign

### Success Criteria

- Desktop: left sidebar 240px, Feishu-like neutral palette, content max-width ~1200px
- Mobile: sidebar hidden by default; hamburger opens drawer with overlay; aria attributes preserved
- Dashboard: three stat cards clickable (active → `/applications`, week → `/applications`, overdue → `/applications?overdue=1`)
- Application list: toolbar card (search + stage chips + overdue chip), sticky table header, row hover, stage color dots
- `GET /applications?overdue=1` filters to overdue applications only; combinable with `?q=` and `?stage=`
- All 16+ existing tests pass; at least one new smoke test for `?overdue=1`
- No new npm/build toolchain; Thymeleaf + CSS + vanilla JS only
- README Phase 7 row in evolution table; routes unchanged except new query param

### Non-Goals (Phase 7)

- Kanban view or drag-and-drop stage changes (Phase 8)
- Inline editing on list or detail pages
- Dark mode
- Application detail page IA redesign (shell + token only)
- New backend features beyond `overdue` query param
- Icon font libraries or illustration assets
- JSON import, auth, MySQL, mobile native app

### Context constraints (from brainstorming)

- **Audience:** solo localhost user; portfolio product narrative
- **Visual reference:** Feishu / DingTalk — enterprise neutral, cards + tables, domestic SaaS habits
- **Layout:** responsive hybrid (C) — sidebar desktop, collapsible drawer mobile
- **Depth:** visual + light interactions (B) — no drag, no inline edit
- **Phasing:** foundation + dashboard + list first; other pages shell-only

---

## 2. Chosen Approach

**方案 B (approved):** Split CSS into `tokens.css` + `layout.css` + `components.css`; new Thymeleaf layout fragments; hero page redesign; minimal `app-shell.js`.

### Rejected alternatives

| Approach | Reason rejected |
|----------|-----------------|
| A: Incremental single `app.css` | File becomes unmaintainable; poor reuse for Phase 8 |
| C: CSS framework (Bootstrap/Pico) | Feishu sidebar density hard to override; naming conflicts |

---

## 3. Design System (`tokens.css` + `components.css`)

### Color tokens

| Token | Value | Usage |
|-------|-------|-------|
| `--color-primary` | `#3370FF` | Primary buttons, links, selection |
| `--color-primary-hover` | `#2860E1` | Button hover |
| `--color-primary-light` | `#E8F3FF` | Sidebar active background |
| `--color-bg` | `#F5F6F7` | Page background |
| `--color-surface` | `#FFFFFF` | Cards, sidebar, table header |
| `--color-text` | `#1F2329` | Body text |
| `--color-text-secondary` | `#646A73` | Labels, table headers, muted |
| `--color-border` | `#DEE0E3` | Dividers, input borders |
| `--color-danger` | `#F54A45` | Overdue, delete, errors |
| `--color-success` | `#34C724` | Success states |
| `--color-warning-bg` | `#FFF5EB` | Overdue row background |

### Typography & spacing

- Font stack: `"PingFang SC", "Microsoft YaHei", "Segoe UI", system-ui, sans-serif`
- Sizes: page title 20px, section 16px, body 14px, caption 12px
- Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 (CSS variables)
- Radius: card 8px, button 6px, input 6px, badge 4px
- Card shadow on hover only: `0 2px 8px rgba(0,0,0,.06)`

### Stage color dots

8px dot + text label (never color alone). Map `ApplicationStage` enum to dot color:

| Dot color | `ApplicationStage` values |
|-----------|---------------------------|
| `#3370FF` (blue) | `APPLIED`, `SCREENING` |
| `#FF8800` (orange) | `TECH_INTERVIEW`, `FINAL_INTERVIEW` |
| `#34C724` (green) | `OFFER` |
| `#8F959E` (gray) | `REJECTED`, `WITHDRAWN` |

Implement via `StageStyles.dotColor(stage)` or equivalent in `com.offerflow.web` (alongside existing `StageLabels`).

### Component classes

| Class | Purpose |
|-------|---------|
| `.btn-primary` / `.btn-default` / `.btn-text` | Button variants |
| `.card` | White bordered panel, 16px padding |
| `.stat-card` | Clickable dashboard metric card |
| `.stat-grid` | 3-column responsive grid (replaces `.grid-3`) |
| `.data-table` | Sticky header, hover row, 44px row height |
| `.toolbar` | Search + filter chip row container |
| `.empty-state` | Centered message + CTA button |
| `.badge` + stage modifier | Dot + label |

### CSS entry

`app.css` imports `tokens.css`, `layout.css`, `components.css` (or layout template links all four). Legacy rules migrate into `components.css`; deprecated classes removed from hero pages.

---

## 4. Layout Shell

### Structure

```
Desktop:
┌──────────┬────────────────────────────────────┐
│ Sidebar  │  page-header (title + actions)     │
│ 240px    │  main-content                      │
│          │    flash messages                  │
│ nav items│    page body                       │
│ + CTA    │                                    │
└──────────┴────────────────────────────────────┘

Mobile (≤768px):
┌──────────────────────────────────────────────┐
│ [☰] OfferFlow                    [optional]  │
├──────────────────────────────────────────────┤
│ main-content (sidebar off-canvas)            │
└──────────────────────────────────────────────┘
```

### Thymeleaf fragments

| Fragment | File | Responsibility |
|----------|------|----------------|
| `shell(activePage, title, subtitle)` | `fragments/layout.html` | HTML skeleton, sidebar slot, main slot, script include |
| `sidebar(activePage)` | `fragments/nav.html` | Nav items + active state + bottom「+ 新增投递」 |
| `header(title, subtitle, actions)` | `fragments/page-header.html` | Title row with optional action buttons fragment param |

**Sidebar nav items**

| Label | Path | `activePage` key |
|-------|------|------------------|
| 仪表盘 | `/dashboard` | `dashboard` |
| 投递列表 | `/applications` | `applications` |
| 复盘搜索 | `/interviews/search` | `interviews-search` |
| 目标公司 | `/companies` | `companies` |
| + 新增投递 | `/applications/new` | `applications-new` (bottom CTA) |

**Active state:** 3px left blue bar + `--color-primary-light` background.

**Desktop:** No duplicate top nav links; page title only in `page-header`.

**Mobile:** Top bar with hamburger + brand; sidebar `translateX(-100%)` until opened; overlay dismisses drawer.

### JavaScript

`static/js/app-shell.js` (< 40 lines):

- Toggle drawer + `aria-expanded`
- Overlay click closes drawer
- Nav link click closes drawer on mobile
- Loaded from `layout.html` footer

### Error pages

`404.html` / `500.html`: no sidebar; centered message + link home; still use tokens for colors/fonts.

---

## 5. Dashboard Redesign

### Layout

1. `page-header`: 求职仪表盘 / 副标题 / [+ 新增投递]
2. `stat-grid`: three `.stat-card` elements
3. Conditional cards: 本周面试, 待跟进 (hidden when empty)
4. Card: 最近更新 (table or empty-state)

### Stat card links

| Card | Data | Click target |
|------|------|--------------|
| 进行中投递 | `dashboard.activeCount()` | `GET /applications` |
| 本周面试 | `dashboard.interviewsThisWeek()` | `GET /applications` (same list; optional future `?filter=week`) |
| 逾期未跟进 | `dashboard.overdueCount()` | `GET /applications?overdue=1` |

Implementation: wrap stat card in `<a class="stat-card" href="...">` for accessibility.

**Overdue card styling:** when count > 0, red number + 3px left border `--color-danger` on card.

### Tables

- Use `.data-table` with sticky header
- Stage column: dot + badge
- Week interview date format: `MM-dd（周X）` via Thymeleaf temporals
- Actions: `btn-text`「查看」

### Empty states

| Condition | UI |
|-----------|-----|
| No applications | empty-state in 最近更新 + link to `/applications/new` |
| No week interviews | hide 本周面试 card (`th:unless`) |
| No overdue | hide 待跟进 card |

---

## 6. Application List Redesign

### Layout

1. `page-header`: 投递列表 / 副标题 / [导出全部 Markdown (zip)] — export moved from body
2. `.toolbar` card: search row + stage chip row
3. Optional info banner when `overdue=1`: 「正在查看逾期未跟进投递」+ [查看全部]
4. `.data-table` or empty-state

### Query parameters

| Param | Behavior |
|-------|----------|
| `q` | Existing company/position search — unchanged |
| `stage` | Existing stage filter — unchanged |
| `overdue=1` | **New:** show only applications where `FollowUpRules.isOverdue(app, today)` |

All three may combine: `?stage=INTERVIEW&overdue=1&q=字节`.

**Controller change:** `ApplicationController` list handler reads `overdue` request param; filter in memory after existing query or extend service method — prefer reusing `FollowUpRules` for consistency with dashboard.

Template model: add `boolean overdueFilter` for banner and chip state.

### Toolbar

- Search: full-width input + primary submit + text「清除」when `q` present
- Stage chips: 全部 + each stage; active chip uses primary-light background
- When `overdue=1`: show active「逾期」chip; clearing removes param

### Table columns

公司 (link) | 岗位 | 阶段 (dot+badge) | 投递日 | 下次跟进 | 操作 (详情/编辑 as btn-text)

- Overdue rows: `--color-warning-bg` + red 逾期 tag
- Sticky header, row hover

### Mobile (≤640px)

- Toolbar stacks vertically
- Stage chips horizontal scroll
- Hide 投递日 column

### Empty states

| Condition | Copy |
|-----------|------|
| No records | 暂无投递记录 + CTA |
| Search miss | 没有匹配「{q}」+ 清除搜索 |
| Overdue filter empty | 没有逾期未跟进的投递，很棒！+ 查看全部 |

---

## 7. Other Pages — Shell Migration Only

Apply `layout :: shell` + `page-header` where applicable; align forms to new input/button styles; remove inline styles.

| Template | Notes |
|----------|-------|
| `applications/detail.html` | Title = company + position |
| `applications/form.html` | New/edit |
| `applications/template-preview.html` | Preview |
| `interviews/form.html` | Debrief form |
| `interviews/search.html` | Toolbar-style search |
| `companies/list.html` | Toolbar for q/industry |
| `companies/detail.html`, `form.html` | Standard header |
| `error/404.html`, `500.html` | Minimal layout |

No IA changes on detail (quick stage buttons, company card, debrief list stay as-is).

---

## 8. Testing & Accessibility

### Tests

- All existing test classes green after template/assertion updates
- **New:** `ApplicationControllerTest` — `GET /applications?overdue=1` returns 200
- **Optional:** dashboard stat card `href` assertions in existing or new web test

### Accessibility

- Sidebar toggle: `aria-expanded`, `aria-controls`, `aria-label`
- Stat cards: semantic `<a>`, not `div onclick`
- Table headers: `scope="col"`
- Stage: dot + text (not color-only)

---

## 9. File Manifest

### New files

```
src/main/resources/static/css/tokens.css
src/main/resources/static/css/layout.css
src/main/resources/static/css/components.css
src/main/resources/static/js/app-shell.js
src/main/resources/templates/fragments/layout.html
src/main/resources/templates/fragments/page-header.html
```

### Major edits

```
src/main/resources/static/css/app.css
src/main/resources/templates/fragments/nav.html
src/main/resources/templates/dashboard.html
src/main/resources/templates/applications/list.html
src/main/java/com/offerflow/controller/ApplicationController.java
```

### Minor edits

All remaining templates under `templates/` (shell swap + style class updates).

### Docs (Task final)

- `README.md` — Phase 7 evolution row, UX notes, `?overdue=1` param
- Optional `JOURNAL.md` note

---

## 10. Phase Boundaries

| Phase 7 | Phase 8+ (deferred) |
|---------|---------------------|
| Design tokens + sidebar shell | Kanban view toggle |
| Dashboard + list hero pages | Drag-and-drop stages |
| `?overdue=1` | Inline edit |
| Light interactions | Dark mode |
| Shell migration | Detail page sidebar layout |

---

## 11. Open Questions

None — all sections approved in brainstorming session 2026-07-12.
