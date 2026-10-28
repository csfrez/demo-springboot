# demo-springboot-admin

> 基于 Spring Boot Admin 的监控管理台：一行 `@EnableAdminServer` 得到可视化面板，同时以 client 身份把自己注册进去。

## 1. 模块定位

- 演示 SBA Server 的搭建：引入 server starter + `@EnableAdminServer`，不用写任何 Controller。
- 项目中的"监控入口"：`demo-springboot-client`、`demo-springboot-server` 的 `spring.boot.admin.client.url` 都指向本模块的 9999 端口。
- 依赖关系：不依赖任何业务模块，是叶子模块；被监控方通过 SBA Client starter 反向注册到它，本模块不感知对方存在。
- 特殊性：pom 同时引入 server 与 client starter，且 client.url 指向自身 9999，启动后会"自注册"，在面板里看到自己。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java | 1.8 | 编译级别（父 POM `java.version`，compiler 3.8.1） |
| Spring Boot | 2.7.18 | 由父 POM 管理（`spring-boot-dependencies` import） |
| spring-boot-admin-starter-server | 2.7.16 | 由父 POM 管理（`spring-boot-admin-dependencies` 2.7.16） |
| spring-boot-admin-starter-client | 2.7.16 | 由父 POM 管理，用于自注册 |
| Actuator / Web | 2.7.18 | 由 SBA starter 传递引入 |
| spring-boot-starter-test | 2.7.18 | 由父 POM 管理（无测试类） |

## 3. 目录结构

```text
demo-springboot-admin/
├── pom.xml
├── mvnw｜mvnw.cmd｜.mvn/wrapper/
└── src/main/
    ├── java/com/csfrez/demospringboot/DemoSpringbootAdminApplication.java
    └── resources/application.properties
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `DemoSpringbootAdminApplication` | 启动类；`@SpringBootApplication` + `@EnableAdminServer`，开启内嵌管理台与实例注册中心 |
| `application.properties` | 唯一配置：应用名、9999 端口、自注册地址、放开全部 Actuator 端点 |
| `spring-boot-admin-starter-server` | 提供管理台 UI、`/applications` REST API、实例状态轮询与通知（非本仓库代码） |

## 5. 接口清单

本模块不对外提供业务 HTTP 接口（没有自定义 Controller），只有 SBA/Actuator 内置端点：

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/` | — | SBA 管理台首页（应用墙），由 server starter 提供 |
| GET | `/applications` | — | SBA REST：已注册应用列表（JSON） |
| GET | `/applications/{name}` | path：应用名 | 指定应用的实例列表 |
| GET | `/instances/{id}` | path：实例 id | 单实例详情与端点信息 |
| GET | `/actuator/*` | — | 本应用自身端点，因 `exposure.include=*` 全部开放 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.application.name` | `demo-springboot-admin` | 自身在被管列表中的显示名 |
| `server.port` | `9999` | 管理台端口，也是 Actuator 端口（未配 `management.server.port`） |
| `spring.boot.admin.client.url` | `http://127.0.0.1:9999` | SBA Client 注册地址，此处指向自己 |
| `management.endpoints.web.exposure.include` | `*` | 暴露全部 Web 端点 |

- 服务端口：9999（管理台与 Actuator 同端口）
- 依赖中间件：无（本模块自己就是被注册的中心；被管应用需主动上报）

## 7. 启动方式

前置条件：JDK 8+、Maven 3.6+（或直接用自带的 `mvnw`），无需任何中间件。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-admin
mvn spring-boot:run
```

本模块 pom 只声明了 `spring-boot-maven-plugin`，既没有 `<executions>` 绑定 `repackage`，也未继承 `spring-boot-starter-parent`，因此 `package` 产出的 `target/demo-springboot-admin.jar` 只是普通 jar（无 `Main-Class`、无 `BOOT-INF`），不能 `java -jar` 启动；请用上面的 `spring-boot:run`，或在 IDE 中运行 `DemoSpringbootAdminApplication`。启动后访问 <http://127.0.0.1:9999>。

## 8. 调用示例

```bash
curl -s http://127.0.0.1:9999/ -o sba.html      # 管理台页面
curl -s http://127.0.0.1:9999/applications      # 已注册应用列表（JSON）
curl -s http://127.0.0.1:9999/actuator/health   # 自身健康检查
```

被监控方需要的最小配置（与 client/server 模块中的真实配置一致）：

```properties
spring.boot.admin.client.url=http://127.0.0.1:9999
management.endpoints.web.exposure.include=*
```

## 9. 注意事项

- **自注册**：server + client starter 同时存在、url 又指向 9999，面板里会出现名为 `demo-springboot-admin` 的实例在监控自己；不是故障，但会干扰观察真实被管应用。
- **完全无鉴权**：pom 未引入 `spring-boot-starter-security`，也没有 `spring.security.*`，9999 上的管理台与 `/actuator/*` 全部匿名可访问；`exposure.include=*` 还放开了 `env`、`heapdump`、`loggers` 等敏感端点，切勿照搬到公网或生产。
- **单端口**：未设置 `management.server.port`，管理台与端点共用 9999；而 client/server 分别另开 9091/8081，混用时容易把管理端口当业务端口。
- **版本绑定**：SBA 2.7.16 属于 Boot 2.7.x 兼容线，由父 POM 的 `spring-boot-admin-dependencies` 管理；升到 Boot 3.x 必须同步换 SBA 3.x，否则启动即 NoSuchMethodError。
- **无测试**：声明了 `spring-boot-starter-test` 但不存在 `src/test`，`-DskipTests` 与否结果一样。

---

[← 返回项目总览](../README.md)
