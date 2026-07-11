# OfferFlow Phase 4 Design Spec — Company Seed Import & Search

**Date:** 2026-07-12  
**Status:** Draft — pending Ray review  
**Author:** Brainstorming session with Ray  
**Scope:** Java backend internet seed pack (~15–20 companies), idempotent import, company name search

## 1. Problem & Goal

### Problem

After Phase 2 (Company Dossier), users can manually maintain target companies with career URLs and referral info. Pain point **#1** remains partially open: **discovering which companies are worth targeting**, especially beyond a handful of famous names. Manually creating 15–20 company profiles before job hunting is high friction.

### Goal

Provide a **curated, importable seed pack** for **Java / backend roles in internet companies**, plus **name search** on the company library — without scraping, AI, or external APIs.

### Success Criteria

- One JSON seed file ships in `src/main/resources/seeds/java-backend-internet.json` with 15–20 companies, each with verified `careersUrl` where possible.
- User clicks **Import seed** on `/companies`; duplicate names are skipped; flash shows `{imported} imported, {skipped} skipped`.
- Re-importing the same seed is idempotent (0 imported, all skipped if already present).
- `GET /companies?q=字节` returns matching companies; combinable with existing `?industry=` filter.
- `.\gradlew.bat test` passes including seed import and search tests.

### Non-Goals (Phase 4)

- Auto-scraping job boards or company discovery engines
- Updating existing company records on re-import (skip only, never overwrite user-edited data)
- Multiple seed packs in UI (v1: one button for `java-backend-internet` only)
- Full-text search over `companyNotes` / `referralNotes`
- AI-generated company profiles
- Phase 3 interview templates (separate future spec)

## 2. Chosen Approach

**方案 A (approved):** Static JSON in classpath + explicit **Import seed** button on company list.

### Rejected alternatives

| Approach | Reason rejected |
|----------|-----------------|
| B: `DataInitializer` auto-import on boot | User cannot choose when to import; pollutes fresh DB unexpectedly |
| C: CSV/JSON file upload UI | Larger scope; file validation and error UX not needed for v1 |

## 3. Seed Data

### File location

```
src/main/resources/seeds/java-backend-internet.json
```

### JSON schema (array of objects)

| Field | Required | Max length | Notes |
|-------|----------|------------|-------|
| `name` | yes | 200 | Unique key for dedup |
| `industry` | yes | 100 | v1 constant: `"互联网"` |
| `websiteUrl` | no | 500 | `https://` preferred |
| `careersUrl` | recommended | 500 | Primary value for user |
| `referralNotes` | no | text | Generic hint only; no personal codes |

### Example entry

```json
{
  "name": "字节跳动",
  "industry": "互联网",
  "websiteUrl": "https://www.bytedance.com",
  "careersUrl": "https://jobs.bytedance.com",
  "referralNotes": "官网投递 + 员工内推；内推码请在导入后自行填写"
}
```

### v1 company list (18 candidates — URLs verified at implementation time)

字节跳动, 阿里巴巴, 腾讯, 美团, 京东, 百度, 网易, 小米, 华为, 拼多多, 快手, 小红书, 哔哩哔哩, 蚂蚁集团, 滴滴, 携程, Shopee, 米哈游

Ray may remove or replace entries before merge; implementation must not invent URLs — use placeholder or omit `careersUrl` if unverified.

## 4. Architecture

### New components

| Unit | Responsibility |
|------|----------------|
| `dto/CompanySeedEntry.java` | Deserialize JSON row (or record) |
| `dto/SeedImportResult.java` | `imported`, `skipped`, `total` counts |
| `service/CompanySeedService.java` | Load JSON, map to `CompanyForm`, call create/skip logic |
| `CompanyRepository` | Add `findByNameContainingIgnoreCaseOrderByNameAsc` |
| `CompanyService` | Add `search(Optional q, Optional industry)` wrapping repository |
| `CompanyController` | `POST /companies/import-seed`, extend `GET /companies` with `q` |

### Import algorithm

```
for each entry in seed JSON:
  if companyRepository.existsByNameIgnoreCase(entry.name):
    skipped++
  else:
    companyService.create(formFrom(entry))
    imported++
return SeedImportResult(imported, skipped, imported + skipped)
```

**Idempotent:** Re-run never overwrites existing companies (preserves user-added referral codes and notes).

### Search algorithm

```
if q is blank:
  use existing findAll(industry)
else if industry present:
  findByNameContainingIgnoreCaseAndIndustryOrderByNameAsc(q, industry)
else:
  findByNameContainingIgnoreCaseOrderByNameAsc(q)
```

Repository may expose one combined query or filter in service — implementation choice, behavior must match above.

### Request flow

```
GET /companies?q=美团
  → CompanyController.list(q, industry)
    → CompanyService.search(...)
  ← companies/list.html

POST /companies/import-seed?seed=java-backend-internet
  → CompanySeedService.importSeed(seedId)
  ← redirect:/companies + flash counts
```

## 5. UI Changes

### `companies/list.html`

1. **Search bar** above industry filters: text input `q`, preserves `industry` in form/hidden field.
2. **Seed import card** (shown always on v1):
   - Title: 「Java 后端 · 互联网 seed」
   - Subtitle: 「约 18 家公司，含招聘页链接；已存在的公司将跳过」
   - Button: `POST` to `/companies/import-seed?seed=java-backend-internet`

### Flash messages

- Success: `已成功导入 {imported} 家公司，跳过 {skipped} 家（已存在）。`
- Error (unknown seed id): redirect with `errorMessage` via `FlashMessages.ERROR`.

## 6. Error Handling

| Condition | HTTP | Behavior |
|-----------|------|----------|
| Unknown `seed` parameter | 400 or redirect + error flash | Message: seed 包不存在 |
| Invalid JSON in resource | 500 | Log error; friendly `error/500` |
| Empty seed file | 200 | Flash: 0 imported |

No change to delete-company rules (still blocked if applications linked).

## 7. Testing

### Unit / integration

| Test | Assertion |
|------|-----------|
| `importSeed_onEmptyDb` | imported == total, skipped == 0 |
| `importSeed_secondTime` | imported == 0, skipped == total |
| `searchByName_partialMatch` | `q=字节` returns 字节跳动 |
| `searchByName_noMatch` | empty list |

### Web (MockMvc)

| Test | Assertion |
|------|-----------|
| `POST import-seed` | 302, flash contains 「导入」 |
| `GET /companies?q=美团` | 200, body contains 美团 |

## 8. Implementation Tasks

| Task | Commit message (conventional) |
|------|-------------------------------|
| **Task 11** | `feat(company): add seed import service and java-backend seed data` |
| **Task 12** | `feat(company): add company name search on list page` |
| **Task 13** | `feat(company): add seed import action on company list UI` |

Each task independently mergeable; after Task 12 search works without import; after Task 13 full Phase 4 usable.

## 9. Documentation Updates (Task 13)

- `README.md`: Phase 4 subsection under company dossier
- `JOURNAL.md`: note Phase 4 completion when implemented

## 10. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Stale `careersUrl` in seed | Document in README that user should verify links; optional periodic manual JSON update |
| Seed list disagreement | Ray reviews JSON before release; easy to edit one file |
| Import counted as success with 0 new rows | Flash always shows imported/skipped breakdown |

## 11. Future (out of scope)

- Phase 3: interview question templates by role type
- Additional seed packs (`finance-tech.json`) with dropdown seed selector
- `POST /companies/import-seed` accepting custom JSON upload
- Merge/update strategy for existing companies (explicit user opt-in)
