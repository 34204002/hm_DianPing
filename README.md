# 🍜 FoodiePulse 本地生活服务平台

基于 Spring Boot 3 的高并发本地生活服务平台，覆盖商户查询、秒杀优惠券、UGC 内容生态、AI 智能搜索、图数据库社交推荐、用户行为分析等核心场景。

## 🛠 技术栈

| 分层 | 技术 |
|------|------|
| 框架 | Spring Boot 3.5、MyBatis-Plus |
| 数据库 | MySQL、Redis、Neo4j |
| 中间件 | Redisson、RabbitMQ |
| AI | Spring AI + OpenAI Function Calling |
| 工具 | Hutool、JWT、Lombok |

## 🚀 核心功能

### ⚡ 高并发秒杀系统
- 基于 Redisson 分布式锁实现一人一单并发控制，1000+ 并发下零超卖
- RabbitMQ 异步下单削峰填谷，响应时间 300ms → 50ms，QPS 提升 430%
- Redisson 令牌桶接口限流，Lua 脚本原子执行，秒杀接口 QPS 1500+

### 🗄 Redis 多级缓存体系
- 布隆过滤器防穿透 + 互斥锁防击穿 + TTL 随机化防雪崩
- 商户查询 QPS 200 → 1000，响应 <45ms
- 基于 GEO 实现附近商户毫秒级检索，HyperLogLog 统计 UV，Bitmap 签到

### 🤖 AI 智能搜索
- Spring AI + OpenAI 实现自然语言搜索商户
- Function Calling 自动编排工具调用（getShopTypes / searchShops），结果 Redis 缓存
- System Prompt 约束 AI 行为，仅基于工具返回数据回答，不编造

### 🧠 Neo4j 图数据库社交推荐
- User / Shop / Blog 三类节点 + FOLLOWS / WROTE / ABOUT / LIKES 四种关系
- 实时同步 MySQL 社交行为到 Neo4j，Cypher 图遍历实现推荐
- 好友去过的店 / 好友赞过的店 / 可能认识的人 三个推荐接口

### 📊 用户行为分析平台
- AOP 切面零侵入采集浏览 / 点赞 / 发布行为数据
- Redis Bitmap → DAU / WAU / MAU，ZSet → 热门商户排行，Hash → 用户偏好画像
- Spring Task 定时归档 MySQL，支持容错回扫，Redis 统一 TTL 管理

### 💬 UGC 内容与社交
- 博客发布、点赞、评论、关注、Feed 流推送
- Redis ZSet 实现点赞 Top 5 实时排名、Feed 滚动分页
- 共同关注基于 Neo4j Cypher 图遍历，一条语句双向查找

## 🏃 快速启动

**环境要求：** JDK 21、MySQL 8.0、Redis 7.0、RabbitMQ 3.x、Neo4j 5.x

1. 克隆项目，修改 `application-dev.yml` 中的数据库 / Redis / RabbitMQ / Neo4j 配置
2. 执行 `db/hmdp.sql` 初始化 MySQL 表结构
3. 启动 Neo4j，首次启动后执行 `MATCH (n) DETACH DELETE n` 清空测试数据
4. 申请 OpenAI API Key（或硅基流动等中转），配置到 `application-dev.yml`
5. 运行 `HmDianPingApplication`
