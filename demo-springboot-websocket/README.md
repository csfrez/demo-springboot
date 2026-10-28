# demo-springboot-websocket

> 在 Spring Boot 进程内旁路启动原生 Netty WebSocket 服务（8888），演示握手鉴权、空闲心跳与 Redis/RabbitMQ 跨节点推送骨架。

## 1. 模块定位

本模块不使用 Spring 的 WebSocket 能力（无 `spring-boot-starter-websocket`、无 `@ServerEndpoint`、无 `WebSocketConfigurer`），而是在 Spring Boot 进程里用 `ServerBootstrap` 另起一个 Netty 监听端口，与内嵌 Tomcat 并存：

- HTTP 端口 8080：仅承载 Spring 容器，**本模块没有任何 Controller**；
- WebSocket 端口 8888：由 `WebSocketServer` 在 `@PostConstruct` 中异步启动。

代码库保留了 4 套 Netty 启动/初始化实现和 handler 包内 10 个类，实际生效的只有 `WebSocketServer` → `WebSocketInitializer` → `ObjectHandshakeHandler` → `ObjectWebSocketHandler` 这一条链路（其余为历史/备用代码，见第 9 节）。定位是“原生 Netty 接入层”教学示例：pipeline 编排、握手后重建 pipeline、token 校验、空闲断连，以及 Redis 保存连接、RabbitMQ 多实例广播的骨架设计。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 2.7.18 | 父工程统一版本，内嵌 Tomcat 与自动装配 |
| Java | 8 | 根 pom `java.version=1.8` |
| Netty | 4.1.121.Final | `netty-all`，原生 WebSocket 服务端与 HTTP/WS 编解码 |
| spring-boot-starter-web | 2.7.18 | 提供 8080 HTTP 容器（本模块无接口） |
| spring-boot-starter-data-redis | 2.7.18 | `RedisTemplate`，用于 token 校验与连接映射的设想 |
| spring-boot-starter-amqp | 2.7.18 | `RabbitTemplate` / `@RabbitListener` 广播 |
| jackson-databind | 2.13.5（Boot BOM 管理） | JSON 解析、RabbitMQ 消息转换 |
| Lombok | 1.18.30（Boot BOM 管理） | `@Slf4j`、`@Data`、`@RequiredArgsConstructor` |

## 3. 目录结构

```text
demo-springboot-websocket/
├── pom.xml
└── src/main/
    ├── java/com/csfrez/ws/    # WebSocketApplication、netty、handler、servie、listener、rabbit、config、model
    └── resources/application.yml
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `netty/WebSocketServer` | **实际生效的启动器**（`@Component`）：绑定 `${websocket.port:8888}`，bossGroup 1 线程 + workerGroup，`@PreDestroy` 优雅关闭 |
| `netty/WebSocketInitializer` | **实际生效的 pipeline**：`HttpServerCodec` → `HttpObjectAggregator(65536)` → `ObjectHandshakeHandler` → `IdleStateHandler(30,0,0)` |
| `handler/ObjectHandshakeHandler` | 从 URI 取 `token` 与硬编码常量 `123456` 比对，失败返回 401；成功后手动 `handshake()` 并移除 HTTP 处理器、挂载 `ObjectWebSocketHandler` |
| `handler/ObjectWebSocketHandler` | 业务帧处理：Close 关闭、Ping 回 Pong、文本回显 `Echo: <原文>`；收到 `IdleStateEvent` 直接断开 |
| `servie/ConnectionService` | 本地连接表 `ConcurrentHashMap<token, Channel>`，提供 add/remove/get |
| `servie/TokenService` | 原设计的 Redis token 校验，方法体已改为恒返回 `true`（Redis 代码被注释） |
| `servie/MessageSenderService` | 组装 JSON 消息并经 `RabbitTemplate.send(queue, msg)` 发到指定队列（默认交换机） |
| `servie/RabbitMqSenderService` | 向 `websocket_exchange` 广播，交换机未声明、方法未被调用 |
| `listener/MessageReceiverListener` | `@RabbitListener(queues="websocket_queue")`，按 `MyMessage.token` 找到 Channel 后 `writeAndFlush` 文本帧 |
| `handler/WebSocketFrameHandler` | 原设计的完整链路：握手完成校验 token→注册连接→投递 `websocket_queue`；**当前未被装配** |
| `netty/NettyWebSocketServer` / `netty/WebSocketServerInitializer` | 早期两套实现：前者内联 pipeline 且 `@Component` 已注释，后者（`TokenAuthHandler` + `WebSocketServerProtocolHandler`）无任何引用，均不生效 |
| `rabbit/RabbitMQConsumer` | 监听 `socket_queue` 的消费者，`@Component` 已注释 |

## 5. 接口清单

**HTTP 接口**

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| — | — | — | 本模块未定义任何 HTTP 接口（未在代码中体现），8080 仅用于承载 Spring 容器 |

**WebSocket 连接地址**

| 连接地址 | 协议 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| `ws://localhost:8888/socket?token=123456` | WebSocket（RFC 6455，Netty 原生） | URL query 参数 `token` 必须等于 `123456`，否则返回 401 并关闭 | 唯一生效入口；握手成功后换成 `ObjectWebSocketHandler`，文本帧回显 `Echo: <原文>`，Ping 帧回 Pong，读空闲 30 秒断开 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `websocket.port` | 8888 | `@Value("${websocket.port:8888}")`；application.yml 未配置，故实际监听 8888 |
| `websocket.path` | `/socket` | 绑定到 `WebSocketConfig`；但生效的 `ObjectHandshakeHandler` 并不校验路径 |
| `websocket.token.enable-heartbeat` | true | 没有任何 Java 代码读取该键，属无效配置 |
| `websocket.token.heartbeat-interval` | 30 | 仅被未启用的 `NettyWebSocketServer` 读取；生效链路把 30 秒写死在 `IdleStateHandler` |
| `websocket.token.timeout` | 60 | 仅被未启用的 `NettyWebSocketServer` 读取，当前不生效 |
| `server.port` | 8080 | Spring Boot HTTP 端口 |
| `spring.redis.host` / `port` | 10.11.0.136 / 6379 | Redis 连接；token 键设想为 `token:<token>` |
| `spring.rabbitmq.host` / `port` | 10.11.0.136 / 5672 | RabbitMQ 地址 |
| `spring.rabbitmq.username` / `password` | admin / admin123 | RabbitMQ 凭据 |
| `spring.rabbitmq.listener.simple.idle-event-interval` | 30s | 消费者空闲事件上报间隔 |

- 服务端口：HTTP 8080 / WebSocket 8888
- 依赖中间件：Redis（6379，token 键 `token:<token>` 仅存在于注释）、RabbitMQ（5672，队列 `websocket_queue`；另有未启用的 `socket_queue` 与未声明的 `websocket_exchange`）

## 7. 启动方式

前置条件：JDK 8、Maven 3.6+；Redis 与 RabbitMQ 需可连（默认指向 10.11.0.136，RabbitMQ 账号 `admin/admin123`），否则请在 `application.yml` 改为本机地址。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-websocket
mvn spring-boot:run
```

启动成功后可见日志：`WebSocket server started at port: 8888`。

## 8. 调用示例

```bash
# 1) 正常握手（token 必须为 123456），发送任意文本观察回显
npx wscat -c "ws://localhost:8888/socket?token=123456"
# 输入 hello，服务端返回：Echo: hello
# 2) 错误 token：握手阶段返回 401，连接随即关闭
npx wscat -c "ws://localhost:8888/socket?token=wrong"
# 3) HTTP 侧无业务接口，仅验证容器已启动（预期 404）
curl -i "http://localhost:8080/"
```

浏览器端等价写法（对应上面的连接地址与 token）：

```javascript
const ws = new WebSocket("ws://localhost:8888/socket?token=123456");
ws.onopen = () => ws.send("hello");
ws.onmessage = e => console.log("recv:", e.data); // recv: Echo: hello
```

## 9. 注意事项

1. **端口冲突**：本模块与 `demo-springboot-ws` 的 WebSocket 端口同为 **8888**（HTTP 也都是 8080），两个模块不能同时启动；同机联调需改其中之一的 `websocket.port` / `ws.port`。
2. 生效的 Netty 服务类是 `WebSocketServer`（`@Component` 存在）；`NettyWebSocketServer` 的 `@Component` 被注释且硬编码 `bind(8888)`，不会启动。`WebSocketServerInitializer` 无任何引用，`WebSocketInitializer` 才是 `WebSocketServer` 使用的初始化器。
3. 鉴权是硬编码：`ObjectHandshakeHandler` 只比较 `token` 查询参数与 `123456`，不校验路径；`TokenService` 与 `WebSocketHandler` 中的 Redis `token:` 校验均被注释，当前不存在真实 token 存储。
4. 心跳为“读空闲断连”：`IdleStateHandler(30, 0, 0, SECONDS)`，30 秒无入站数据即关闭；application.yml 的 60 秒 timeout 不生效，客户端需小于 30 秒发一次文本或 PING。
5. Redis 未参与连接管理：`ConnectionService` 用本地 `ConcurrentHashMap<token, Channel>` 保存连接，Redis 仅出现在被注释的 token 校验中，多实例间不共享。
6. RabbitMQ 链路在当前装配下是断的：`MessageReceiverListener` 监听 `websocket_queue`，但生效的 `ObjectWebSocketHandler` 不会把 Channel 注册进 `ConnectionService`（注册逻辑在未装配的 `WebSocketFrameHandler`），推送时找不到在线连接；`RabbitMQConsumer`（`socket_queue`）未启用，`websocket_exchange` 从未声明，`RabbitMqSenderService` 未被调用。
7. 包名 `com.csfrez.ws.servie` 是源码中的拼写（非 `service`），引用时注意。
8. 本模块没有 `src/test` 测试类；`application.yml` 中的中间件地址为内网 10.11.0.136，换环境需同步修改。

---

[← 返回项目总览](../README.md)
