# MORI COFFEE · 经营数据台

一个前后端分离的咖啡门店经营数据看板：**Nuxt 4 前端 + Spring Boot 3 后端 + SQLite 数据库**。

![经营数据台预览](docs/preview.png)

## 架构

```text
浏览器
  │
  ▼
Nuxt 4 前端  http://127.0.0.1:3000
  │   REST / JSON（CORS）
  ▼
Spring Boot 3 后端  http://127.0.0.1:8080/api
  │   Spring JDBC
  ▼
SQLite  backend/data/mori-coffee.db
```

前后端分别独立启动、独立构建。后端只负责数据聚合与 CSV 导出，图表由前端使用 SVG 绘制。

### 后端三层架构

```text
Controller 层   接收 HTTP 请求、绑定参数、返回 Result<VO> / 文件流
     │          （只依赖 Service 接口，不含业务逻辑、不接触 DAO）
     ▼
Service 层      参数校验、默认区间、环比计算、补零、占比、CSV 生成、演示数据生成
     │          （接口 + impl 实现；只依赖 DAO 接口，抛出 BusinessException）
     ▼
DAO 层          只执行 SQL，返回 Entity / 统计 DTO
                （接口 + JdbcTemplate 实现，不含业务规则）
```

| 对象 | 包 | 用途 |
| --- | --- | --- |
| Entity | `entity` | 与数据表一一对应：`Store`、`Product`、`Order`、`OrderItem` |
| DTO | `dto` | 层间传参：前端原始参数 `DashboardQueryDTO`、校验后的查询条件 `OrderQueryDTO`、DAO 统计结果 `dto.stat.*` |
| VO | `vo` | 返回给前端的数据结构，如 `DashboardVO`、`HealthVO` |
| Result | `common` | 统一返回结构 `{code, message, data}` |

### 前端分层

```text
pages / components   只负责展示与交互
     │
composables          useDashboard：筛选状态、URL 同步、Toast；useApi：注入 API 实例
     │
api                  http.ts：请求与 Result 解包、ApiError；dashboard.ts：各接口函数
```

## 功能

- **核心指标**：营业额、订单量、客单价、会员订单占比，自动对比上一等长周期。
- **筛选**：近 7 / 30 / 90 天，自定义起止日期，全部门店或单店。
- **图表**：SVG 营收面积折线图、品类环形图、时段订单柱状图，折线图支持悬停查看每日数据。
- **经营分析**：门店营收排名、Top 6 人气单品、最近订单流水。
- **CSV 导出**：按照当前日期与门店筛选条件导出带 BOM 的 CSV。
- **交互与响应式**：数字滚动、加载骨架、Toast、URL 筛选状态同步、移动端抽屉导航与订单卡片。
- **自动初始化**：SQLite 表不存在时自动建表；没有订单时自动生成 120 天演示数据。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Nuxt 4.6 / Vue 3.5 / TypeScript / 原生 CSS / SVG |
| 前端运行时 | Node.js 24（也兼容 Node.js 22.22.3+） |
| 后端 | Java 17 / Spring Boot 3.5 / Spring JDBC |
| 数据库 | SQLite + `sqlite-jdbc` |
| 构建 | npm / Maven |

## 目录结构

```text
.
├── frontend/                       # Nuxt 前端
│   ├── app/
│   │   ├── api/
│   │   │   ├── http.ts             # HTTP 客户端、Result 解包、ApiError
│   │   │   └── dashboard.ts        # health / dashboard / export 接口
│   │   ├── composables/
│   │   │   ├── useApi.ts           # 读取 apiBase，创建 API 实例
│   │   │   └── useDashboard.ts     # 筛选、加载状态、URL 同步
│   │   ├── components/             # 侧边栏、图表、订单表格等
│   │   ├── pages/index.vue         # 看板页面
│   │   ├── types/                  # api.ts（Result）、dashboard.ts（VO 类型）
│   │   ├── utils/format.ts
│   │   ├── assets/css/main.css
│   │   └── app.vue
│   ├── public/favicon.svg
│   ├── nuxt.config.ts
│   ├── package.json
│   └── package-lock.json
│
├── backend/                        # Spring Boot 后端
│   ├── src/main/java/com/mori/dashboard/
│   │   ├── controller/             # HealthController / DashboardController / OrderController
│   │   ├── service/                # Service 接口
│   │   │   ├── impl/               # Service 实现（业务逻辑）
│   │   │   └── support/            # Service 层公共组件：参数解析、商品行格式化
│   │   ├── dao/                    # DAO 接口
│   │   │   └── impl/               # DAO 实现（JdbcTemplate + SQL）
│   │   ├── entity/                 # 数据表实体
│   │   ├── dto/                    # 层间传输对象（查询条件、统计结果）
│   │   ├── vo/                     # 接口返回对象
│   │   ├── common/                 # Result 统一返回
│   │   ├── exception/              # BusinessException、全局异常处理
│   │   ├── config/                 # CORS、启动时初始化演示数据
│   │   └── util/                   # CsvUtil、NumberUtil
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── schema.sql
│   ├── src/test/                   # 工具类、Service（Mock DAO）、全链路接口测试
│   ├── data/mori-coffee.db         # 已包含演示数据
│   └── pom.xml
│
├── docs/preview.png
└── README.md
```

## 快速启动

### 环境要求

- Java 17+
- Maven 3.9+
- Node.js 24.15+（或 Node.js 22.22.3+）
- npm

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：<http://127.0.0.1:8080>

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

### 2. 启动前端

另开一个终端：

```bash
cd frontend
npm install
npm run dev
```

打开 <http://127.0.0.1:3000>。

前端默认访问 `http://127.0.0.1:8080/api`。如果后端端口不同，启动前端时覆盖：

```bash
NUXT_PUBLIC_API_BASE=http://127.0.0.1:9090/api npm run dev
```

## 配置

### 后端

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端端口 |
| `DB_PATH` | `./data/mori-coffee.db` | SQLite 文件路径，相对 `backend/` 运行目录 |
| `CORS_ORIGINS` | `http://localhost:*,http://127.0.0.1:*` | 允许的前端来源，逗号分隔 |
| `SEED_ENABLED` | `true` | 数据库为空时是否自动生成演示数据 |

示例：

```bash
SERVER_PORT=9090 DB_PATH=./data/dev.db mvn spring-boot:run
```

### 前端

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `NUXT_PUBLIC_API_BASE` | `http://127.0.0.1:8080/api` | 后端 API 根地址 |
| `PORT` | `3000` | Nuxt 生产服务端口 |

## API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 服务状态、订单量、数据日期范围 |
| GET | `/api/dashboard` | 看板聚合数据 |
| GET | `/api/orders/export` | 导出当前筛选结果的 CSV |

`/api/dashboard` 与导出接口支持：

| 参数 | 格式 | 说明 |
| --- | --- | --- |
| `start` | `YYYY-MM-DD` | 开始日期 |
| `end` | `YYYY-MM-DD` | 结束日期（包含当天） |
| `store` | 数字 | 门店 ID；省略表示全部门店 |

不传日期时，默认返回数据集中最后 30 天。单次查询最多 366 天。

JSON 接口统一返回 `Result` 结构：

```json
{ "code": 200, "message": "success", "data": { "...": "..." } }
```

参数不合法时 HTTP 状态码为 `400`，返回 `{"code":400,"message":"开始日期不能晚于结束日期。","data":null}`。CSV 导出接口直接返回文件流。

```bash
curl "http://127.0.0.1:8080/api/dashboard?start=2026-10-01&end=2026-10-31&store=2"

curl -OJ "http://127.0.0.1:8080/api/orders/export?start=2026-10-01&end=2026-10-31"
```

## 数据库

SQLite 中有四张表：

- `stores`：门店
- `products`：商品
- `orders`：订单主表
- `order_items`：订单明细

当前包含 3 家门店、15,261 笔订单，日期范围 `2026-07-15` 至 `2026-11-11`，累计营业额约 86 万元。

首次启动时：

1. Spring Boot 执行 `schema.sql` 建表和索引；
2. `DemoDataInitializer` 调用 `DemoDataService` 检查 `orders` 是否为空；
3. 如果为空，用固定随机种子生成 120 天演示数据；
4. 如果已有数据则直接跳过。

如需重置演示数据库：

```bash
# 先停止后端
rm backend/data/mori-coffee.db
cd backend && mvn spring-boot:run
```

## 构建与生产运行

### 后端

```bash
cd backend
mvn clean package
java -jar target/mori-coffee-backend-1.0.0.jar
```

### 前端

```bash
cd frontend
npm run build
PORT=3000 NUXT_PUBLIC_API_BASE=http://127.0.0.1:8080/api node .output/server/index.mjs
```

生产环境需要把 `CORS_ORIGINS` 设置为实际前端地址，例如：

```bash
CORS_ORIGINS=https://dashboard.example.com java -jar target/mori-coffee-backend-1.0.0.jar
```

## 校验命令

```bash
cd frontend
npm run typecheck
npm run build

cd ../backend
mvn test
```

已验证：后端单元测试与全链路接口测试（`mvn test`）、重构前后接口数据与 CSV 逐字节一致、后端自动建表与种子数据生成、默认/自定义日期和门店筛选、CSV 下载、CORS 预检、Nuxt 交互、移动端布局、错误状态与重试；浏览器控制台无报错。

## 说明

- 品牌、门店、商品和订单数据均为虚构，仅用于演示。
- 后端接口目前均为只读查询，不提供写入 API。
- 前端不依赖外部图表库或 CDN，断网环境也能正常展示。
