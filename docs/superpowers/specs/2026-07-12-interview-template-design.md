# OfferFlow Phase 3 Design Spec — Interview Prep & Debrief Templates

**Date:** 2026-07-12  
**Status:** Approved (2026-07-12)  
**Author:** Brainstorming session with Ray  
**Scope:** Java backend interview template pack — prep checklist + debrief framework, fill-on-click (no template CRUD)

## 1. Problem & Goal

### Problem

After MVP (A1 pipeline + A2 debrief notes) and Phase 2/4 (A4 company dossier + seed), two pains remain weak:

| ID | Pain | Current state |
|----|------|---------------|
| A2 | Weak interview debrief | Free-text fields; users stare at blank textareas after interviews |
| A3 | Unstructured prep | `prepChecklist` field exists but user must invent checklist from scratch |

### Goal

Ship **Phase 3**: one curated **Java backend** template pack in classpath JSON that:

1. Fills **prep checklist** on an application (when empty)
2. Pre-fills **debrief form** when creating a new interview note

No AI, no scraping, no template database — same philosophy as Phase 4 company seeds.

### Success Criteria

- `src/main/resources/seeds/java-backend-interview.json` ships with prep + debrief sections.
- Application detail page has **填充准备清单**; empty checklist gets template text; non-empty is skipped with flash.
- **新增复盘（带模板）** opens new interview form with debrief fields pre-populated.
- Optional **插入模板** on empty debrief form only fills blank fields.
- Unknown template id → error flash; no overwrite of user-edited content.
- `.\gradlew.bat test` passes including service + web tests.

### Non-Goals (Phase 3 v1)

- Multiple template packs in UI (v1: `java-backend` only)
- `InterviewTemplate` entity or template CRUD
- Auto-fill on edit of existing interview notes
- Overwriting non-empty prep checklist or debrief fields
- Cross-application search of debrief content
- AI-generated questions or answers

## 2. Chosen Approach

**方案 A (approved):** Single classpath JSON + `InterviewTemplateService` + fill-on-click into existing `JobApplication` / `InterviewNoteForm` fields.

### Rejected alternatives

| Approach | Reason rejected |
|----------|-----------------|
| B: Template entity + CRUD | Scope too large for v1; duplicates company seed pattern unnecessarily |
| C: Static reference page only | High friction; does not improve A2/A3 measurably |

## 3. Seed Data

### File location

```
src/main/resources/seeds/java-backend-interview.json
```

### JSON schema (single object)

| Field | Required | Notes |
|-------|----------|-------|
| `id` | yes | Template pack id, e.g. `"java-backend"` |
| `title` | yes | Display label, e.g. `"Java 后端"` |
| `prepChecklist` | yes | Multi-line text; checkbox-style lines with `□` prefix |
| `debrief` | yes | Object with sub-fields below |
| `debrief.roundLabel` | no | Suggested round, e.g. `"技术面"` |
| `debrief.questionsAsked` | yes | Markdown-ish section headers + bullet placeholders |
| `debrief.selfAssessment` | yes | Prompt sections for self-review |
| `debrief.improvements` | yes | Action-item placeholders |

### Example (abbreviated)

```json
{
  "id": "java-backend",
  "title": "Java 后端",
  "prepChecklist": "□ JVM：内存模型、GC 算法、OOM 排查\n□ 并发：线程池、synchronized、volatile、AQS\n□ 集合：HashMap 原理、ConcurrentHashMap\n□ Spring：IOC/AOP、Bean 生命周期、事务传播\n□ 数据库：索引、事务隔离、慢 SQL\n□ 缓存：Redis 数据结构、穿透/击穿/雪崩\n□ 消息队列：幂等、顺序、积压（如简历涉及）\n□ 网络：TCP/HTTP、HTTPS\n□ 项目：亮点 2 个 + 难点 2 个 + 数据指标\n□ 算法：常见题型各刷 1 道（数组/链表/二叉树）",
  "debrief": {
    "roundLabel": "技术面",
    "questionsAsked": "## 八股 / 基础\n- \n\n## 项目深挖\n- \n\n## 算法 / 手写\n- \n\n## 开放题 / 场景设计\n- ",
    "selfAssessment": "## 答得好的\n- \n\n## 卡壳 / 不会的\n- \n\n## 整体自评（1-5 分）\n",
    "improvements": "- 待补知识点：\n- 表达方式改进：\n- 下次模拟练习："
  }
}
```

Implementation fills in full checklist (~15–20 items); content is curated static text, not generated.

## 4. Architecture

### New components

| Unit | Responsibility |
|------|----------------|
| `dto/InterviewTemplatePack.java` | Deserialize seed JSON (record or class) |
| `dto/DebriefTemplate.java` | Nested debrief section (record) |
| `dto/ApplyPrepResult.java` | `applied` boolean + optional message |
| `service/InterviewTemplateService.java` | Load pack, apply prep, build debrief form defaults |
| `service/UnknownInterviewTemplateException.java` | Unknown pack id |
| `ApplicationController` | `POST /applications/{id}/apply-template` |
| `InterviewController` | Support `?template=` on new interview GET |

### Fill rules

**Prep checklist (`JobApplication.prepChecklist`):**

```
if prepChecklist is null or blank:
  write template.prepChecklist
  return applied=true
else:
  return applied=false (flash: 已有内容，未覆盖)
```

**Debrief (`InterviewNoteForm` on NEW note only):**

```
if GET .../interviews/new?template=java-backend:
  for each debrief field:
    if form field is blank: set from template
  (date remains today default from controller)
```

**Insert template button on form (optional v1):**

```
POST or GET with action insert-template:
  same as above — only fill fields that are currently blank
```

Never auto-apply template on `GET .../interviews/{id}/edit`.

### Request flow

```
Application detail
  → POST /applications/{id}/apply-template?template=java-backend
    → InterviewTemplateService.applyPrepChecklist(appId, templateId)
  ← redirect:/applications/{id} + flash

Application detail
  → GET /applications/{id}/interviews/new?template=java-backend
    → InterviewTemplateService.enrichNewNoteForm(form, templateId)
  ← interviews/form.html (pre-filled)

Unknown template id
  ← redirect + FlashMessages.ERROR
```

## 5. UI Changes

### `applications/detail.html`

Add card above prep checklist section (always visible in v1):

- **Title:** `Java 后端 · 准备模板`
- **Subtitle:** `常见考点清单；已有内容不会覆盖`
- **Actions:**
  - `POST` **填充准备清单** → `/applications/{id}/apply-template?template=java-backend`
  - Change **+ 新增复盘** area:
    - Keep existing **+ 新增复盘** (blank form)
    - Add **+ 新增复盘（带模板）** → `.../interviews/new?template=java-backend`

### `interviews/form.html`

- When template loaded: info banner `已加载 Java 后端复盘框架，请按实际面试填写`
- When creating (no id): optional **插入模板** button if all debrief text fields empty

### Flash messages

| Case | Message |
|------|---------|
| Prep applied | `已填充准备清单。` |
| Prep skipped (non-empty) | `准备清单已有内容，未覆盖。` |
| Unknown template | `模板不存在：{templateId}` |

## 6. Error Handling

| Condition | Behavior |
|-----------|----------|
| Unknown `template` param | Redirect back + error flash |
| Missing JSON resource | `IllegalStateException` → 500 + log |
| Application not found | Existing `ApplicationNotFoundException` / 404 path |

No schema migration required — uses existing `prepChecklist` and `InterviewNote` LOB fields.

## 7. Testing

### Unit / integration

| Test | Assertion |
|------|-----------|
| `loadPack_javaBackend` | Pack id, title, non-empty prep + debrief |
| `applyPrep_emptyChecklist` | Field written, `applied=true` |
| `applyPrep_existingChecklist` | Field unchanged, `applied=false` |
| `buildDebriefForm` | Form fields match template sections |
| `unknownTemplate` | Throws `UnknownInterviewTemplateException` |

### Web (MockMvc)

| Test | Assertion |
|------|-----------|
| `POST apply-template` on app with empty prep | 302, flash success, detail shows checklist snippet |
| `POST apply-template` when prep exists | 302, flash skip message |
| `GET interviews/new?template=java-backend` | 200, body contains `八股` or section header |
| Unknown template | 302, error flash |

## 8. Implementation Tasks

| Task | Commit message (conventional) |
|------|-------------------------------|
| **Task 14** | `feat(interview): add interview template service and java-backend pack` |
| **Task 15** | `feat(application): add prep checklist template fill on detail page` |
| **Task 16** | `feat(interview): prefill debrief form from template + web tests` |

Task 14 is independently testable (service + JSON). Task 15 adds application UI. Task 16 completes debrief flow + docs.

## 9. Documentation Updates (Task 16)

- `README.md`: Phase 3 subsection under feature list
- `JOURNAL.md`: note Phase 3 completion

## 10. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Template content feels generic | Curate ~15–20 concrete Java backend topics; user edits after fill |
| User expects overwrite | Flash explicitly says 未覆盖; spec and UI subtitle state rule |
| Confusion between blank vs templated debrief | Two links: 新增复盘 vs 新增复盘（带模板） |

## 11. Future (out of scope)

- Additional packs: `frontend-react.json`, `go-backend.json`
- Template picker dropdown on detail page
- Merge template sections into partially filled forms (with confirm dialog)
- Export debrief history as Markdown
- Phase 3b: spaced-repetition review of missed questions
