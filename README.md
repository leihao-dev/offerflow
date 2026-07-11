# OfferFlow

个人求职管理系统：投递管线、阶段跟进、面试复盘、**目标公司档案**。Spring Boot 单体应用，本地运行，数据保存在 H2 文件中。

## 解决了什么问题

- **A1 投递混乱** — 看板 + 阶段筛选 + 跟进日期 + 逾期高亮
- **A2 面试复盘弱** — 每条投递下记录问题、自评、改进项
- **A4 公司信息分散（Phase 2）** — 目标公司档案库：官网 / 招聘页 / 内推信息 / 面经外链，投递可关联复用
- **A3 预留** — 准备清单、JD 原文、岗位级调研笔记字段

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
| `/applications/{id}` | 详情 + 公司档案卡片 + 面试复盘 |
| `/companies` | 目标公司列表 + 行业筛选 |
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

设计背景见 [`docs/superpowers/specs/2026-07-11-offerflow-design.md`](docs/superpowers/specs/2026-07-11-offerflow-design.md)（长期愿景 Phase 5）及 Day 2 `/think` 规划。

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

- Phase 3：面经模板题单（按岗位类型）
- Phase 4：行业 seed 公司包（用户自策展扩展）
- 用户账号 + MySQL 云同步
- 移动端 App
- 邮件/微信提醒逾期跟进

## License

MIT
