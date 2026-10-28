# demo-springboot-redis

> 基于 RedisTemplate 与 Redisson 的分布式锁演示模块，同时包含 Lua 原子加解锁脚本。

## 1. 模块定位

演示两种分布式锁实现：Spring Data Redis + Lua 自定义锁，以及 Redisson `RLock`；通过并发请求验证同一 `identity` 的串行执行。`RedisUtil` 是 `RedisTemplate` 的静态 Lua 执行封装，`HelloController` 提供两个测试入口。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java | 8 | 编译目标（根 pom `java.version`） |
| Spring Boot | 2.7.18 | 父工程与自动配置 |
| Spring Data Redis | 随 Boot 2.7.18 BOM | `StringRedisTemplate`/`RedisTemplate` 与 Lua 执行 |
| Redisson | 3.17.7 | `RLock` 可重入分布式锁 |
| Hutool | 5.8.33 | `SpringUtil`、UUID、随机数、HTTP 测试 |
| Lombok | 随 Boot BOM | 日志与 getter |

## 3. 目录结构

```text
demo-springboot-redis/
├── pom.xml
└── src
    ├── main
    │   ├── java/com/csfrez/redis/
    │   │   ├── RedisApplication.java
    │   │   ├── config/RedisConfig.java
    │   │   ├── lock/{BaseRedisLock,BaseRedissonLock,RedisDistributedLock}.java
    │   │   ├── rest/HelloController.java
    │   │   └── util/RedisUtil.java
    │   └── resources/
    │       ├── application.yml
    │       ├── lua/{lock.lua,release.lua}
    │       └── static/index.html
    └── test/java/com/csfrez/redis/
        ├── RedisApplicationTests.java
        └── lock/HelloTest.java
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `RedisApplication` | Spring Boot 启动类，扫描 `com.csfrez.redis`。 |
| `RedisConfig` | 配置 String key/JSON value 的 `RedisTemplate`；**`@Configuration` 被注释，当前不生效**。 |
| `RedisUtil` | 静态持有 `RedisTemplate`，提供泛型 `executeLua(script, keys, args)`；当前无调用方。 |
| `RedisDistributedLock` | 自定义锁核心：UUID 作为 value，`lock.lua` 执行 `SET NX EX`，`release.lua` 比对 value 后删除；value 存于 `ThreadLocal`。 |
| `BaseRedisLock` | 锁模板：key 前缀 `redis:lock:`，默认 TTL 30 秒、5 次尝试；提供 `consume/execute/supply`，`finally` 释放。 |
| `BaseRedissonLock` | 封装 Redisson `RLock`：key 前缀 `redisson:lock:`，默认显式租期 30 秒，释放前校验当前线程持有。 |
| `HelloController` | 两个并发演示接口，业务睡眠 1–4 秒后返回。 |
| `HelloTest` | 以 10 个线程并发请求两个接口，验证互斥；`RedisApplicationTests` 仅加载上下文。 |

加锁/解锁细节：`BaseRedisLock` 生成 `redis:lock:redis_lock:{identity}`（`name` 转小写），`tryLock` 用 `IdUtil.fastSimpleUUID()` 生成唯一 value，执行 `SET key value NX EX expireSeconds`；成功即写入 `ThreadLocal`。释放时 `release.lua` 先 `GET` 比较 value，相等才 `DEL`，避免误删他人锁。失败后按 1、3、6、10、15 秒递增等待（累计睡眠），最多循环 5 次。Redisson key 为 `redisson:lock:redisson_lock:{identity}`，通过 `redissonClient.getLock(...).lock(30, SECONDS)` 获取，`unlock()` 释放；**显式传入 leaseTime 后看门狗不会自动续期**。

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/redis/{identity}` | `identity`：路径变量，锁标识 | 持有 Lua 锁，随机睡眠 1–4 秒，返回 `redis world {随机数}`；相同 identity 串行。 |
| GET | `/redisson/{identity}` | `identity`：路径变量，锁标识 | 持有 Redisson 锁，随机睡眠 1–4 秒，返回 `redisson world {随机数}`。 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | HTTP 端口。 |
| `spring.application.name` | `demo-springboot-redis` | 应用名。 |
| `spring.redis.host` | `10.11.0.136` | Redis 地址，**硬编码在 yml**。 |
| `spring.redis.port` | `6379` | Redis 端口。 |

- 服务端口：`8080`
- 依赖中间件：Redis（Redisson 与 Spring Data Redis 共用该实例；未配置密码、库号、超时）

## 7. 启动方式

前置条件：Redis 可访问（默认 `10.11.0.136:6379`，按需改 `application.yml`）；JDK 8、Maven；无需先建库。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-redis
mvn spring-boot:run
```

## 8. 调用示例

```bash
# Lua 锁：并发执行时相同 identity 会排队
curl http://localhost:8080/redis/123456

# Redisson 锁
curl http://localhost:8080/redisson/123456

# 压测（需先启动服务）：10 个并发请求
# 也可直接运行 HelloTest 中的两个 @Test
```

## 9. 注意事项

- **严重逻辑错误**：`RedisDistributedLock.tryLock` 在 5 次全部失败后仍 `return true`，调用方会误以为持锁并执行临界区；最后一次失败还会先睡眠约 15 秒。应返回 `false` 并抛异常。
- **未实现可重入**：注释声称用 `ThreadLocal` 判断重入，实际只有一个 value 槽位，嵌套加锁会生成新 UUID，无法重入；内层释放还会提前删除外层锁的映射。
- 锁 TTL 固定 30 秒且无续期，业务执行超过 30 秒会失去互斥；应评估 TTL 或使用看门狗。
- 重试等待采用线性累加睡眠，最坏总等待约 35 秒，且发生在请求线程；`maxRetries` 名为重试次数，实际是总尝试次数。
- `RedisConfig` 的 `@Configuration` 被注释，自定义 JSON 序列化未启用；`BaseRedissonLock`、`RedisDistributedLock` 又在静态字段中用 Hutool `SpringUtil` 取 Bean，而模块内没有注册 `SpringUtil`，存在类加载/初始化时序风险。
- `spring.redis.host` 硬编码为内网地址，换环境必须改配置；生产应使用环境变量/配置中心并补密码、超时。
- 测试强依赖本机 `8080` 服务已启动及 Redis 可用；`contextLoads` 也会触发真实连接配置。

---

[← 返回项目总览](../README.md)
