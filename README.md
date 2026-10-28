# demo-springboot

> 基于 Spring Boot 2.7.18 的多模块示例工程：20 个模块中 18 个可独立启动，覆盖数据访问、RPC、缓存、消息、实时通信、安全认证与工程实践。

## 1. 项目简介

| 项 | 说明 |
| --- | --- |
| Maven 坐标 | `com.csfrez:demo-springboot:2.0.1-SNAPSHOT` |
| 技术基线 | Java 8、Spring Boot 2.7.18、Maven 3.6+ |
| 模块数量 | 20（18 个可启动应用 + `dao`、`dubbo` 两个共享库） |
| 源码仓库 | <https://github.com/csfrez/demo-springboot> |

工程采用「父 POM 统一收口版本 + 子模块独立启动」的组织方式：根 [pom.xml](pom.xml) 通过 `dependencyManagement` 统一 Spring Boot、Dubbo、MyBatis、Hutool、Guava 等依赖版本；每个应用模块都有自己的 `@SpringBootApplication` 启动类和 `application.*`，可以单独运行、打包和部署。`demo-springboot-dao` 与 `demo-springboot-dubbo` 没有启动类，分别沉淀数据访问依赖和 Dubbo 日志过滤器，供 `server`、`client` 等模块复用。

> ⚠️ 本项目是学习与验证型示例集合：没有服务注册发现、配置中心、网关和统一鉴权，配置中存在大量内网地址与演示口令，**请勿直接用于生产环境**。

## 2. 模块总览

| 分类 | 模块 | 演示能力 | 端口 | 外部依赖 |
| --- | --- | --- | --- | --- |
| 基础 | [demo-springboot-common](demo-springboot-common/README.md) | 统一响应体、全局异常、Web 日志切面、防重复提交注解、CORS、Jackson 工具 | 7070 | 无 |
| 基础 | [demo-springboot-design-pattern](demo-springboot-design-pattern/README.md) | 责任链模式实现商品参数校验 | 8080 | 无 |
| 基础 | [demo-springboot-service](demo-springboot-service/README.md) | 服务编排：ServiceManager / ServiceExecutor / InstBuilder | 8080 | 无 |
| 数据访问 | [demo-springboot-dao](demo-springboot-dao/README.md) | 数据访问依赖聚合库（MyBatis、Druid、tk.Mapper、PageHelper、MySQL 驱动） | — | 无（由使用方提供数据源） |
| 数据访问 | [demo-springboot-mybatis-plus](demo-springboot-mybatis-plus/README.md) | MyBatis-Plus CRUD、字段自动填充、Druid、代码生成器 | 8080 | MySQL |
| 数据访问 | [demo-springboot-jsqlparser](demo-springboot-jsqlparser/README.md) | JSqlParser + MyBatis 拦截器改写 SQL（追加 `env` 环境隔离条件） | 8080 | MySQL |
| RPC 与监控 | [demo-springboot-dubbo](demo-springboot-dubbo/README.md) | Dubbo 公共库：消费端 / 提供端日志过滤器 | — | ZooKeeper（使用方） |
| RPC 与监控 | [demo-springboot-admin](demo-springboot-admin/README.md) | Spring Boot Admin 服务端（应用监控） | 9999 | 无 |
| RPC 与监控 | [demo-springboot-client](demo-springboot-client/README.md) | Admin Client + Dubbo 消费者 | 9090（管理 9091） | ZooKeeper |
| RPC 与监控 | [demo-springboot-server](demo-springboot-server/README.md) | 综合服务端：MyBatis/MySQL + MongoDB + Redis + Thymeleaf + Dubbo 提供者 + Admin Client | 8080（管理 8081） | MySQL、MongoDB、Redis、ZooKeeper |
| 缓存与并发 | [demo-springboot-redis](demo-springboot-redis/README.md) | RedisTemplate / Redisson 分布式锁（含 Lua 脚本） | 8080 | Redis |
| 缓存与并发 | [demo-springboot-thread](demo-springboot-thread/README.md) | 自定义线程池、拒绝策略、线程池监控与告警 | 8080 | 无 |
| 缓存与并发 | [demo-springboot-retry](demo-springboot-retry/README.md) | Spring Retry：`@Retryable` / `@Recover` / RetryEvent 监听 | 8080 | 无 |
| 安全与认证 | [demo-springboot-security](demo-springboot-security/README.md) | AES / RSA 加解密与主板序列号机器码 | 8080 | 无 |
| 安全与认证 | [demo-springboot-token](demo-springboot-token/README.md) | Sa-Token 登录、权限 / 角色、会话与强制下线 | 8080 | Redis |
| 文件处理 | [demo-springboot-file](demo-springboot-file/README.md) | 上传 / 下载 / 预览、图片压缩、图片与 PDF 互转 | 8080 | 本地磁盘 |
| 文件处理 | [demo-springboot-download](demo-springboot-download/README.md) | concept-download：下载、压缩、缓存 | 8080 | 本地磁盘 |
| 实时通信 | [demo-springboot-websocket](demo-springboot-websocket/README.md) | 原生 Netty WebSocket + 握手鉴权 + Redis/RabbitMQ 推送骨架 | 8080 / WS 8888 | Redis、RabbitMQ |
| 实时通信 | [demo-springboot-ws](demo-springboot-ws/README.md) | netty-websocket-spring-boot-starter + RabbitMQ/Redis 广播 | 8080 / WS 8888 | Redis、RabbitMQ |
| AI | [demo-springboot-ai](demo-springboot-ai/README.md) | ai4j 接入 DeepSeek：同步对话与流式调用（增量写日志） | 8080 | DeepSeek API Key |

端口为各模块 `application.*` 中的默认值。18 个 Web 模块里除 `common`（7070）、`client`（9090）、`admin`（9999）外，其余 **15 个模块默认都占用 8080**，同时启动前请按需覆盖端口（见 5.4）。`websocket` 与 `ws` 的 WebSocket 端口同为 8888，也不能同时启动。

## 3. 模块依赖与运行拓扑

```mermaid
flowchart TB
  subgraph MON["监控与 RPC 示例"]
    ADMIN["demo-springboot-admin<br/>:9999<br/>Spring Boot Admin Server"]
    CLIENT["demo-springboot-client<br/>:9090<br/>Admin Client + Dubbo Consumer"]
    SERVER["demo-springboot-server<br/>:8080<br/>Dubbo Provider + Admin Client"]
    DUBBO["demo-springboot-dubbo<br/>（共享库）"]
    DAO["demo-springboot-dao<br/>（共享库）"]
  end

  CLIENT -- "注册 / 心跳" --> ADMIN
  SERVER -- "注册 / 心跳" --> ADMIN
  CLIENT -- "依赖" --> DUBBO
  SERVER -- "依赖" --> DUBBO
  SERVER -- "依赖" --> DAO
  CLIENT -. "Dubbo 注册 / 订阅" .-> ZK[("ZooKeeper<br/>10.67.7.1:2181")]
  SERVER -. "Dubbo 注册 / 订阅" .-> ZK
  DAO --> MYSQL[("MySQL<br/>country 表")]
  SERVER --> MONGO[("MongoDB")]
  SERVER --> REDIS[("Redis")]
  CLIENT -- "Dubbo RPC：DemoService.sayHello" --> SERVER
```

其余模块彼此独立、可单独启动：`common`、`design-pattern`、`service`、`mybatis-plus`、`jsqlparser`、`redis`、`thread`、`retry`、`security`、`token`、`file`、`download`、`websocket`、`ws`、`ai`。

### 3.1 Dubbo 示例链路

1. 启动 ZooKeeper（默认 `10.67.7.1:2181`，按需修改 `client` / `server` 的 `dubbo.registry.address`）。
2. 启动 `demo-springboot-server`：`DefaultDemoService` 以 `@Service` 暴露 `DemoService.sayHello`。
3. 启动 `demo-springboot-client`：`DemoConsumerController` 通过 `@DubboReference` 注入代理，访问 `GET /sayHello?name=xxx` 验证调用。

### 3.2 Spring Boot Admin 监控

1. 启动 `demo-springboot-admin`（9999）。
2. 启动 `client` 或 `server`，二者配置了 `spring.boot.admin.client.url=http://127.0.0.1:9999`，启动后即可在 Admin 控制台看到实例与端点信息。

## 4. 技术栈

| 组件 | 版本 | 主要使用模块 |
| --- | --- | --- |
| Java | 1.8 | 全部 |
| Spring Boot | 2.7.18 | 全部 |
| Spring Boot Admin | 2.7.16 | admin、client、server |
| MyBatis Spring Boot Starter | 2.3.2 | server、jsqlparser、dao |
| tk.mybatis mapper | 4.3.0 | server、dao |
| PageHelper | 2.1.0 | server、dao |
| Druid | 1.2.24 | server、mybatis-plus、dao |
| MySQL Connector/J | 8.4.0 | server、mybatis-plus、jsqlparser、dao |
| MyBatis-Plus | 3.5.12 | mybatis-plus |
| JSqlParser | 4.6 | jsqlparser |
| Apache Dubbo | 2.7.23 | dubbo、client、server |
| Sa-Token | 1.44.0 | token |
| Redisson | 3.17.7 | redis |
| Netty | 4.1.121.Final | websocket |
| netty-websocket-spring-boot-starter | 0.12.0 | ws |
| Apache PDFBox | 2.0.33 | file |
| Thumbnailator | 0.4.20 | file |
| ai4j | 1.4.3 | ai |
| Hutool | 5.8.33 | 多个模块 |
| Guava | 33.3.0-jre | common、file、design-pattern |
| concept-download | 2.1.0 | download |
| spel-validator-javax | 0.5.0-beta | common |

## 5. 快速开始

### 5.1 环境要求

| 组件 | 要求 | 说明 |
| --- | --- | --- |
| JDK | 1.8 | 根 POM `java.version=1.8`；更高版本 JDK 可编译，但依赖仍按 1.8 目标 |
| Maven | 3.6+ | 根目录没有 `mvnw`；`admin`、`client`、`dao`、`dubbo`、`jsqlparser`、`security`、`server` 自带 wrapper（3.5.4） |
| 中间件 | 按模块 | 见第 6 节依赖矩阵 |

### 5.2 构建

```bash
git clone git@github.com:csfrez/demo-springboot.git
cd demo-springboot
mvn clean install -DskipTests
```

- 建议加 `-DskipTests`：部分测试依赖真实 Redis/MySQL；`ai`、`token` 的测试类包名（`com.csfrez.ws`）与启动类不一致，默认执行会因找不到 `@SpringBootConfiguration` 而失败。
- 仅 `ai`、`redis`、`token`、`ws` 绑定了 `spring-boot-maven-plugin:repackage`，产物是可执行 jar；其余模块生成普通 jar，需用 `spring-boot:run` 或 IDE 运行主类。
- 多数模块未固定 `spring-boot-maven-plugin` 版本（如 `admin`、`client`、`common`、`design-pattern`、`download`、`file`、`retry`、`service`、`thread`、`websocket`），Maven 可能解析到 3.x 插件；建议在 IDE 中运行，或为这些模块补 `<version>${spring-boot.version}</version>`。

### 5.3 启动单个模块

```bash
# 先完成一次全量构建，把 dao / dubbo 等共享库安装到本地仓库
mvn clean install -DskipTests

cd demo-springboot-redis
mvn spring-boot:run
```

`jsqlparser` 与 `mybatis-plus` 的 POM 未声明 `spring-boot-maven-plugin`，`mvn spring-boot:run` 无法解析 `spring-boot` 前缀。请用 IDE 运行主类，或显式指定插件：

```bash
mvn -pl demo-springboot-jsqlparser org.springframework.boot:spring-boot-maven-plugin:2.7.18:run
mvn -pl demo-springboot-mybatis-plus org.springframework.boot:spring-boot-maven-plugin:2.7.18:run
```

### 5.4 并行启动与端口覆盖

```bash
# 方式一：覆盖 Spring Boot 端口
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=18081

# 方式二：可执行 jar 直接传参（仅 ai / redis / token / ws）
java -jar demo-springboot-redis/target/demo-springboot-redis-2.0.1-SNAPSHOT.jar --server.port=18081

# 方式三：环境变量
SERVER_PORT=18081 mvn spring-boot:run                  # bash
$env:SERVER_PORT=18081; mvn spring-boot:run           # PowerShell
```

需要修改配置中的中间件地址时，直接编辑对应模块的 `src/main/resources/application.properties` 或 `application.yml`。

### 5.5 最小验证

```bash
curl http://localhost:7070/hello          # common：统一响应示例
curl http://localhost:8080/hello          # retry / thread / security 等模块的存活探针
curl http://localhost:8080/redis/demo     # redis：分布式锁（需 Redis）
```

## 6. 中间件与配置

### 6.1 依赖矩阵

| 模块 | MySQL | Redis | MongoDB | ZooKeeper | RabbitMQ | 其他 |
| --- | --- | --- | --- | --- | --- | --- |
| common | — | — | — | — | — | — |
| design-pattern | — | — | — | — | — | — |
| service | — | — | — | — | — | — |
| dao | 驱动依赖 | — | — | — | — | 由使用方配置数据源 |
| mybatis-plus | 需要 | — | — | — | — | — |
| jsqlparser | 需要 | — | — | — | — | — |
| dubbo | — | — | — | 使用方需要 | — | — |
| admin | — | — | — | — | — | — |
| client | — | — | — | 需要 | — | — |
| server | 需要 | 需要 | 需要 | 需要 | — | — |
| redis | — | 需要 | — | — | — | — |
| thread | — | — | — | — | — | — |
| retry | — | — | — | — | — | — |
| security | — | — | — | — | — | — |
| token | — | 需要 | — | — | — | — |
| file | — | — | — | — | — | 本地磁盘 |
| download | — | — | — | — | — | 本地磁盘 |
| websocket | — | 需要 | — | — | 需要 | — |
| ws | — | 需要 | — | — | 需要 | — |
| ai | — | — | — | — | — | DeepSeek API Key |

### 6.2 默认地址与口令

以下均为代码/配置中的演示值，换环境时必须修改：

| 依赖 | 默认地址 / 账号 | 出现模块 | 配置键（示例） |
| --- | --- | --- | --- |
| MySQL | `10.11.0.142:3306/demo`，`root/123456` | jsqlparser、mybatis-plus | `spring.datasource.url` |
| MySQL | `10.11.0.136:3306/test`，`root/123456` | server | `spring.datasource.url` |
| Redis | `10.11.0.136:6379`（无密码） | redis、token、websocket、ws | `spring.redis.host`、`sa-token.alone-redis.*` |
| Redis | `10.67.1.33:6379`，`database=4` | server | `spring.redis.*` |
| MongoDB | `mongodb://opengate:123456@10.1.108.122:27017/opengate_sit` | server | `spring.data.mongodb.uri` |
| ZooKeeper | `10.67.7.1:2181` | client、server | `dubbo.registry.address` |
| RabbitMQ | `10.11.0.136:5672`，`admin/admin123` | websocket、ws | `spring.rabbitmq.*` |
| Admin Server | `http://127.0.0.1:9999` | admin、client、server | `spring.boot.admin.client.url` |
| 文件下载前缀 | `http://10.96.2.190:8080/api/file/download/` | file | `file.download.base-url` |
| Druid 控制台 | `druid/druid@123` | server、mybatis-plus | `spring.datasource.druid.stat-view-servlet.*` |
| DeepSeek | `api-key: ""`（空） | ai | `ai.deepseek.api-key` |

### 6.3 启动前检查清单

1. 中间件已按 6.1 启动，且对应模块配置已改为本机/可用地址。
2. 数据库已建表：`server` 需要 `country` 表，`mybatis-plus` 需要 `user` 表（建表脚本见 `demo-springboot-mybatis-plus/src/test/resources/sql/user.sql`）；`jsqlparser` 只配置了数据源、没有 Mapper，当前无需建表。
3. `server` 的 `CountryController` 要求 `pageNum`、`pageSize` 必填，否则返回 400。
4. `ai` 需先配置有效的 `ai.deepseek.api-key`，否则 `/chat` 调用失败。
5. `file` 的 `file.storage.location=./file-storage` 为相对路径，首次上传时需保证工作目录可写。

## 7. 代码生成与版本管理

### 7.1 MyBatis Generator（server 模块）

- 配置文件：`demo-springboot-server/src/main/resources/generator/generatorConfig.xml`（读取同模块 `application.properties` 的数据源，目标表为 `country`）。
- 生成内容：`com.csfrez.demospringboot.model.Country`、`mapper.CountryMapper`、`resources/mapper/CountryMapper.xml`。
- 执行命令（在 `demo-springboot-server` 目录下，或在根目录用 `-pl`）：

```bash
# 解决 Windows 中文乱码
set MAVEN_OPTS=-Dfile.encoding=UTF-8          # cmd
$env:MAVEN_OPTS="-Dfile.encoding=UTF-8"       # PowerShell

mvn mybatis-generator:generate
# 或在项目根目录
mvn -pl demo-springboot-server mybatis-generator:generate
```

> 注意：生成器插件把 `mysql:mysql-connector-java` 固定为 `5.1.47`，而 `application.properties` 使用 `com.mysql.cj.jdbc.Driver`（8.x 驱动类），直接执行会因插件类路径中找不到该类而失败。可将插件依赖换成 `com.mysql:mysql-connector-j:8.4.0`，或把驱动类临时改为 5.1.x 的 `com.mysql.jdbc.Driver`。

### 7.2 MyBatis-Plus 代码生成器

入口：`demo-springboot-mybatis-plus/src/test/java/com/csfrez/mybatis/CodeGenerator.java`（`FastAutoGenerator`，读取 `10.11.0.142:3306/demo` 的 `user` 表）。

- 在 IDE 中直接运行 `main` 方法；输出目录按 `${user.dir}/demo-springboot-mybatis-plus/src/main/java` 计算，因此**建议把工作目录设为仓库根目录**，否则会生成嵌套目录。
- 自定义 Service 模板位于 `demo-springboot-mybatis-plus/src/test/resources/templates/service.java.ftl`。

### 7.3 版本升级

```bash
mvn versions:set -DnewVersion=2.0.2-SNAPSHOT
mvn versions:commit
# 放弃修改
mvn versions:revert
```

## 8. 开发约定

- 包名按模块域划分：`com.csfrez.demospringboot`（基础/服务端）、`com.csfrez.redis`、`com.csfrez.token`、`com.csfrez.ws`、`com.csfrez.mybatis`、`com.csfrez.svr` 等。
- 新增模块时同步修改根 `pom.xml` 的 `<modules>`、根 README 的模块表，并在模块根目录提供 README。
- 端口、数据源、中间件地址统一放在模块的 `application.properties` / `application.yml`，不要写进代码；当前示例中的硬编码仅用于演示。
- 公共依赖版本优先在根 POM 的 `dependencyManagement` 中收口；模块内只声明 `groupId` / `artifactId`。
- 提交前至少保证 `mvn clean install -DskipTests` 通过；涉及中间件的模块请在 README 中注明前置条件。

## 9. 已知问题与注意事项

1. **端口冲突**：15 个模块默认使用 8080，`websocket` 与 `ws` 的 WebSocket 端口同为 8888，默认无法同时启动，需通过 `--server.port` / `websocket.port` / `ws.port` 覆盖。
2. **配置硬编码**：数据源、Redis、MongoDB、ZooKeeper、RabbitMQ 地址与口令均为内网演示值（见 6.2），其中多处使用弱口令。
3. **`jsqlparser` 端口写法无效**：`application.yml` 把 `port: 8080` 写在 `spring.server` 下，正确位置应为 `server.port`；当前只是恰好与默认端口一致。
4. **`server` 别名包配置不一致**：`mybatis.type-aliases-package=com.csfrez.springboot.model`，实际模型包为 `com.csfrez.demospringboot.model`；目前未使用别名，暂不影响运行。
5. **插件与打包**：`jsqlparser`、`mybatis-plus` 未声明 `spring-boot-maven-plugin`；多数模块未固定插件版本；仅 4 个模块绑定 `repackage`。命令行运行时请固定版本或改用 IDE。
6. **测试类包名错误**：`demo-springboot-ai`、`demo-springboot-token` 的测试类位于 `com.csfrez.ws`，与启动类包不一致，`mvn test` 会失败；请按需修正包名或跳过测试。
7. **配置键已废弃**：`spring.redis.*` 自 Spring Boot 2.4 起建议改为 `spring.data.redis.*`，当前版本仍兼容，升级 3.x 前需迁移。
8. **`security` 与 Spring Security 无关**：该模块只演示 AES/RSA 与机器码采集，没有引入 `spring-boot-starter-security`。
9. **`common` 的 `@SubmitLimit` 目前空转**：切面中的 Redis 加锁逻辑被注释，注解参数不生效。
10. **WebSocket 演示链路不完整**：`websocket` 的握手鉴权为硬编码 token `123456`，RabbitMQ 推送链路在默认装配下未闭环；两个 WebSocket 模块均不适合直接当生产模板。
11. **中文注释乱码**：部分 `application.*` 与 Java 注释是 GBK 历史编码，UTF-8 下显示为乱码，不影响运行；本套 README 已按语义重新描述。
12. **仓库配套**：无根级 `mvnw`、LICENSE、CI 与 Dockerfile；根目录 `IFLOW.md` 是早期的项目说明，细节以本 README 与各模块 README 为准。

## 10. 模块文档索引

- 基础与工程实践：[common](demo-springboot-common/README.md)、[design-pattern](demo-springboot-design-pattern/README.md)、[service](demo-springboot-service/README.md)
- 数据访问：[dao](demo-springboot-dao/README.md)、[mybatis-plus](demo-springboot-mybatis-plus/README.md)、[jsqlparser](demo-springboot-jsqlparser/README.md)
- RPC 与监控：[admin](demo-springboot-admin/README.md)、[dubbo](demo-springboot-dubbo/README.md)、[client](demo-springboot-client/README.md)、[server](demo-springboot-server/README.md)
- 缓存与并发：[redis](demo-springboot-redis/README.md)、[thread](demo-springboot-thread/README.md)、[retry](demo-springboot-retry/README.md)
- 安全与认证：[security](demo-springboot-security/README.md)、[token](demo-springboot-token/README.md)
- 文件处理：[file](demo-springboot-file/README.md)、[download](demo-springboot-download/README.md)
- 实时通信：[websocket](demo-springboot-websocket/README.md)、[ws](demo-springboot-ws/README.md)
- AI：[ai](demo-springboot-ai/README.md)
