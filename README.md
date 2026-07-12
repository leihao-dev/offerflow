# OfferFlow

个人求职管理系统：投递管线、阶段跟进、面试复盘、目标公司档案。Spring Boot 单体应用，本地运行，数据保存在 H2 文件中。飞书风侧栏布局 + 响应式 UI（Phase 7）。

**技术栈：** Java 17 · Spring Boot 3.3.5 · Thymeleaf · Spring Data JPA · H2 · Gradle

---

## 解决什么问题

| 痛点 | 能力 |
|------|------|
| **A1 投递混乱** | 仪表盘、阶段筛选、跟进日期、逾期高亮、本周面试列表、投递搜索、单条/批量 Markdown 导出 |
| **A2 面试复盘弱** | 带模板新增复盘（Java / 前端 React / Go）、**复盘记录**页浏览/搜索、详情锚点跳转 |
| **A3 准备无结构** | 投递详情按模板填充准备清单（仅空清单时写入）；**模板预览**后再填充 |
| **A4 公司信息分散** | 目标公司档案库、3 套 seed 导入与**预览**、名称/行业搜索、投递关联、手动公司名关联提示 |

---

## 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | [Eclipse Temurin](https://adoptium.net/) 推荐 |
| Gradle | — | 使用 Wrapper，无需单独安装 |

### 1. 配置 Java（Windows）

若 `.\gradlew.bat bootRun` 报 `JAVA_HOME is not set`：

```powershell
# 确认已安装
java -version   # 应显示 openjdk 17.x

# 当前会话（路径按本机 JDK 安装位置调整）
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### 2. 克隆并启动

```powershell
git clone https://github.com/leihao-dev/offerflow.git
cd offerflow
.\gradlew.bat bootRun
```

浏览器打开 **http://localhost:8080** → 自动进入仪表盘。

### 3. 常用命令

```powershell
.\gradlew.bat bootRun    # 启动应用
.\gradlew.bat test       # 运行测试（18 个测试类）
.\gradlew.bat build      # 打包
```

### 常见问题

| 现象 | 处理 |
|------|------|
| `JAVA_HOME is not set` | 见上文 §1 |
| Gradle 下载超时 | 项目已配置腾讯云镜像与 `networkTimeout=120000`（`gradle/wrapper/gradle-wrapper.properties`） |
| 想清空数据重来 | 停止应用后删除 `./data/` 目录 |
| `Database may be already in use` | 已有 `bootRun` 在跑；关掉旧进程或只保留一个实例 |

---

## 推荐使用流程

1. **导入公司 seed** — `/companies` → 选包 → **预览 seed** → 确认后导入
2. **新建投递** — `/applications/new` → 从下拉关联公司 → 填写岗位、阶段、跟进日
3. **面试前** — 投递详情 → **预览**面试模板 → 填充准备清单
4. **面试后** — 详情页「+ 复盘（带模板）」→ 在预填框架上补充实际内容
5. **日常查看** — 仪表盘可点统计卡跳转；列表 `?q=` / `?overdue=1` 筛选；**复盘记录**浏览最近 50 条或 `?q=` 搜索；单条或 **zip 批量** 导出 Markdown

---

## 界面与体验（Phase 7）

- **布局**：桌面左侧边栏导航；窄屏（≤768px）汉堡菜单 + 抽屉
- **设计系统**：飞书风色板（`tokens.css` / `layout.css` / `components.css`），阶段色点徽章
- **仪表盘**：可点击统计卡（进行中 / 本周面试 / 逾期 → 列表或 `?overdue=1`）
- **投递列表**：工具栏搜索 + 阶段 chips + 逾期筛选；sticky 表头、行 hover

### 复盘记录（Phase 7b）

- **侧栏「复盘记录」** — `/interviews/search`（URL 不变）
- **默认浏览** — 最近 50 条复盘表格，与投递列表同款操作（详情 / 编辑）
- **搜索** — `?q=` 跨全部复盘全文过滤
- **详情锚点** — 详情链至 `/applications/{id}#note-{noteId}`，投递详情页高亮对应卡片

---

## 功能一览

### 投递管线

- 投递 CRUD、阶段快速更新、跟进日期与逾期高亮
- 列表按阶段筛选（`?stage=`）、逾期筛选（`?overdue=1`）与公司名/岗位搜索（`?q=`），可组合使用
- 投递详情支持编辑、删除（级联删除关联复盘）

### 仪表盘

- 进行中投递数、本周面试数、逾期未跟进数（统计卡可点击跳转）
- **本周面试列表**：展示本周内有面试记录的投递，可一键跳转详情
- **待跟进列表**：逾期投递高亮，阶段色点 + 快速进入详情

### 面试准备与复盘

- **准备清单**：详情页下拉选模板 → POST 填充（已有内容时不覆盖）；可先 **预览模板** 查看 prep / 复盘框架
- **复盘**：新增/编辑/删除面试记录；支持 `?template=` 预填复盘框架
- **复盘记录**：`/interviews/search` 默认最近 50 条；`?q=` 搜索全部；详情 / 编辑 / `#note-{id}` 锚点
- 三套模板：Java 后端、前端 React、Go 后端

### 目标公司档案

- 公司 CRUD：官网、招聘页、内推信息、调研笔记、面经外链
- 投递表单可下拉关联公司，或手动输入公司名（兼容旧数据）
- 已关联公司的投递详情展示档案卡片与外链
- 手动输入的公司名若与已有档案重名，保存成功并 flash 提示建议从下拉关联
- **seed 预览**：`/companies?previewSeed=` 导入前查看 pack 内前 5 家公司

### 数据导出

- `GET /applications/{id}/export` 下载单条投递的 Markdown（含 JD、准备清单、全部复盘）
- `GET /applications/export-all` 下载全部投递的 zip（每条一个 `.md`，上限 500 条）

---

## 页面与 API 速查

### 页面路由

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 重定向到仪表盘 |
| GET | `/dashboard` | 仪表盘 |
| GET | `/applications` | 投递列表（`?stage=` `?q=` `?overdue=1`） |
| GET | `/applications/new` | 新建投递表单 |
| POST | `/applications` | 保存新投递 |
| GET | `/applications/{id}` | 投递详情 |
| GET | `/applications/{id}/edit` | 编辑投递 |
| POST | `/applications/{id}` | 更新投递 |
| POST | `/applications/{id}/stage` | 快速更新阶段 |
| POST | `/applications/{id}/delete` | 删除投递 |
| GET | `/applications/export-all` | 下载全部 Markdown（zip） |
| GET | `/applications/{id}/export` | 下载单条 Markdown |
| POST | `/applications/{id}/apply-template` | 填充准备清单（表单字段 `template`） |
| GET | `/applications/{id}/preview-template` | 预览面试模板（`?template=`） |
| GET | `/applications/{id}/interviews/new` | 新增复盘（`?template=` 可选） |
| POST | `/applications/{id}/interviews` | 保存复盘 |
| GET | `/interviews/search` | 复盘记录（默认最近 50 条，`?q=` 搜索全部） |
| GET | `/interviews/{id}/edit` | 编辑复盘 |
| POST | `/interviews/{id}` | 更新复盘 |
| POST | `/interviews/{id}/delete` | 删除复盘 |
| GET | `/companies` | 公司列表（`?q=` `?industry=` `?previewSeed=`） |
| POST | `/companies/import-seed` | 导入 seed（表单字段 `seed`） |
| GET | `/companies/new` | 新建公司 |
| POST | `/companies` | 保存公司 |
| GET | `/companies/{id}` | 公司详情 |
| GET | `/companies/{id}/edit` | 编辑公司 |
| POST | `/companies/{id}` | 更新公司 |
| POST | `/companies/{id}/delete` | 删除公司（有关联投递时拒绝） |
| GET | `/h2-console` | H2 控制台（开发调试） |

### 投递阶段

| 枚举值 | 显示名 |
|--------|--------|
| `APPLIED` | 已投递 |
| `SCREENING` | 简历筛选 |
| `TECH_INTERVIEW` | 技术面试 |
| `FINAL_INTERVIEW` | 终面 |
| `OFFER` | Offer |
| `REJECTED` | 已拒绝 |
| `WITHDRAWN` | 已撤回 |

---

## Seed 与面试模板包

### 公司 seed

| Pack ID | 标题 | 文件 | 约数量 |
|---------|------|------|--------|
| `java-backend-internet` | Java 后端 · 互联网 | `seeds/java-backend-internet.json` | 18 |
| `finance-tech` | 金融 / 金融科技 | `seeds/finance-tech.json` | 10 |
| `foreign-tech` | 外企科技 | `seeds/foreign-tech.json` | 10 |

导入规则：已存在同名公司跳过，**不覆盖**用户已编辑字段。列表页可先 **预览 seed** 再导入。

### 面试模板

| Pack ID | 文件 |
|---------|------|
| `java-backend` | `seeds/java-backend-interview.json` |
| `frontend-react` | `seeds/frontend-react-interview.json` |
| `go-backend` | `seeds/go-backend-interview.json` |

### 如何新增一包（扩展）

无需改核心 CRUD，只需 JSON + 注册表：

1. 在 `src/main/resources/seeds/` 添加 JSON 文件
2. **公司 seed：** 在 `CompanySeedService` 的 `SEED_RESOURCES`、`SEED_TITLES`、`SEED_ORDER` 各加一行
3. **面试模板：** 在 `InterviewTemplateService` 的 `TEMPLATE_RESOURCES`、`TEMPLATE_ORDER` 各加一行
4. 运行 `.\gradlew.bat test`，列表/详情页下拉自动出现新包

---

## 数据存储

- H2 文件库：`jdbc:h2:file:./data/offerflow`（配置见 `application.yml`）
- 删除 `./data/` 可重置全部数据
- H2 控制台：http://localhost:8080/h2-console（JDBC URL 与 `application.yml` 一致）

---

## 项目结构

```
offerflow/
├── build.gradle
├── src/main/java/com/offerflow/
│   ├── controller/       # Web 路由
│   ├── service/          # 业务逻辑（含 Seed / Template / Export）
│   ├── model/            # JPA 实体：JobApplication、Company、InterviewNote
│   ├── repository/       # 数据访问
│   ├── dto/              # 表单与 seed/template 传输对象
│   ├── web/              # StageLabels、StageStyles、FlashMessages、异常处理
│   └── support/          # FollowUpRules、ExportLimits、DebriefLimits 等
├── src/main/resources/
│   ├── application.yml
│   ├── templates/        # Thymeleaf 页面与 layout/sidebar fragments
│   ├── seeds/            # 公司 seed + 面试模板 JSON
│   └── static/
│       ├── css/          # tokens / layout / components
│       └── js/           # app-shell.js（移动端侧栏）
├── src/test/java/        # 单元与 Web 冒烟测试
├── .claude/              # CC 技能与 hooks
├── docs/superpowers/     # 设计规格与实现计划
├── README.md
└── JOURNAL.md            # CC 周挑战开发日记
```

---

## CC 周挑战

本项目为 CC 一周挑战作业，使用 **Cursor + Superpowers + Waza** 开发。

| 产物 | 路径 |
|------|------|
| 开发日记 | [`JOURNAL.md`](JOURNAL.md) |
| 投递教练 skill | `.claude/skills/offerflow-coach/SKILL.md` |
| 会话提醒 hook | `.claude/hooks/hooks.json` |

工作流：brainstorming 写 spec → writing-plans 拆 Task → 增量 commit。详细过程与反思见 JOURNAL。

### 设计文档索引

| 文档 | 内容 |
|------|------|
| [`2026-07-11-offerflow-design.md`](docs/superpowers/specs/2026-07-11-offerflow-design.md) | MVP 与长期愿景 |
| [`2026-07-11-offerflow-bugfix-round2-design.md`](docs/superpowers/specs/2026-07-11-offerflow-bugfix-round2-design.md) | Day 2 主路径 bug 修复 |
| [`2026-07-12-company-seed-design.md`](docs/superpowers/specs/2026-07-12-company-seed-design.md) | Phase 4 公司 seed |
| [`2026-07-12-interview-template-design.md`](docs/superpowers/specs/2026-07-12-interview-template-design.md) | Phase 3 面试模板 |
| [`2026-07-12-phase5-polish-expansion-design.md`](docs/superpowers/specs/2026-07-12-phase5-polish-expansion-design.md) | Phase 5 打磨与扩展包 |
| [`2026-07-12-phase6-knowledge-portability-design.md`](docs/superpowers/specs/2026-07-12-phase6-knowledge-portability-design.md) | Phase 6 知识库与便携性 |
| [`2026-07-12-phase7-ux-enhancement-design.md`](docs/superpowers/specs/2026-07-12-phase7-ux-enhancement-design.md) | Phase 7 UX 增强（飞书风侧栏） |
| [`2026-07-12-phase7b-debrief-hub-design.md`](docs/superpowers/specs/2026-07-12-phase7b-debrief-hub-design.md) | Phase 7b 复盘 hub 与技术债 |

实现计划见 [`docs/superpowers/plans/`](docs/superpowers/plans/)。

---

## 演进历史

| 阶段 | 范围 | 关键交付 |
|------|------|----------|
| MVP + 四轮打磨 | A1/A2 核心 | 投递 CRUD、复盘、仪表盘、UI/异常修复 |
| Phase 2 | A4 | 公司档案库、投递关联、详情卡片 |
| Phase 3 | A2/A3 | 面试准备清单与复盘模板（Java 后端首包） |
| Phase 4 | A4 | 互联网 seed、公司搜索、seed 导入 UI |
| Phase 5 | A1 + 扩展 | 投递搜索、本周面试、Markdown 导出、3 seed + 3 模板下拉、公司名提示 |
| Phase 6 | A2 + 便携 + 发现性 | 复盘全文搜索、批量 zip 导出、seed/模板预览 |
| Phase 7 | UX | 飞书风设计系统、侧栏布局、仪表盘/列表改版、`?overdue=1` |
| Phase 7b | A2 + 债务 | 复盘记录 hub、详情锚点、侧栏改名 |

完整 commit 历史：`git log --oneline`。

---

## 后续规划

- 看板视图 / 拖拽改阶段（Phase 8）
- 间隔重复复习（面经/错题）
- JSON 备份导入恢复
- 用户账号 + MySQL 云同步
- 移动端 App
- 邮件/微信提醒逾期跟进

