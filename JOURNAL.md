# JOURNAL

> OfferFlow · CC 周挑战开发日记  
> 仓库：https://github.com/leihao-dev/offerflow  
> 分支：`master`

---

## Day-by-day

### Day 1 — 2026-07-11

- **做了什么：**
  - 安装 Superpowers 插件与 Waza 技能（`/think`、`/check` 等）
  - 用 Superpowers **brainstorming** + Waza **`/think`** 确定方向：OfferFlow 求职管理系统
  - 编写设计规格 `docs/superpowers/specs/2026-07-11-offerflow-design.md`
  - 按 7 个 Task 增量实现：Gradle 脚手架 → 实体 → Service → Web → 面试复盘 → 仪表盘
  - 配置 JDK 17（Temurin），Gradle 改用腾讯云镜像加速下载
- **卡在哪：**
  - 本机 PATH 最初没有 Java，`gradlew bootRun` 报 `JAVA_HOME is not set`
  - Gradle 官方源下载超时（10s timeout）
  - 代码先提交到 `main` 分支，与远程 `master` 不一致
- **怎么解开的：**
  - `winget` 确认 Temurin 17 已安装，手动配置 `JAVA_HOME` 和用户级 `Path`
  - 修改 `gradle-wrapper.properties`：腾讯云镜像 + `networkTimeout=120000`
  - 将 `main` 快进合并到 `master`，删除本地 `main`，推送到 `origin/master`

### Day 2 — （待填写）

- **做了什么：**
- **卡在哪：**
- **怎么解开的：**

### Day 3 — （待填写）

### Day 4 — （待填写）

### Day 5 — （待填写）

### Day 6 — （待填写）

### Day 7 — （待填写）

---

## 认知变化：Day 1 vs Day 7

| 维度 | Day 1 | Day 7 |
|------|-------|-------|
| 对 CC 的定位 | 代码生成加速器，问一句写一段 | （待填写） |
| 对 Superpowers 的感受 | brainstorming 流程重，但少返工；`/think` 适合定方向 | （待填写） |
| 实际工作流 | spec → plan → 分 Task 提交 → 每 Task 一个 commit | （待填写） |

**具体例子 1：** （待填写 — 建议写一次 Superpowers brainstorming 如何改变了你的方案）

**具体例子 2：** （待填写 — 建议写 Waza `/think` vs 直接让 AI 写代码的差异）

---

## 原来还能这样（≥2 件）

1. **Superpowers brainstorming** 强制先澄清需求再写代码，OfferFlow 从「作业检查器」改成了真正有长期使用价值的求职 CRM
2. **分 Task + 规范 commit**（`feat(scope): subject`）让 7 天 git 历史清晰可读，面试 Demo 时可以按 commit 讲演进
3. （待填写）

---

## 这玩意还不行（≥1 件）

- **场景：** （待填写 — 建议记录一次 AI 理解错误、Superpowers 管太多、或 Cursor 上下文丢失）
- **Prompt 片段 / 截图：** （待粘贴）
- **期望 vs 实际：** （待填写）

---

## 再给一周会怎么改

1. 加 Spring Security 登录 + 多用户隔离
2. MySQL 替代 H2，支持云部署
3. REST API + 移动端（Flutter / RN）
4. 面经全文搜索 + 间隔重复复习
5. 邮件/微信提醒逾期跟进

---

## 换个角色思考（可选）

假设我是**产品同事**，从没碰过 CC，要用 OfferFlow 管理自己的求职：

我会配：

| 组件 | 配置 | 原因 |
|------|------|------|
| **skill** `offerflow-coach` | 投递后引导填公司/阶段/跟进日；面试后 24h 内填复盘 | 产品同事不需要懂 Spring，只需要**低摩擦录入** |
| **hook** `sessionStart` | 打开 CC 时提醒「今天有没有新投递或面试要记」 | 防止忙起来就忘记录 |
| **不做** | 不让同事接触 `.java` 或 Gradle | 技术细节是研发的事，产品只关心「我有没有漏跟进」 |

如果同事连 localhost 都不想开，下一步会把 OfferFlow 部署成内网 Web 服务，而不是让他装 JDK。

---

## Superpowers 体验笔记

- **完整走过 brainstorming：** 是（Day 1）
- **是否绕开某些流程：** （待填写 — 如小改用 Lightweight Mode 而非 TDD）
- **绕开原因：** （待填写）
