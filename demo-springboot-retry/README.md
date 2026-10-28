# demo-springboot-retry

> 基于 Spring Retry 的声明式重试示例，覆盖异常匹配、指数/固定退避与恢复方法。

## 1. 模块定位

通过 `@EnableRetry` + `@Retryable` + `@Recover` + `@Backoff` 演示方法级重试：随机失败的通用方法、模拟网络调用、模拟数据库调用，并用 `RetryListener` 记录每次失败和最终结果。网络与数据库异常均为本地模拟，不连接真实中间件。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java | 8 | 编译目标（根 pom `java.version`） |
| Spring Boot | 2.7.18 | 父工程、Web 与自动配置 |
| Spring Retry | 1.3.4（Boot 2.7.18 BOM 管理） | `@Retryable`、`@Recover`、`RetryListener` |
| Spring AOP | 随 Boot 2.7.18 BOM | 为重试注解创建代理 |

## 3. 目录结构

```text
demo-springboot-retry/
├── pom.xml
└── src/main
    ├── java/com/csfrez/retry/
    │   ├── RetryApplication.java
    │   ├── config/RetryConfig.java
    │   ├── listener/RetryEventListener.java
    │   ├── rest/{HelloController,RetryController}.java
    │   └── service/{HelloService,RetryService}.java
    └── resources/application.properties
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `RetryApplication` | 启动类，`@EnableRetry` 开启注解式重试。 |
| `RetryConfig` | 注册 `RetryEventListener` Bean；Spring Retry 会把它加入重试拦截器监听链。 |
| `RetryEventListener` | 继承 `RetryListenerSupport`；`onError` 记录第几次尝试失败，`close` 记录最终成功或耗尽。 |
| `RetryService` | 声明三个 `@Retryable` 方法及对应 `@Recover`，同时维护两个进程内尝试计数器。 |
| `RetryController` | 暴露 `/retry/network`、`/retry/database`，直接调用重试服务。 |
| `HelloController` / `HelloService` | `/hello` 经 `HelloService` 调用 `retryableMethod()`，用于演示通用异常重试。 |

重试规则：
- `retryableMethod()`：`value = Exception.class`，`maxAttempts = 5`，`Backoff(delay = 1000, multiplier = 2)`。即最多 5 次尝试（首次 + 4 次重试），间隔约 1、2、4、8 秒；随机数小于 0.9 时抛 `RuntimeException`，耗尽后由 `recover(Exception)` 返回 `Recovery: ...`。
- `retryNetworkCall()`：`maxAttempts = 4`，未限定异常类型，`Backoff(delay = 500, multiplier = 2)`，即最多 4 次尝试，间隔约 0.5、1、2 秒；`networkAttempt < 2` 时抛 `NetworkException`，耗尽后由 `recover(NetworkException)` 兜底。
- `retryDatabaseCall()`：`include = DataAccessException.class`，`maxAttempts = 2`，`Backoff(delay = 1000)`（默认 multiplier=1），即最多 2 次尝试、固定间隔 1 秒；`dbAttempt < 2` 时抛本地 `DataAccessException`，耗尽后由同名参数的重载 `recover` 兜底。

`maxAttempts` 包含首次调用，不是“额外重试次数”。`@Recover` 仅在重试耗尽后触发；正常成功不会进入恢复方法。控制器通过注入的 Bean 调用，满足 Spring AOP 代理前提。

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/hello` | 无 | 调用 `retryableMethod()`，最多 5 次尝试；成功返回 `Success {随机数}`，耗尽返回 `Recovery: ...`。 |
| GET | `/retry/network` | 无 | 调用 `retryNetworkCall()`；首次启动后的第一次调用会失败一次再成功，返回 `Network call succeeded` 或恢复信息。 |
| GET | `/retry/database` | 无 | 调用 `retryDatabaseCall()`；首次调用失败一次后成功，返回 `Database call succeeded` 或恢复信息。 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | HTTP 端口。 |
| `spring.application.name` | `demo-springboot-retry` | 应用名。 |
| `retryableMethod.maxAttempts` | `5` | 注解值，含首次调用。 |
| `retryableMethod.backoff` | `delay=1000ms, multiplier=2` | 指数退避：1/2/4/8 秒。 |
| `retryNetworkCall.maxAttempts` | `4` | 注解值，含首次调用。 |
| `retryNetworkCall.backoff` | `delay=500ms, multiplier=2` | 指数退避：0.5/1/2 秒。 |
| `retryDatabaseCall.maxAttempts` | `2` | 注解值，含首次调用。 |
| `retryDatabaseCall.backoff` | `delay=1000ms` | 固定间隔 1 秒。 |
| `retryDatabaseCall.include` | 本地 `DataAccessException` | 只重试该自定义异常。 |

- 服务端口：`8080`
- 依赖中间件：无（网络、数据库均为代码模拟，不访问真实服务）

## 7. 启动方式

前置条件：JDK 8、Maven；无需数据库或消息队列。无外部配置，直接启动即可。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-retry
mvn spring-boot:run
```

## 8. 调用示例

```bash
# 通用重试：可能等待约 15 秒（1+2+4+8）
curl http://localhost:8080/hello

# 网络重试：首次调用会失败一次，随后成功
curl http://localhost:8080/retry/network

# 数据库重试：首次调用失败一次，固定等待约 1 秒
curl http://localhost:8080/retry/database
```

## 9. 注意事项

- **计数器是单例可变状态**：`networkAttempt`、`dbAttempt` 是 `RetryService` 的实例字段，进程内只增不减且非线程安全。首次请求后计数器已大于阈值，后续请求会直接成功、不再演示重试；并发下次数也不可靠。应改为每次调用重置的局部变量或按请求隔离。
- **注释与代码不一致**：`retryableMethod` 注释写“每次间隔 5 秒”，实际是 1 秒起步、2 倍递增；`retryNetworkCall` 注释写“重试 3 次”，实际 `maxAttempts=4`（首次 + 3 次重试）。
- **非幂等操作重试风险**：`retryDatabaseCall` 只递增内存计数来模拟写库，没有事务、唯一键或幂等键。真实新增/扣款/下单被重试可能重复执行；只应对幂等操作重试。
- `retryableMethod` 用 `Exception.class` 捕获过宽，会把空指针、参数错误等不可恢复异常也重试 5 次，浪费 15 秒；应按业务异常收窄。
- `RetryService` 中的 `DataAccessException` 是本文件自定义的同名类，**不是** Spring 的 `org.springframework.dao.DataAccessException`；`include` 只匹配这个本地类型，命名容易误导。
- 重试在请求线程同步执行，`/hello` 最坏阻塞约 15 秒且无超时、熔断；上游需设置超时并评估雪崩风险。
- `@Retryable`/`@Recover` 依赖 Spring 代理：自调用（同类方法直接调用）不会触发重试；恢复方法必须与重试方法在同一 Bean 中。

---

[← 返回项目总览](../README.md)
