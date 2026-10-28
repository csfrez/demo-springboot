# demo-springboot-thread

> 自定义线程池、拒绝策略、运行监控与钉钉告警的线程池实践示例。

## 1. 模块定位

演示如何扩展 Spring `ThreadPoolTaskExecutor` 与 JDK `ThreadPoolExecutor`：统一传递 MDC 上下文、定制拒绝策略、采集线程池指标，并在过载时发送钉钉告警。注意源码包名是 `com.csfrez.retry`，与模块名不一致，但启动类扫描该包，组件仍可生效。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java | 8 | 编译目标（根 pom `java.version`） |
| Spring Boot | 2.7.18 | 父工程、Web 与自动配置 |
| Spring Framework | 随 Boot 2.7.18 BOM | `ThreadPoolTaskExecutor`、`@EnableScheduling` |
| Hutool | 5.8.33 | `HttpUtil` 发送钉钉告警、`StrUtil` 格式化 |

## 3. 目录结构

```text
demo-springboot-thread/
├── pom.xml
└── src/main
    ├── java/com/csfrez/retry/
    │   ├── ThreadApplication.java
    │   ├── config/HelloConfig.java
    │   ├── monitor/ThreadPoolMonitor.java
    │   ├── policy/{CustomCallerRunsPolicy,CustomRejectedPolicy}.java
    │   ├── pool/{CustomThreadPoolExecutor,CustomThreadPoolTaskExecutor}.java
    │   ├── rest/HelloController.java
    │   ├── service/HelloService.java
    │   └── util/{DingAlertUtil,ThreadUtil}.java
    └── resources/application.properties
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `ThreadApplication` | 启动类，`@EnableScheduling` 开启调度支持。 |
| `HelloConfig` | 声明 `CustomThreadPoolTaskExecutor`：core=2、max=5、queue=10、keepAlive=500 秒、前缀 `common-`、守护线程、`CustomCallerRunsPolicy`。 |
| `CustomThreadPoolExecutor` | 继承 JDK 线程池，重写 `execute`/`submit(Callable)`，用包装器把父线程 MDC 复制到工作线程并在结束后还原。 |
| `CustomThreadPoolTaskExecutor` | 继承 Spring 线程池，重写 `execute`、`submit(Callable)`、`submitListenable(Runnable)` 传递 MDC；任务结束统一 `MDC.clear()`。 |
| `CustomCallerRunsPolicy` | 拒绝时记录日志、**同步发送钉钉告警**，未关闭则在提交线程直接 `task.run()`（CallerRuns 语义）。 |
| `CustomRejectedPolicy` | 拒绝时把任务放入 `ThreadUtil` 的静态队列，不执行任务；当前未被启用。 |
| `ThreadPoolMonitor` | 读取 `ThreadPoolTaskExecutor` 底层执行器，打印活跃线程数、最大线程数、队列长度。 |
| `DingAlertUtil` | 拼接钉钉 text 消息并 POST 到机器人 webhook，异常仅记录日志。 |
| `ThreadUtil` | 维护无界 `LinkedBlockingQueue`，供拒绝策略暂存任务，`/retry` 手工取出执行。 |
| `HelloService` / `HelloController` | 提交单任务或批量任务，并提供接口观察提交与“重试”。 |

线程池执行顺序：先占满 2 个核心线程，再进入容量 10 的队列，队列满后扩容到 5 个最大线程，仍满才触发拒绝；因此约 15 个并发任务后开始 CallerRuns。`CustomThreadPoolExecutor` 与 `CustomThreadPoolTaskExecutor` 的 MDC 包装不会改变任务本身；后者只清空、不恢复工作线程原有 MDC。监控只包含活跃数、最大线程数、队列长度，未统计核心数、当前线程数、完成任务数和拒绝次数。

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/hello` | 无 | 提交一个打印线程名的任务，`HelloService.hello()` 返回 `null`，响应体为空。 |
| GET | `/hello/{count}` | `count`：路径变量，提交任务数 | 循环提交 `count` 个任务，每个随机睡眠 0–4999 毫秒后打印；接口立即返回空响应。 |
| GET | `/retry` | 无 | 取出 `ThreadUtil` 队列中的拒绝任务，在请求线程逐个 `run()`，返回空响应。 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | HTTP 端口。 |
| `spring.application.name` | `demo-springboot-thread` | 应用名。 |
| `corePoolSize`（`HelloConfig` 硬编码） | `2` | 核心线程数。 |
| `maxPoolSize`（`HelloConfig` 硬编码） | `5` | 最大线程数。 |
| `queueCapacity`（`HelloConfig` 硬编码） | `10` | 任务队列容量。 |
| 拒绝策略（`HelloConfig` 硬编码） | `CustomCallerRunsPolicy` | 告警后由提交线程执行。 |
| 钉钉 webhook（`DingAlertUtil` 硬编码） | `access_token=` 为空 | 占位地址，实际无法发送成功。 |

- 服务端口：`8080`
- 依赖中间件：无；拒绝时会同步访问钉钉机器人（当前 webhook 为占位）

## 7. 启动方式

前置条件：JDK 8、Maven；无数据库/Redis 依赖。若需真实告警，先补齐 `DingAlertUtil` 中的 `access_token`。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-thread
mvn spring-boot:run
```

## 8. 调用示例

```bash
# 单任务
curl http://localhost:8080/hello

# 提交 20 个任务：会触发队列满、扩容和 CallerRuns
curl http://localhost:8080/hello/20

# 取出被 CustomRejectedPolicy 暂存的任务（当前策略下通常为空）
curl http://localhost:8080/retry
```

## 9. 注意事项

- **线程池参数全部写死**在 `HelloConfig`，无法按环境调整；修改后必须重新编译，生产应外置到配置中心或 `@ConfigurationProperties`。
- `ThreadPoolMonitor` 的 `@Scheduled(fixedRate = 1000)` 被注释，`@EnableScheduling` 实际没有定时任务，监控方法从未被调用；指标也缺少池大小、完成任务数、拒绝次数和耗时。
- **钉钉 webhook 只有 `https://oapi.dingtalk.com/robot/send?access_token=`，token 为空**；告警 URL 无超时设置，且拒绝策略在提交线程同步调用 HTTP，过载时会进一步拖慢请求。
- `CustomCallerRunsPolicy` 在告警后直接 `task.run()`，Web 请求线程可能执行最长约 5 秒的任务；高并发下会造成请求堆积和超时，这是有意的背压，但需评估。
- `ThreadUtil.pushRejectedTask` 先 `offer(task, 1, SECONDS)`，随后又无条件 `offer(task)`；第一次成功时任务会被重复入队，当前 `CustomRejectedPolicy` 并未启用，问题处于潜伏状态。
- `/retry` 不是真正的重试调度：它在请求线程同步执行队列任务，没有延迟、退避、幂等或失败再入队；队列无界，异常时可能堆积。
- `/hello/{count}` 不校验 `count`：负数为空操作，过大值会瞬间打满线程池并触发大量同步告警；批量任务各自 `new Random()`，睡眠上限 5 秒。

---

[← 返回项目总览](../README.md)
