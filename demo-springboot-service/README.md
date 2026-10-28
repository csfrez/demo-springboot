# demo-springboot-service

> 通过可序列化 Lambda 解析服务元数据，并用统一执行器完成服务调用、日志与结果封装的示例。

## 1. 模块定位

演示一种轻量的“服务编排/执行器”写法：调用方传入 `Service` 的方法引用和单个参数，`ServiceManager` 从 Lambda 中解析出目标类与方法名，从 Spring 容器取得 Bean，再由 `ServiceExecutor` 统一记录耗时、捕获异常并返回 `SerResult`。它适合作为方法论示例，不是完整的服务治理或工作流引擎。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java | 8 | 编译目标（根 pom `java.version`），依赖 `SerializedLambda` |
| Spring Boot | 2.7.18 | 父工程、Web 与自动配置 |
| Spring Context | 随 Boot 2.7.18 BOM | `ApplicationContext`、按类型获取 Bean |
| Spring AOP | 随 Boot 2.7.18 BOM | 已引入 starter，但当前源码没有切面 |
| Lombok | 随 Boot BOM | `@Data`、`@Setter`、`@Slf4j` |

## 3. 目录结构

```text
demo-springboot-service/
├── pom.xml
└── src/main
    ├── java/com/csfrez/svr/
    │   ├── ServiceApplication.java
    │   ├── dto/SerResult.java
    │   ├── lambda/SerialBiFunction.java
    │   ├── resolver/InstBuilder.java
    │   ├── rest/HelloController.java
    │   ├── service/HelloService.java
    │   ├── servicer/{ServiceExecutor,ServiceManager}.java
    │   └── util/{LambdaUtil,SpringUtil}.java
    └── resources/application.properties
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `ServiceManager` | 调用入口；以 Lambda 为 key 缓存 `LambdaMeta`，解析服务并组装执行器。 |
| `ServiceManager.LambdaMeta` | 保存服务 `Class`、Spring Bean 实例和方法名。 |
| `LambdaUtil` | 通过反射调用 Lambda 的 `writeReplace()`，取得 `SerializedLambda`。 |
| `SpringUtil` | 实现 `ApplicationContextAware`，暴露静态 `getBean(Class)`。 |
| `ServiceExecutor` | 执行 `serviceFn.apply(inst, param)`，记录开始/耗时/结果，异常转 `SerResult.fail`。 |
| `InstBuilder` | 通过无参构造反射创建对象，再以方法引用链式 set 属性。 |
| `SerialBiFunction` | 可序列化的双参数函数：服务实例 + 一个入参 → 返回值。 |
| `SerResult` | 统一结果：`code`（200/500）、`msg`、`data`。 |
| `HelloService` | 示例服务，返回 `当前毫秒时间戳 + " hello " + name`。 |

服务注册与执行流程：模块**没有独立注册中心**，所谓“注册”是首次调用时对 Lambda 的惰性解析与缓存。`ServiceManager.call(HelloService::hello, name)` 先以 Lambda 实例为 key 查 `ConcurrentHashMap`；未命中则用 `SerializedLambda.getImplClass()` 得到 `com/csfrez/svr/service/HelloService`，转成类名后 `Class.forName`，再通过 `SpringUtil.getBean` 从 Spring 容器取实例，并用 `getImplMethodName()` 得到 `hello`，封装成 `LambdaMeta` 缓存。随后 `InstBuilder` 创建 `ServiceExecutor` 并注入 Lambda、参数、元数据，`callService()` 执行 `serviceFn.apply(lambdaMeta.getInst(), param)`，成功返回 `SerResult.success(result)`，异常则记录错误日志并返回失败结果。`HelloController` 只取出 `SerResult.data`，忽略 `code`/`msg`。

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/hello` | `name`：查询参数，非必填，默认 `Peter` | 经 `ServiceManager` 调用 `HelloService.hello(name)`，返回时间戳问候语；失败时仍为 HTTP 200 且响应体为空。 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | HTTP 端口。 |
| `spring.application.name` | `demo-springboot-service` | 应用名。 |
| `ServiceManager.INIT_COUNT`（代码常量） | `6666` | `ConcurrentHashMap` 初始容量，不是容量上限，也没有淘汰策略。 |

- 服务端口：`8080`
- 依赖中间件：无（仅 Spring 容器与进程内缓存）

## 7. 启动方式

前置条件：JDK 8、Maven；无数据库、Redis 等外部依赖。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-service
mvn spring-boot:run
```

## 8. 调用示例

```bash
# 使用默认 name=Peter
curl http://localhost:8080/hello

# 指定参数；返回值形如 1730000000000 hello 张三
curl --get --data-urlencode "name=张三" http://localhost:8080/hello
```

## 9. 注意事项

- **异常被吞掉**：`ServiceExecutor` 捕获异常后返回 `SerResult.fail`，而 `HelloController` 直接 `ResponseEntity.ok(call.getData())`，既不看 `code` 也不返回 `msg`；调用失败时仍是 HTTP 200、空 body。应返回完整 `SerResult` 或映射为非 2xx。
- **缓存按 Lambda 对象身份命中**：`SerialBiFunction` 未覆写 `equals/hashCode`，`ConcurrentHashMap` 使用对象身份；不同调用点或每次新建的 Lambda 实例都会重复解析并新增条目，且没有淘汰，长运行下有内存增长风险。`6666` 只是初始容量。
- **能力边界明显**：只支持“实例方法 + 单个参数 + 单返回值”的 `SerialBiFunction`，不支持零参/多参、`void`、异步、批量、流程编排、补偿、重试、事务和超时；所谓“服务注册”只是从 Spring 容器按类取 Bean，不是注册中心。
- 解析依赖 `writeReplace` 反射和 `Class.forName`，仅适用于可序列化的方法引用；对 JDK 动态代理、接口式 Service 或非 public 实现类可能取不到/转型失败。
- `InstBuilder` 要求目标类有可访问的无参构造器，且不做 setter 非空校验；`ServiceExecutor` 是手工 `new` 出来的，不参与 Spring 依赖注入。
- `SpringUtil` 用静态字段保存上下文，多上下文会互相覆盖，也不利于单元测试；容器未初始化时调用会直接抛异常。
- `name` 没有非空、长度或字符校验，直接拼进响应；若用于页面展示需做转义与校验。
- 模块没有测试代码；引入的 AOP starter 目前没有对应切面，属于未使用依赖。

---

[← 返回项目总览](../README.md)
