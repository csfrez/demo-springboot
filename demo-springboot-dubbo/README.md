# demo-springboot-dubbo

> Dubbo 通用能力库：只提供消费端/提供端两个日志过滤器与 SPI 注册文件，供 client、server 以依赖方式复用。

## 1. 模块定位

- 演示 Dubbo Filter 扩展点：用 `@Activate(group=..., order=...)` + `META-INF/dubbo` SPI 文件，让日志过滤器自动挂到调用链上，无需在 application.properties 里显式声明。
- 在项目中的角色是"横切能力 jar"：`demo-springboot-client`（消费端）和 `demo-springboot-server`（提供端）都依赖它，因此一次编写、两端生效。
- 依赖关系：被 `demo-springboot-client`、`demo-springboot-server` 依赖；自身依赖 `dubbo-spring-boot-starter` 与 `dubbo-dependencies-zookeeper`（把 ZooKeeper/Curator 客户端传递出去）。
- 注意它**不是**接口模块：`DemoService` 不在本模块，client 与 server 各自写了一份同包同名的接口。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java | 1.8 | 编译级别（父 POM `java.version`） |
| Dubbo | 2.7.23 | 由父 POM 管理（`dubbo.version`，`dubbo-dependencies-bom` import） |
| dubbo-spring-boot-starter | 2.7.23 | 由父 POM 管理，引入 Dubbo 自动配置 |
| dubbo-dependencies-zookeeper（pom） | 2.7.23 | 由父 POM 管理，传递 ZooKeeper/Curator 客户端（已排除 log4j 绑定） |
| Spring Boot（传递的自动配置） | 2.7.18 | 由父 POM 管理，starter 依赖它 |
| SLF4J + Logback | 1.7.36 / 1.2.12（Boot BOM 管理） | 由父 POM（Boot BOM）管理，过滤器日志门面 |
| spring-boot-starter-test | 2.7.18 | 由父 POM 管理（无测试代码） |

## 3. 目录结构

```text
demo-springboot-dubbo/
├── pom.xml
├── mvnw / mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
└── src/main/
    ├── java/com/csfrez/demospringboot/filter/
    │   ├── ConsumerLoggingFilter.java
    │   └── ProviderLoggingFilter.java
    └── resources/META-INF/dubbo/
        └── com.alibaba.dubbo.rpc.Filter
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `ConsumerLoggingFilter` | `@Activate(group=CONSUMER, order=200000)`：调用前打印 `interface$method args`，拿到 `Result` 后打印返回值；用 URL 是否含 `MonitorService` 名称来跳过监控心跳 |
| `ProviderLoggingFilter` | `@Activate(group=PROVIDER, order=-200000)`：进入时打印 `RpcContext` 远端地址、接口、方法、入参，返回前打印结果 |
| `META-INF/dubbo/com.alibaba.dubbo.rpc.Filter` | SPI 注册文件，两个键 `consumerlogging`、`providerlogging` 分别指向上面两个类 |
| `pom.xml` | 仅声明 starter 与 zookeeper 依赖，无业务代码、无自动配置 |

## 5. 接口清单

本模块不对外提供 HTTP 接口，也不是 Dubbo 服务提供者。按扩展点清单列出实际能力：

| 扩展点 | 作用域 | Order | 说明 |
| --- | --- | --- | --- |
| `ConsumerLoggingFilter` | `CONSUMER` | 200000 | 消费端调用/返回日志，跳过 `MonitorService` 监控调用 |
| `ProviderLoggingFilter` | `PROVIDER` | -200000 | 提供端收/回日志，含远端 IP、接口名、入参与返回值 |
| SPI 文件 | `META-INF/dubbo/com.alibaba.dubbo.rpc.Filter` | — | 键 `consumerlogging` / `providerlogging`，无 XML、无 `@Bean` |

## 6. 配置说明

本模块没有 `application.properties` / `application.yml`（库模块），"配置"只有注解与 SPI 文件：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| SPI 文件名 | `com.alibaba.dubbo.rpc.Filter` | Dubbo 按"接口全限定名"扫描该文件，名字必须与扩展接口一致 |
| SPI 内容 | `consumerlogging`、`providerlogging` | 键名即扩展名，值为实现类全限定名 |
| `@Activate(group)` | `CONSUMER` / `PROVIDER` | 不写 group 的过滤器会对所有角色生效 |
| `@Activate(order)` | `200000` / `-200000` | order 越小越先执行，故提供端过滤器位于调用链前面 |

- 服务端口：不启动独立服务（无主类、无配置文件）
- 依赖中间件：无（自身不连 ZooKeeper，注册中心地址由 client/server 的 `dubbo.registry.address` 决定）

## 7. 启动方式

前置条件：JDK 8+、Maven；无需中间件。作为库安装到本地仓库：

```bash
# 在 demo-springboot 根目录执行
mvn -pl demo-springboot-dubbo -am install
```

没有主类，不能 `spring-boot:run`；装好后由 `demo-springboot-client` / `demo-springboot-server` 以 `<dependency>` 引入并在 Dubbo 调用时自动加载。

## 8. 调用示例

依赖引入（真实片段，来自 `demo-springboot-client/pom.xml`，版本由父 POM 提供）：

```xml
<dependency>
    <groupId>com.csfrez</groupId>
    <artifactId>demo-springboot-dubbo</artifactId>
</dependency>
```

使用侧的真实代码（filter 由 Dubbo SPI 自动织入，业务代码无需感知）：

```java
@DubboReference
private DemoService demoService;          // 消费端：调用时触发 ConsumerLoggingFilter
```

```java
@Service                                   // org.apache.dubbo.config.annotation.Service
public class DefaultDemoService implements DemoService {  // 提供端：被调用时触发 ProviderLoggingFilter
    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " (from Spring Boot), time is " + System.currentTimeMillis();
    }
}
```

## 9. 注意事项

- **SPI 文件名疑似失效**：文件名仍是 Alibaba 时代的 `com.alibaba.dubbo.rpc.Filter`，而项目用的是 Apache Dubbo 2.7.23，其过滤器扩展点全限定名是 `org.apache.dubbo.rpc.Filter`；Dubbo 只加载文件名与接口全名一致的文件，按此规则这两个过滤器不会被激活（日志中看不到"发送请求/接收请求"）。需改名为 `META-INF/dubbo/org.apache.dubbo.rpc.Filter` 后验证，别直接假设已生效。
- **日志覆盖全链路且打印入参**：模块一旦被依赖，所有调用都会在 INFO 级别打印完整参数与返回值，密码、token、大对象会进日志，同时带来序列化开销；生产环境应加开关或降到 DEBUG。
- **ZK 客户端被传递**：`dubbo-dependencies-zookeeper` 以 `pom` 类型引入，curator/zookeeper 会跟着进 client、server 的 classpath，可能与其它组件的 ZK 版本冲突。
- **接口双份定义**：`DemoService` 在 client、server 各有一份同包同名源码，本模块不提供接口 jar；两者靠"全限定名+方法签名一致"才能通信，改任一侧而另一侧未同步，运行期会抛 `NoSuchMethodError`/序列化异常。
- **异常被静默吞掉**：两个 filter 中打印日志的 `try/catch (Exception ignore)` 会掩盖序列化失败等问题，排查调用异常时不要只依赖这些日志。

---

[← 返回项目总览](../README.md)
