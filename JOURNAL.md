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

### Day 2 — 2026-07-12

- **做了什么：**
  - 完成 **4 轮 Big Step 打磨**（UI 高亮 / Flash 与 404 / 逾期逻辑与日期校验 / 公共导航与移动端菜单），每轮单独 commit
  - **手动走查**全部主流程（仪表盘 → 列表 → 新增 → 详情 → 面试复盘），记录 4 个实际 bug
  - 用 Superpowers **brainstorming** 做代码复盘：定位根因（Thymeleaf `stageLabels[s]` 枚举 Map 查找失败；详情页 `application` 与 Thymeleaf 保留名冲突）
  - 编写并提交 `docs/superpowers/specs/2026-07-11-offerflow-bugfix-round2-design.md`，按方案 A 修复
  - 模板统一改用 `StageLabels.label()`；详情 model 改名为 `jobApplication`；新增 `error/500.html` 与 Web 冒烟测试
- **卡在哪：**
  - 功能「看起来写完」但一点就 Whitelabel，测试却全绿——**只测了 404 异常路径，没测主路径**
  - 阶段筛选按钮、下拉框文字空白，第一眼以为是 CSS 问题，其实是模板表达式
  - 详情页 500 报错不明显，需翻终端日志才能看到 `application` 保留名冲突
- **怎么解开的：**
  - 先手动复现、再 brainstorming 复盘列 bug 清单，确认范围（P0+P1）后再动手，避免盲目改
  - 用 `StageLabels.label(s)` 绕过 Map 枚举键坑；Controller 传 `jobApplication` 避开保留名
  - 在 `GlobalExceptionHandlerWebTest` 补 3 条冒烟测试（列表标签 / 表单选项 / 创建→详情），`.\gradlew.bat test` 全过后再提交
  - 用 Waza **`/think`** 规划 **Phase 2（公司档案库 A4）**：独立 `Company` 实体 + 公司库 Web + 投递关联（Task 8–10）
  - 实现目标公司档案：官网/招聘页/内推信息/面经外链；投递表单可选公司或手动输入；详情页展示档案卡片

---

## 认知变化：Day 1 vs Day 2

| 维度 | Day 1 | Day 2 |
|------|-------|-------|
| 对 CC 的定位 | 代码生成加速器，问一句写一段 | 先 spec/复盘再写码；AI 适合拆 Task，人工负责走查主路径 |
| 对 Superpowers 的感受 | brainstorming 流程重，但少返工 | 第二次 brainstorming（bugfix + Phase 2）同样避免盲目改 |
| 实际工作流 | spec → plan → 分 Task 提交 | MVP → 四轮打磨 → 手动测 bug → spec 修复 → Phase 2 三 Task |

**具体例子 1：** bugfix 轮若直接让 AI「修 Whitelabel」，可能只改 URL；复盘后发现是 Thymeleaf 枚举 Map + 保留名两个根因，一轮 spec 全解决。

**具体例子 2：** Phase 2 用 `/think` 拒绝了「自动爬公司/AI 面经」，改为用户自策展的公司档案库——2 天可交付且解决真实痛点（外链 + 内推）。

---

## 原来还能这样（≥2 件）

1. **Superpowers brainstorming** 强制先澄清需求再写代码，OfferFlow 从「作业检查器」改成了真正有长期使用价值的求职 CRM
2. **分 Task + 规范 commit**（`feat(scope): subject`）让 7 天 git 历史清晰可读，面试 Demo 时可以按 commit 讲演进
3. **手动测试 + brainstorming 复盘** 比直接让 AI「修一下」更有效——先列清 4 个 bug 和根因，再写 spec，一轮 commit 就收敛
4. **Phase 2 公司档案** 同一公司投多岗时，内推/招聘页只维护一次，投递详情一键外链

---

## 这玩意还不行（≥1 件）

- **场景：** （待填写 — 建议记录一次 AI 理解错误、Superpowers 管太多、或 Cursor 上下文丢失）
- **Prompt 片段 / 截图：** （待粘贴）
- **期望 vs 实际：** （待填写）

---

## 作业收尾（待完成）

| 事项 | 状态 |
|------|------|
| 推送远程 `origin/master` | 进行中 |
| 录 3–5 分钟 Demo 视频 | 待做 |
| 全站手动 QA | 待做 |
| 按作业要求发邮件 | 待做 |

## 后续产品方向（Phase 3+）

- 面经模板题单（按岗位类型，非 AI 生成）
- 行业 seed 公司包（用户自策展的扩展）
- 投递导出 Markdown、仪表盘本周面试列表

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

- **完整走过 brainstorming：** 是（Day 1 MVP、Day 2 bugfix、Phase 2 规划）
- **是否绕开某些流程：** 小 bug 修复时用过 Lightweight Mode；大功能仍走 spec
- **绕开原因：** 单行模板修复不需要完整 brainstorming，但涉及 3+ 文件仍先列清单
