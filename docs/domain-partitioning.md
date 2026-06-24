# governance 领域分包设计（建标 / 建模 / 建指标）

> 所属：数据治理分系统（governance）· 设计草案
> 上游依据：主仓 `docs/architecture/01-架构总览.md` §2（五大分系统）、`docs/architecture/03-技术选型.md`（AD-16 自研元模型引擎）、本仓 `README.md`（职责与边界）。
> 范围：把数据中台「数据架构」业务能力（建标 / 建模 / 建指标）在 governance 内部做 bounded context 分包，**不新增子模块**。

---

## 1. 背景与定位

- 平台为**五大分系统**，governance = 数据治理分系统，owns：元数据 / **元模型引擎** / 专题-主题-实体三层模型 / 数据标准 / 数据质量 / 血缘。
- 前端「数据架构」（数仓设计 / 数据标准 / 数据建模 / DWS·ADS / 数据指标）是 **UI 信息架构分组**，其后端能力**全部落在 governance**（前端整体在 `webui`）。数据指标（原子/派生/复合）亦归 governance（口径唯一治理，非 BI 渲染）。
- 一切建模动作都坐在 **AD-16 自研元模型引擎**（Apache Atlas TypeDef 体系为蓝本：entityDef+继承 / classificationDef / relationshipDef 带基数 / 草稿发布 / 版本 / 平台公共+租户私有作用域）之上。

---

## 2. 分包风格：layer-first（本期）+ 层内按 bounded context 细分

沿用 M1 既有约定（`domain/metadata` 已是「层优先 + 层内按特性」），**本期不切换为 feature-first**，避免改动已落地的 M1 包结构与测试。

```
io.hashmatrix.governance
├── api/      <context>/      REST 控制器、DTO、统一返回（starter-web）
├── app/      <context>/      用例编排、事务、审计（starter-audit）、跨 context 协调
├── domain/
│   ├── <context>/            该 context 的聚合根 / 值对象 / 领域服务（现有 metadata、metamodel）
│   ├── port/                 全部出站端口(driven port)——扁平共享，命名 <X>Port / <X>Repository
│   └── （TenantSchema 等跨 context 共享 VO 暂置 domain 根，后续可收敛入 domain/shared）
└── infra/    <context>/      端口实现（PG JSONB / ES），如 JdbcEntityDefRepository
```

> **实现现状对齐**：M1 与 metamodel ST-1 均采用「领域 VO 按 context 分包（`domain/metadata`、`domain/metamodel`）+ **出站端口统一放共享 `domain/port/`**」（现有 `MetadataCatalogPort` / `InfraConnectivityPort` / `EntityDefRepository`）。本文档据此对齐——**端口不按 context 下沉**。

**依赖规则**

- 层间：`api → app → domain ← infra`，内层不依赖外层。
- context 间：依赖只允许发生在 **domain 层**，且必须**无环**（见 §4），交互**只经共享 `domain/port/` 的端口接口 + 不可变值对象**，不得直接引用对方聚合根 / infra / 表。
- 跨 context 用例编排放在 `app/<顶层 context>`（如落标拦截、状态机流转放 `app/modeling`），不下沉 domain。

> **后续优化（已记录，暂不实施）**：
> - 当 context 数量与体量增长、layer-first 出现「同一 context 代码散落四层」的导航成本时，迁移到 **feature-first**（顶层即 context，layers 内嵌）。迁移需同步调整既有包与测试，故留待引擎核心（Epic#1）稳定后专项重构。
> - 同批评估：把出站端口由共享 `domain/port/` 下沉到各 context（`domain/<context>/port/`）、以及 `catalog` 包由 `domain/metadata` 改名归一。本期为对齐实现一律不动。

---

## 3. Bounded Context 划分与前端映射

| context 包名 | 职责 | 对应「数据架构」页面 | 本期 |
|---|---|---|---|
| `metamodel`（基座） | 元模型引擎：typedef+继承、关系基数、分类树、草稿发布、版本、平台公共+租户私有作用域 | （引擎，无直接页面） | **进行中（ST-1）**：`domain/metamodel`（EntityDef/AttributeDef/DataType/Scope/…）+ `domain/port/EntityDefRepository` + Flyway per-tenant 迁移 |
| `catalog` | 元数据**实例**（遵循元模型）、资产搜索、Facet | （现 `domain/metadata`，context 名 catalog，包名暂留） | 已有（M1） |
| `lineage` | 血缘登记 / 查询 / 漂移标记 | 维度建模「同步元数据」对账血缘 | 随建模 |
| `warehouse`（建标地基·**独立**） | **专题-主题-实体三层模型**：数仓分层、数据域/子域、业务过程、数据集市 | 数仓设计（全部） | ✅ |
| `standard`（建标） | 标准字典、落标校验、熔断大盘；安全等级 G1–G4 | 数据标准（全部） | ✅ |
| `metric`（建指标） | 原子 / 派生 / 复合指标、修饰词、时间周期 | 数据指标（全部） | ✅ |
| `modeling`（建模·顶层消费方） | 建模前置网关、关系/维度设计器、DWS/ADS、模型管理双轨制 + 状态机 | 数据建模 + DWS/ADS | ✅ |

> `warehouse` 独立成 context（不并入 `standard`）：它就是架构定义的**专题-主题-实体三层模型**，概念重、被建模网关强依赖，独立利于复用与演进。

### 本期不纳入（已记录依赖，后续接入）

| context | 不纳入原因 | 接入前需先稳的上游依赖 |
|---|---|---|
| `quality`（数据质量 / DQC） | 入站耦合面大，依赖多个尚未落地的业务 context | ① **standard**：落标率/质量分低于阈值触发熔断（落标大盘联动）<br>② **modeling**：熔断 → ADS DQC **强阻断**挂起服务<br>③ **catalog**：质量规则作用对象 = 元数据资产<br>④ **metamodel**：质量规则本身亦为元模型实例 |

> 待 ①②③④ 稳定后，新增 `quality` context（DQC 规则、质量分、监控熔断大盘），与 `standard` 熔断、`modeling` 的 ADS DQC SLA 联动。

---

## 4. Context 依赖图（无环）

```
                 metamodel  (基座，被所有 context 依赖)
                     ▲
   ┌─────────┬───────┼───────────┬──────────┐
catalog   warehouse  standard   metric   (lineage)
            ▲          ▲          ▲
            └──────────┴────┬─────┘
                         modeling   (顶层：网关继承分层/域/集市、字段落标、引用原子指标)

  (后续) quality ── 入站依赖 ──▶ standard · modeling · catalog · metamodel
```

依据各子模块 PRD「关联关系」：分层/域/集市 → 建模网关；字典 → 落标校验；原子指标 → 维度建模引用度量 & DWS 拖拽；落标率 → 熔断 → ADS DQC。**`modeling` 单向依赖其它，绝不反向**。

---

## 5. 落地节奏（与 Epic#1 对齐）

1. **沿用现有布局**（对齐实现，不做 metadata→catalog 改名、不动端口位置）：领域 VO 按 context 分包（`domain/metadata`、`domain/metamodel`）+ 出站端口共享 `domain/port/`。新 context 落地时按此约定加包即可。
2. `metamodel` 基座随 Epic#1（13 sub-issue）推进——**ST-1（EntityDef 持久化底座）进行中**（工作区已有 typedef record + `EntityDefRepository` + Flyway per-tenant 迁移）。
3. 建标三件套按依赖底→上：`warehouse → standard → metric`，各自可独立交付。
4. `modeling` 最后，集成上述能力（建模网关、双轨制、状态机）。
5. 引擎稳定后，评估 feature-first 重构 + 端口下沉 + catalog 改名（§2 后续优化）与 `quality` 接入（§3）。

---

## 6. 决策记录（ADR 摘要）

| 编号 | 决策 | 状态 |
|---|---|---|
| DP-1 | 数据架构（建标/建模/建指标，含数据指标）后端归 governance，不拆子模块 | 已定 |
| DP-2 | 本期维持 layer-first + 层内 context 细分；feature-first 列为后续优化 | 已定 |
| DP-3 | `warehouse`（专题-主题-实体三层模型）独立成 context | 已定 |
| DP-4 | `quality` 本期不纳入，依赖 standard/modeling/catalog/metamodel 稳定后接入 | 已定 |
| DP-5 | 出站端口统一置于共享 `domain/port/`（六边形 driven port），不按 context 下沉；与 M1 及 metamodel ST-1 实现一致 | 已定（对齐实现） |

> 示例数据约定：本文及后续代码示例一律使用虚构脱敏占位（如 `example.com`、`tenant-demo`、`dwd_order_detail`），不含任何真实甲方信息。
