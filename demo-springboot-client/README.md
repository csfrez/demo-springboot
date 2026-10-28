# demo-springboot-client

> Dubbo 消费端 + HTTP 门面：用 `@DubboReference` 调远程 `DemoService`，暴露几个 REST 端点，并把自己注册到 SBA 管理台。

## 1. 模块定位

- 演示"Web 层 → Dubbo 消费者 → 远程提供者"的完整调用链：浏览器请求 `/sayHello`，本模块通过注册中心找到 `demo-springboot-server` 暴露的 `DemoService` 并获得返回值。
- 同时演示两块运维能力：SBA Client 上报（9999 管理台）与独立 `management.server.port`（业务 9090 / 管理 9091）。
- 依赖关系：依赖 `demo-springboot-dubbo`（获得 Dubbo starter 与日志过滤器）；**不依赖** `demo-springboot-server` 的 jar，只靠同名的 `DemoService` 接口全限定名与服务端对齐。
- 另有两个明显从安全示例复制来的端点（`/hello`、`/product/{id}`、`/order/{id}`），不参与 Dubbo 链路。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java | 1.8 | 编译级别（父 POM `java.version`） |
| Spring Boot | 2.7.18 | 由父 POM 管理（`spring-boot-dependencies` import） |
| spring-boot-starter-web | 2.7.18 | 由父 POM 管理，提供内嵌 Tomcat 与 Spring MVC |
| spring-boot-admin-starter-client | 2.7.16 | 由父 POM 管理（`spring-boot-admin-dependencies` 2.7.16），上报到 SBA |
| Dubbo | 2.7.23 | 由父 POM 管理，经 `demo-springboot-dubbo` 传递 |
| demo-springboot-dubbo | 2.0.1-SNAPSHOT | `project.version`，提供 starter + 两个日志过滤器 |
| spring-boot-starter-test | 2.7.18 | 由父 POM 管理（无测试类） |

## 3. 目录结构

```text
demo-springboot-client/
├── pom.xml
├── mvnw / mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
└── src/main/
    ├── java/com/csfrez/demospringboot/
    │   ├── DemoSpringbootClientApplication.java
    │   ├── dubbo/DemoService.java
    │   └── rest/DemoConsumerController.java
    └── resources/application.properties
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `DemoSpringbootClientApplication` | 启动类，仅 `@SpringBootApplication`；Dubbo/Admin 全靠 starter 自动装配 |
| `DemoService` | 与 server 同包同名的 Dubbo 契约，只声明 `String sayHello(String name)` |
| `DemoConsumerController` | `@RestController`，`@DubboReference` 注入 `DemoService`，提供 4 个端点，其中 `/sayHello` 直接返回远程调用结果 |
| `application.properties` | 端口 9090 / 管理端口 9091、SBA 地址、Dubbo 消费者参数（check=false、retries=0 等） |

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| 任意方法（`@RequestMapping` 未限定） | `/sayHello` | query：`name`（必填，缺失 400） | 调用远程 `DemoService.sayHello`，返回 `Hello, {name} (from Spring Boot), time is {ts}` |
| GET | `/hello` | — | 固定返回 `Hello Spring Security`（无安全框架，纯遗留字符串） |
| GET | `/product/{id}` | path：`id` | 返回 `product id : {id}`，无业务逻辑 |
| GET | `/order/{id}` | path：`id` | 返回 `order id : {id}`，无业务逻辑 |
| GET | `/actuator/*` | — | 在 9091 管理端口暴露（`exposure.include=*`），SBA 采集指标用 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.application.name` | `demo-springboot-client` | 也作为 `dubbo.application.name` 与 SBA 展示名 |
| `server.port` | `9090` | 业务 HTTP 端口 |
| `management.server.port` | `9091` | 管理端点独立端口，与业务隔离 |
| `management.endpoints.web.exposure.include` | `*` | 放开全部 Web 端点 |
| `management.endpoints.jmx.exposure.include` | `*` | 放开全部 JMX 端点 |
| `logging.level.root` | `INFO` | 根日志级别 |
| `spring.boot.admin.client.url` | `http://127.0.0.1:9999` | SBA 管理台地址 |
| `spring.security.user.name` / `password` | `admin` / `admin` | 占位配置，未引入 security 依赖，实际不生效 |
| `dubbo.application.name` | `${spring.application.name}` | 应用名引用 |
| `dubbo.scan.base-packages` | `com.csfrez.demospringboot.dubbo` | Dubbo 注解扫描包 |
| `dubbo.registry.address` | `zookeeper://10.67.7.1:2181` | 注册中心地址（硬编码内网 IP） |
| `dubbo.registry.timeout` | `10000` | 注册中心超时（ms） |
| `dubbo.registry.file` | `${user.home}/dubbo-cache/${spring.application.name}/dubbo.cache` | 注册信息本地缓存 |
| `dubbo.protocol.name` / `port` / `accepts` / `charset` | `dubbo` / `-1` / `1024` / `UTF-8` | 协议配置；`-1` 表示由 Dubbo 自动挑选可用端口 |
| `dubbo.consumer.*` | `init=false`、`check=false`、`loadbalance=leastactive`、`timeout=60000`、`proxy=javassist`、`retries=0` | 全局消费端默认值，`@DubboReference` 未覆盖时生效 |

- 服务端口：9090；管理端点 9091
- 依赖中间件：ZooKeeper `10.67.7.1:2181`（注册中心）、Spring Boot Admin `127.0.0.1:9999`（可选）、提供者 `demo-springboot-server`（含其 Dubbo 协议端口）

## 7. 启动方式

前置条件：JDK 8+、Maven、可连的 ZooKeeper；先启动 `demo-springboot-server` 并确认它已注册（`dubbo.consumer.check=false`，无提供者时本模块仍能启动，但调用会失败）。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests（同时安装 dao、dubbo）
cd demo-springboot-client
mvn spring-boot:run
```

启动后业务地址 <http://127.0.0.1:9090>，管理端点 <http://127.0.0.1:9091/actuator>。

## 8. 调用示例

```bash
# 走 Dubbo 远程调用（需 server 在线且 ZooKeeper 可达）
curl "http://127.0.0.1:9090/sayHello?name=csfrez"

# 本地固定返回
curl http://127.0.0.1:9090/hello
curl http://127.0.0.1:9090/product/1001
curl http://127.0.0.1:9090/order/2001

# 管理端口（不在 9090 上）
curl http://127.0.0.1:9091/actuator/health
```

`/sayHello` 正常返回示例：`Hello, csfrez (from Spring Boot), time is 1730000000000`。

## 9. 注意事项

- **注册中心硬编码**：`zookeeper://10.67.7.1:2181` 是内网地址，换环境必须改配置；由于 `dubbo.consumer.check=false` + `init=false`，ZK 不通时启动**不会**报错，第一次调用才抛 `RpcException`/`No provider available`，很容易误判为代码问题。
- **接口重复定义**：`DemoService` 在 client 与 server 各写一份（同全限定名、不同 jar），没有共享 api 模块；改签名只改一侧会在运行期炸，编译期完全无感知。
- **安全配置无效**：`spring.security.user.*` 存在但 pom 没有 `spring-boot-starter-security`，`/hello` 返回的 `Hello Spring Security` 只是字符串，不要认为已经启用了登录鉴权。
- **端口/端点易混**：业务 9090、管理 9091，SBA 采集走 9091；多个模块同机运行时 9091 属于本模块，别与其他模块的管理端口冲突。
- **重试与超时**：`retries=0`（不重试，避免重复副作用）、`timeout=60000`（60s），提供者慢时请求会长时间挂起；`proxy=javassist` 若迁到 Dubbo 3.x（默认 jdk 代理）需同步调整。

---

[← 返回项目总览](../README.md)
