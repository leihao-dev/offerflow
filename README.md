# OfferFlow

个人求职管理系统：投递管线、阶段跟进、面试复盘、**目标公司档案**。Spring Boot 单体应用，本地运行，数据保存在 H2 文件中。

## 解决了什么问题

- **A1 投递混乱** — 看板 + 阶段筛选 + 跟进日期 + 逾期高亮
- **A2 面试复盘弱（Phase 3）** — 带模板新增复盘，预填问题/自评/改进框架
- **A4 公司信息分散（Phase 2）** — 目标公司档案库：官网 / 招聘页 / 内推信息 / 面经外链，投递可关联复用
- **A3 准备无结构（Phase 3）** — 投递详情一键填充 Java 后端准备清单

## 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | [Eclipse Temurin](https://adoptium.net/) 推荐 |
| Gradle | — | 使用 Wrapper，无需单独安装 |

```powershell
java -version
# 应显示 openjdk 17.x
```

## 快速开始

```powershell
git clone https://github.com/leihao-dev/offerflow.git
cd offerflow
.\gradlew.bat bootRun
```

浏览器打开：**http://localhost:8080** → 进入仪表盘

### 常用命令

```powershell
.\gradlew.bat bootRun    # 启动应用
.\gradlew.bat test       # 运行测试
.\gradlew.bat build      # 打包
```

## 主要页面

| 路径 | 功能 |
|------|------|
| `/dashboard` | 进行中 / 本周面试 / 逾期跟进 |
| `/applications` | 投递列表 + 阶段筛选 |
| `/applications/new` | 新增投递（可选公司档案或手动输入） |
| `/applications/{id}` | 详情 + 公司档案 + 准备模板 + 面试复盘 |
| `/companies` | 目标公司列表 + 行业筛选 + 名称搜索 + seed 导入 |
| `/companies/new` | 新增公司档案 |
| `/companies/{id}` | 公司详情（外链、内推、关联投递） |
| `/h2-console` | H2 数据库控制台（开发调试用） |

## Phase 2：目标公司档案（Company Dossier）

Phase 2 在 MVP 投递管线之上增加 **A4 公司情报层**，解决「招聘页要一个个查、内推信息难找」的痛点。

### 功能概览

| 能力 | 说明 |
|------|------|
| **公司档案 CRUD** | 维护公司名、行业、官网、招聘页、内推码/联系人/方式/链接、调研笔记、面经外链 |
| **投递关联** | 新增投递时从下拉选择已有公司，或手动输入公司名（兼容旧数据） |
| **详情页卡片** | 已关联公司的投递详情展示档案摘要，一键打开官网 / 招聘页 / 内推 / 面经 |
| **双向导航** | 投递列表公司名 → 公司详情；公司详情 → 关联投递列表 |

### 推荐使用流程

1. 在 **目标公司** 添加意向公司，填写招聘页 URL 和内推信息
2. **新增投递** 时从下拉选择该公司，只需填岗位与阶段
3. 同一公司投多个岗位时，内推/招聘页信息自动复用

### 相关 commit（Task 8–10）

```
feat(company): add Company domain and service
feat(company): add company library web pages
feat(application): link applications to company dossier
```

设计背景见 [`docs/superpowers/specs/2026-07-11-offerflow-design.md`](docs/superpowers/specs/2026-07-11-offerflow-design.md)（长期愿景 A4 / Phase 5）及 [`docs/superpowers/specs/2026-07-11-offerflow-bugfix-round2-design.md`](docs/superpowers/specs/2026-07-11-offerflow-bugfix-round2-design.md)（Day 2 主路径修复）。

## Phase 4：行业 seed 公司包 + 搜索

Phase 4 在 Phase 2 公司档案库之上，解决「从零录入 15–20 家目标公司太费事」的痛点。

### 功能概览

| 能力 | 说明 |
|------|------|
| **Java 后端互联网 seed** | `src/main/resources/seeds/java-backend-internet.json`，约 18 家公司，含招聘页链接 |
| **一键导入** | 目标公司列表页点击「导入 seed」；已存在公司按名称跳过，不覆盖用户编辑 |
| **名称搜索** | `GET /companies?q=字节` 模糊匹配公司名，可与 `?industry=` 组合 |

### 推荐使用流程

1. 打开 **目标公司**，点击 **导入 seed** 快速填充互联网大厂档案
2. 按需编辑内推码、调研笔记等个人字段
3. 用搜索框或行业筛选定位公司，再 **新增投递** 时从下拉关联

### 相关 commit（Task 11–13）

```
feat(company): add seed import service and java-backend seed data
feat(company): add company name search on list page
feat(company): add seed import action on company list UI
```

设计背景见 [`docs/superpowers/specs/2026-07-12-company-seed-design.md`](docs/superpowers/specs/2026-07-12-company-seed-design.md)。

## Phase 3：面经 / 准备模板

Phase 3 补全 **A2 + A3**：面试前有结构化的准备清单，面试后有复盘框架可填。

### 功能概览

| 能力 | 说明 |
|------|------|
| **准备清单模板** | 投递详情「填充准备清单」；仅空清单时写入 |
| **复盘框架** | 「新增复盘（带模板）」预填轮次、问题、自评、改进章节 |

### 推荐使用流程

1. 创建投递 → 详情页点击 **填充准备清单**
2. 面试后点击 **新增复盘（带模板）** → 在预填框架上补充实际内容
3. 保存复盘 → 详情页查看历史记录

### 相关 commit（Task 14–16）

```
feat(interview): add interview template service and java-backend pack
feat(application): add prep checklist template fill on detail page
feat(interview): prefill debrief form from template + web tests
```

设计背景见 [`docs/superpowers/specs/2026-07-12-interview-template-design.md`](docs/superpowers/specs/2026-07-12-interview-template-design.md)。

## 数据存储

H2 文件数据库位于 `./data/offerflow.*`。删除 `data/` 目录可重置全部数据。

## 项目结构

```
offerflow/
├── build.gradle              # Gradle 构建
├── src/main/java/            # Java 源码
│   └── com/offerflow/
│       ├── controller/       # Web 路由
│       ├── service/          # 业务逻辑
│       ├── model/            # JPA 实体（JobApplication、Company、InterviewNote）
│       └── repository/       # 数据访问
├── src/main/resources/
│   ├── templates/            # Thymeleaf 页面
│   └── static/css/           # 样式
├── src/test/java/            # 单元测试
├── .claude/                  # CC 技能与 hooks
├── docs/superpowers/         # 设计规格与实现计划
├── README.md
└── JOURNAL.md                # CC 周挑战开发日记
```

## CC 配置（`.claude/`）

| 文件 | 作用 |
|------|------|
| `skills/offerflow-coach/SKILL.md` | 引导投递录入与面试复盘 |
| `hooks/hooks.json` | 会话开始时提醒记录 |

## CC 周挑战

本项目为 CC 一周挑战作业，使用 **Cursor + Superpowers + Waza** 开发。开发过程见 [`JOURNAL.md`](JOURNAL.md)。

## 技术栈

Java 17 · Spring Boot 3.3 · Thymeleaf · Spring Data JPA · H2 · Gradle

## 后续规划

- 更多面试模板包（前端、Go 等）
- 更多 seed 包（金融、外企等）+ 导入时选择 seed
- 用户账号 + MySQL 云同步
- 移动端 App
- 邮件/微信提醒逾期跟进

## License

MIT
