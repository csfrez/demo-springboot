# demo-springboot-ws

> 用 `netty-websocket-spring-boot-starter` 的 `@ServerEndpoint` 在 8888 暴露 `/socket/{token}`，以 Redis 维护“用户→会话”索引、RabbitMQ 队列做消息分发。

## 1. 模块定位

本模块是注解式 WebSocket 示例，**不手写 Netty 启动代码**：在 POJO 上标注 `@ServerEndpoint`，由 starter（org.yeauty）扫描并启动独立的 Netty 服务端，与 Spring Boot 内嵌 Tomcat 并存。

- HTTP 8080：Spring MVC，提供 `/hello`、`/send`、`/count` 与 `static/demo.html` 测试页。
- WebSocket 8888：注解中 `port = "${ws.port}"`，来自 application.yml 的 `ws.port: 8888`。
- 连接以路径变量 `token` 标识用户：`ConnectionService` 维护本地 `Session` 表，并在 Redis 写 `userId:<id>` 会话索引；业务消息统一投递 `websocket_queue`，由 `MessageReceiverListener` 按用户推送，便于多实例解耦。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 2.7.18 | 父工程统一版本，Web/MVC 与自动装配 |
| Java | 8 | 根 pom `java.version=1.8` |
| netty-websocket-spring-boot-starter | 0.12.0 | 提供 `@ServerEndpoint`/`@OnMessage` 等注解与内嵌 Netty 容器（Netty 版本由 starter 传递） |
| spring-boot-starter-data-redis | 2.7.18 | `RedisTemplate<String, List<String>>` 保存会话索引 |
| spring-boot-starter-amqp | 2.7.18 | `RabbitTemplate` 发送、`@RabbitListener` 消费 |
| Hutool | 5.8.33 | `Pair`、`StrUtil`、`CollUtil`、`JSONUtil` |
| Lombok | 1.18.30（Boot BOM 管理） | `@Slf4j`、`@Data`、`@AllArgsConstructor` |

## 3. 目录结构

```text
demo-springboot-ws/
├── pom.xml
└── src/
    ├── main/java/com/csfrez/ws/    # WsApplication、rest、handler、service、listener、config、model
    ├── main/resources/             # application.yml、static/demo.html、static/index.html
    └── test/java/com/csfrez/ws/WsiApplicationTests.java
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `WsApplication` | 启动类，`@EnableWebSocket`（`org.yeauty.annotation`）启用 starter 的注解端点扫描 |
| `handler/WebSocketHandler` | `@ServerEndpoint(path="/socket/{token}", port="${ws.port}")`；`@BeforeHandshake` 校验 token，`@OnOpen` 注册会话，`@OnMessage` 处理 PING/PONG/CLOSED 与业务文本，`@OnBinary` 原样回写，`@OnEvent` 处理空闲事件 |
| `service/ConnectionService` | 本地 `ConcurrentHashMap<sessionId, Session>` 与 `<sessionId, userId>`；Redis key `userId:<userId>` 存 `List<sessionId>`，`openSession`/`closeSession` 成对维护 |
| `service/MessageSenderService` | 以 JSON 消息属性把 `MyMessage` 发到指定队列（`rabbitTemplate.send(queueName, msg)`，走默认交换机） |
| `listener/MessageReceiverListener` | 监听 `websocket_queue`，按 `userId` 从 Redis 取 sessionId 列表，再在本地 `Session` 表找活跃连接并 `sendText`，失效项顺带清理 |
| `listener/GracefulShutdownListener` | 监听 `ContextClosedEvent`，停机前把本机 sessionId 从 Redis 列表移除并删除空的 `userId:<id>` |
| `rest/WebSocketController` | 三个调试接口：`/hello` 向指定 sessionId 发消息、`/send` 投递 MQ、`/count` 打印当前会话 |
| `config/RabbitMqConfig` / `config/RedisConfig` | 声明持久化队列 `websocket_queue` 与 `Jackson2JsonMessageConverter`；定制 `RedisTemplate<String, List<String>>`（key 用 String、value 用 `GenericJackson2JsonRedisSerializer`） |
| `model/MyMessage` | `userId` + `content` 消息体，实现 `Serializable` |

## 5. 接口清单

**HTTP 接口**

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/hello` | `id`（必填，Session ID，非 token/userId） | 找到该 Session 则 `sendText("hello world ==> " + id + ", " + 时间戳)`，并把同一字符串作为响应返回 |
| GET | `/send` | `userId`（必填，即连接的 token） | 组装 `MyMessage(userId, "hello world ==> <userId>, <时间戳>")` 投递 `websocket_queue`，由监听器推送给该用户所有会话；HTTP 无响应体 |
| GET | `/count` | 无 | 打印本地 Session 表与 sessionId→userId 映射，固定返回 `ok`（不返回数量） |

**WebSocket 连接地址**

| 连接地址 | 协议 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| `ws://localhost:8888/socket/123456?gray=0` | WebSocket（`@ServerEndpoint`，starter 内嵌 Netty） | 路径变量 `token` 必须等于 `123456`，否则 `@BeforeHandshake` 中 `session.close()`；`gray` 为 `@RequestParam` 查询参数 | 连接后以 token 作为 userId 建索引；`PING` 得 `PONG`，`CLOSED` 主动关闭，其他文本追加 ` from server <时间戳>` 后经 MQ 推回 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | 8080 | Spring Boot HTTP 端口 |
| `ws.port` | 8888 | 注入 `@ServerEndpoint(port=...)`，WebSocket 独立监听端口 |
| `ws.readerIdleTimeSeconds` | 60 | 读空闲阈值；`@OnEvent` 的 `READER_IDLE` 分支目前只打日志 |
| `ws.writerIdleTimeSeconds` | 120 | 写空闲阈值；`WRITER_IDLE` 时服务端主动 `sendText("PING")` |
| `spring.rabbitmq.host` / `port` | 10.11.0.136 / 5672 | RabbitMQ 地址 |
| `spring.rabbitmq.username` / `password` | admin / admin123 | RabbitMQ 凭据 |
| `spring.rabbitmq.listener.simple.idle-event-interval` | 30s | 消费者空闲事件上报间隔 |
| `spring.redis.host` / `port` | 10.11.0.136 / 6379 | Redis 地址，存 `userId:<userId>` → `List<sessionId>` |

- 服务端口：HTTP 8080 / WebSocket 8888
- 依赖中间件：Redis（6379，key `userId:<userId>`）、RabbitMQ（5672，队列 `websocket_queue`；未声明自定义交换机，发送走默认交换机 `""`，路由键即队列名）

## 7. 启动方式

前置条件：JDK 8、Maven 3.6+；Redis 与 RabbitMQ 必须可连（默认 10.11.0.136，账号 `admin/admin123`），或先改 `application.yml`。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-ws
mvn spring-boot:run
```

启动后 WebSocket 监听 8888；浏览器打开 `http://localhost:8080/demo.html` 即为内置客户端（固定连 `ws://localhost:8888/socket/123456?gray=0`，每 60 秒发一次 `PING`）。

## 8. 调用示例

```bash
# 1) WebSocket：token 必须为 123456；PING 回 PONG，hello 回 "hello from server <时间戳>"
npx wscat -c "ws://localhost:8888/socket/123456?gray=0"
# 2) 向指定 userId（等于连接时的 token）推送，内容由服务端生成
curl "http://localhost:8080/send?userId=123456"
# 3) 直接向某个 Session 发消息；id 是 @OnOpen 日志里的 session.id()，不是 token
curl "http://localhost:8080/hello?id=<sessionId>"
# 4) 打印当前会话表（返回 ok）
curl "http://localhost:8080/count"
```

浏览器端最小示例（与 `demo.html` 一致）：

```javascript
const ws = new WebSocket("ws://localhost:8888/socket/123456?gray=0");
ws.onopen = () => ws.send("PING");
ws.onmessage = e => console.log("recv:", e.data);
```

## 9. 注意事项

1. **端口冲突**：本模块与 `demo-springboot-websocket` 的 WebSocket 端口同为 **8888**（HTTP 也同为 8080），二者不能同时启动；同机联调请改其一（`ws.port` 或 `websocket.port`）。
2. 鉴权是硬编码：`@BeforeHandshake` 仅比较路径变量 `token` 与 `"123456"`；`@RequestParam gray` 未声明默认值，连接时建议始终带上 `?gray=0`，否则可能握手失败。
3. 消息回环固定用户：`@OnMessage` 投递 MQ 时写死 `new MyMessage("123456", message)`，因此只有 `token=123456` 的连接能收到回推，其他 token 的消息会被推给 123456。
4. 跨节点推送不完整：`MessageReceiverListener` 从 Redis 取到的是**全集群** sessionId，但 `Session` 只存在本机 `sessionMap`，其他节点的 sessionId 会因 `session == null` 被当作失效项从 Redis 删除。
5. `/hello` 的 `id` 是 Session ID（`session.id().toString()`），不是 token 也不是 userId；`/count` 名为 count 却只打印日志并返回 `ok`。
6. 心跳为服务端单向 PING：`WRITER_IDLE`（120 秒）触发 `sendText("PING")`，客户端需回 `PONG`（demo.html 已实现）；`READER_IDLE` 只记日志、不断开。
7. Redis 用 `GenericJackson2JsonRedisSerializer` 序列化 `List<String>`，redis-cli 看到的是带类型 JSON；`application.yml` 中间件地址为内网 10.11.0.136，换环境需同步修改。

---

[← 返回项目总览](../README.md)
