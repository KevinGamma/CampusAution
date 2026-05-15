<p align="center">
  <h1 align="center">🏫 校园拍卖与集市平台</h1>
  <p align="center">Campus Auction & Marketplace — 山东大学校园二手交易平台</p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.5-6DB33F?logo=springboot" />
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs" />
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.10-blue" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" />
  <img src="https://img.shields.io/badge/Element_Plus-2.8-409EFF?logo=element" />
  <img src="https://img.shields.io/badge/JWT-HS256-black?logo=jsonwebtokens" />
</p>

---

## 📖 项目简介

本项目是一个专为**山东大学（SDU）**校园社区设计的综合性二手物品交易平台。学生可在此平台挂出闲置物品，通过**竞价拍卖**或**一口价直购**两种模式与买家达成交易，平台内置完整的金融信用闭环与双向评价体系。

> **目标**：让校园闲置物品以公开透明的方式流转，减少浪费，促进低碳循环消费。

---

## ✨ 核心特性

### 🔨 双交易模式
- **竞价拍卖**：卖家设定起拍价与截止时间，系统自动追踪最高出价；卖家可随时接受当前最高价或指定某笔出价成交。
- **一口价直购**：买家一键按固定价格购买，无需等待拍卖结束，适合急售场景。

### 💰 金融闭环
- **账户充值**：用户可向个人余额账户充值。
- **余额校验**：出价/购买前前后端双重余额校验，防止透支。
- **原子扣款**：通过数据库单条 `UPDATE ... WHERE balance >= amount` 语句实现免锁原子扣减，彻底消除 TOCTOU 竞争条件。
- **平台手续费**：每笔成交收取 **5%** 平台服务费，卖家实际到账为 `成交价 × 95%`。

### 🌟 社交信用
- **自定义资料**：用户可上传头像、编辑个人简介。
- **卖家主页**：公开展示卖家信息、在售商品及历史评价，供买家参考。
- **双向评价**：交易完成后，买卖双方均可对对方进行 1–5 星评价与文字留言，评价与平均评分实时更新。

### 🖼️ 资源管理
- **多图上传**：支持多张图片上传（JPEG / PNG / GIF / WebP，单文件 ≤ 10 MB）。
- **轮播展示**：商品详情页自动轮播多张图片。
- **全生命周期编辑**：商品在 ACTIVE 状态下，创建者可修改标题、描述、价格及图片，权限由后端严格校验。

### 🔍 精准检索
- **关键词搜索**：标题与描述全文模糊匹配。
- **分类筛选**：按商品类别快速过滤。
- **价格/日期范围**：支持最低价、最高价、上架起止时间复合过滤。
- **多维排序**：按最新上架、价格升序、价格降序灵活切换。

### 🛡️ 职责分离（RBAC）

| 角色 | 权限 |
|------|------|
| **STUDENT（学生）** | 发布商品、参与竞价/直购、充值、编写评价 |
| **ADMIN（管理员）** | 查看全部数据、删除任意商品、生成模拟测试数据 |

---

## 🏗️ 技术架构

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.5 | Web 框架、依赖注入、事务管理 |
| MyBatis-Plus | 3.5.10.1 | ORM，Lambda 查询 DSL，行级锁 |
| MySQL | 8.0+ | 关系型数据存储 |
| JWT (jjwt) | 0.12.6 | 无状态身份认证（HS256，7 天有效期） |
| Spring Transaction | — | `@Transactional` 声明式事务，ACID 保证 |
| Lombok | — | 样板代码消除 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.4.29 | Composition API，响应式状态 |
| Vite | 5.3.1 | 极速构建与热更新 |
| Element Plus | 2.8.0 | 企业级 UI 组件库 |
| vue-router | 4.3.3 | 客户端路由与导航守卫 |
| Axios | 1.7.2 | HTTP 客户端，全局 JWT 拦截器 |

### 系统架构示意

```
┌──────────────────────────────────────────────┐
│                  Browser (Vue 3)              │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐  │
│  │ HomeView │  │ AuctionD │  │ AdminView  │  │
│  └────┬─────┘  └────┬─────┘  └─────┬──────┘  │
│       └─────────────┴──────────────┘          │
│              Axios + JWT Interceptor           │
└───────────────────────┬──────────────────────┘
                        │ REST / JSON
┌───────────────────────▼──────────────────────┐
│              Spring Boot 3.4.5                │
│  ┌─────────────────────────────────────────┐  │
│  │  JwtInterceptor → @RoleAccess Guard     │  │
│  └──────────────────┬──────────────────────┘  │
│  ┌──────────────────▼──────────────────────┐  │
│  │  AuctionController │ UserController      │  │
│  │  ReviewController  │ FileController      │  │
│  └──────────────────┬──────────────────────┘  │
│  ┌──────────────────▼──────────────────────┐  │
│  │  AuctionService (@Transactional)        │  │
│  │  • placeBid()  FOR UPDATE 行级锁        │  │
│  │  • acceptBid() 5% 费率扣减              │  │
│  │  • buy()       原子余额扣减             │  │
│  └──────────────────┬──────────────────────┘  │
│  ┌──────────────────▼──────────────────────┐  │
│  │  MyBatis-Plus Mapper Layer              │  │
│  │  UserMapper.deductBalanceIfSufficient() │  │
│  └──────────────────┬──────────────────────┘  │
└───────────────────────┬──────────────────────┘
                        │
┌───────────────────────▼──────────────────────┐
│                  MySQL 8.0                    │
│  users │ auctions │ bids │ orders │ reviews   │
└──────────────────────────────────────────────┘
```

---

## ⚙️ 业务逻辑说明

### 5% 平台手续费

每笔成交（无论竞价还是直购）均收取成交金额的 **5%** 作为平台服务费：

$$\text{卖家实际到账} = \text{成交价} \times (1 - 5\%) = \text{成交价} \times 0.95$$

代码实现（[`AuctionServiceImpl.java`](src/main/java/com/campus/auction/service/impl/AuctionServiceImpl.java)）：

```java
private static final BigDecimal FEE_RATE = new BigDecimal("0.05");

BigDecimal sellerReceives = price
    .multiply(BigDecimal.ONE.subtract(FEE_RATE))
    .setScale(2, RoundingMode.HALF_UP);
```

使用 `BigDecimal` 全程精确运算，`HALF_UP` 舍入规则，杜绝浮点误差。

---

### 三层安全防御（以余额校验为例）

| 层级 | 位置 | 机制 |
|------|------|------|
| **第一层：前端禁用** | Vue 组件 | 余额不足时，"出价"/"购买"按钮置灰，阻止无效请求 |
| **第二层：后端逻辑拦截** | `AuctionServiceImpl` | Service 层读取用户余额并与出价金额比对，不满足则抛出 `ServiceException(400)` |
| **第三层：数据库行级原子操作** | `UserMapper` | 单条 SQL 实现"校验+扣减"原子性，彻底消除并发竞争 |

```sql
-- 第三层：原子扣款（UserMapper.java）
UPDATE users
   SET balance = balance - #{amount}
 WHERE id = #{id}
   AND balance >= #{amount}
-- 返回 0 行 → 余额不足，上层回滚事务
```

---

### 竞价安全（防超卖 & 防重入）

出价时对拍卖行加 `FOR UPDATE` 悲观锁：

```java
Auction auction = lambdaQuery()
    .eq(Auction::getId, auctionId)
    .last("FOR UPDATE")          // 行级排他锁
    .one();
```

同一时刻只有一个事务能修改 `current_price`，高并发下仍保证出价有序、当前价准确。

---

## 🗄️ 数据库设计

| 表名 | 主要字段 | 说明 |
|------|----------|------|
| `users` | id, username, password, balance, role, avatar_url, bio | 用户账户，角色枚举 STUDENT/ADMIN |
| `auctions` | id, title, start_price, current_price, end_time, status, sale_type, image_urls, category | 商品列表，状态枚举 ACTIVE/FINISHED/SOLD/CANCELLED |
| `bids` | id, auction_id, bidder_id, amount, timestamp | 竞价记录 |
| `orders` | id, auction_id, bid_id(可空), buyer_id, seller_id, amount | 成交订单，bid_id 为空表示直购 |
| `reviews` | id, order_id, reviewer_id, reviewee_id, rating, comment | 评价，UNIQUE(order_id, reviewer_id) 防重复评价 |

> 初始化脚本：[`src/main/resources/schema.sql`](src/main/resources/schema.sql)  
> 默认管理员账号：`admin / admin123`

---

## 📡 API 接口概览

> 所有需要认证的接口均需在请求头携带：`Authorization: Bearer <token>`

### 用户接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/users/register` | 用户注册 | 公开 |
| POST | `/users/login` | 用户登录，返回 JWT | 公开 |
| POST | `/users/recharge` | 账户充值 | 已登录 |
| PUT  | `/users/profile` | 更新头像 / 简介 | 已登录 |
| GET  | `/users/{id}` | 获取卖家公开资料 | 公开 |
| GET  | `/users/{id}/reviews` | 获取用户收到的评价 | 公开 |

### 商品接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET  | `/auctions` | 列表（含搜索、筛选、排序） | 已登录 |
| GET  | `/auctions/{id}` | 商品详情（ETag 缓存） | 已登录 |
| GET  | `/auctions/all` | 全部商品（任意状态） | ADMIN |
| POST | `/auctions` | 发布商品 | STUDENT |
| PUT  | `/auctions/{id}` | 编辑商品（仅创建者） | STUDENT |
| POST | `/auctions/{id}/bids` | 竞价出价 | STUDENT |
| POST | `/auctions/{id}/buy` | 一口价购买 | STUDENT |
| POST | `/auctions/{id}/accept-current-highest` | 接受最高出价 | STUDENT（创建者） |
| POST | `/auctions/{id}/accept-bid/{bidId}` | 接受指定出价 | STUDENT（创建者） |
| POST | `/auctions/{id}/cancel` | 取消拍卖 | STUDENT（创建者） |
| DELETE | `/auctions/{id}` | 删除商品 | 创建者 / ADMIN |

### 评价 & 文件接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/reviews` | 提交交易评价 | 已登录 |
| POST | `/api/images/upload` | 上传图片（multipart/form-data） | 已登录 |

---

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+

### 后端启动

```bash
# 1. 克隆项目
git clone <repo-url>
cd campus-auction

# 2. 创建数据库
mysql -u root -p -e "CREATE DATABASE campus_auction CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. 初始化表结构
mysql -u root -p campus_auction < src/main/resources/schema.sql

# 4. 修改数据库连接配置
# 编辑 src/main/resources/application.yml
#   spring.datasource.url / username / password

# 5. 启动后端
mvn clean spring-boot:run
# 服务默认监听 http://localhost:8080
```

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式（热更新）
npm run dev
# 访问 http://localhost:5173

# 生产构建
npm run build
```

---

## 📁 项目结构

```
campus-auction/
├── src/main/java/com/campus/auction/
│   ├── annotation/       # @RoleAccess 权限注解
│   ├── context/          # UserContext（ThreadLocal 用户信息）
│   ├── controller/       # REST 控制器（5 个）
│   ├── dto/              # 请求/响应 DTO（17 个）
│   ├── entity/           # 数据实体（5 个：User/Auction/Bid/Order/Review）
│   ├── enums/            # 枚举：UserRole / AuctionStatus / SaleType
│   ├── exception/        # ServiceException（携带 HttpStatus）
│   ├── interceptor/      # JwtInterceptor（认证 + 授权一体）
│   ├── mapper/           # MyBatis-Plus Mapper（含自定义原子 SQL）
│   ├── service/          # 业务逻辑（接口 + 实现，5 个服务）
│   └── utils/            # JwtUtils（HS256 签发/校验）
├── src/main/resources/
│   ├── application.yml   # 主配置（端口、数据库、JWT、上传限制）
│   └── schema.sql        # 建表脚本（含默认管理员数据）
├── frontend/
│   ├── src/
│   │   ├── api/          # Axios 封装（auction / user / review / upload）
│   │   ├── composables/  # useAuth（单例认证状态）/ useCountdown
│   │   ├── router/       # 路由配置与导航守卫（角色验证）
│   │   ├── views/        # 8 个页面组件
│   │   └── components/   # 复用组件（AuctionCard / SearchFilterBar / EditDialog）
│   ├── package.json
│   └── vite.config.js
├── uploads/              # 上传图片本地存储目录
└── pom.xml
```

---

## 🏆 项目亮点

1. **并发安全的竞价系统**：`FOR UPDATE` 行级锁 + `@Transactional` 事务组合，高并发下仍能保证出价有序、当前价准确，无需引入分布式锁。

2. **TOCTOU 免疫的余额扣减**：单条原子 SQL `UPDATE ... WHERE balance >= amount` 替代"先查后改"两步操作，从根本上消除 Check-Then-Act 竞争条件。

3. **精确金融计算**：全程 `BigDecimal` + `RoundingMode.HALF_UP`，5% 手续费计算不产生任何浮点误差。

4. **三层权限防御**：前端 UI 禁用 → Service 层逻辑校验 → 数据库原子操作，任意单层被绕过均不影响数据一致性。

5. **RESTful API 设计**：语义化 HTTP 方法（GET/POST/PUT/DELETE），统一 `Result<T>` 响应信封，HTTP 状态码与业务码双重表达错误语义。

6. **轻量前端状态管理**：`useAuth` Composable 通过闭包实现跨组件共享认证状态，结合 localStorage 持久化，刷新页面不丢登录态，无需引入 Vuex/Pinia。

7. **ETag HTTP 缓存**：商品详情接口支持 ETag，重复请求命中缓存时返回 `304 Not Modified`，减少不必要的带宽消耗。

8. **防枚举攻击**：登录接口对"用户不存在"与"密码错误"统一返回相同错误信息，防止攻击者通过响应差异枚举有效用户名。

---

## 📄 许可证

本项目仅供学习与课程展示使用。

---

<p align="center">Made with ❤️ for SDU Campus</p>
