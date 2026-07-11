# OfferFlow

个人求职管理系统：投递管线、阶段跟进、面试复盘。Spring Boot 单体应用，本地运行，数据保存在 H2 文件中。

## 解决了什么问题

- **A1 投递混乱** — 看板 + 阶段筛选 + 跟进日期
- **A2 面试复盘弱** — 每条投递下记录问题、自评、改进项
- **A3/A4 预留** — 准备清单、JD 原文、公司笔记字段（后续扩展）

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
| `/applications/new` | 新增投递 |
| `/applications/{id}` | 详情 + 面试复盘 |
| `/h2-console` | H2 数据库控制台（开发调试用） |

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
│       ├── model/            # JPA 实体
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

- 用户账号 + MySQL 云同步
- 移动端 App
- 面经题库 + 技能差距分析
- Freemium 会员

## License

MIT
