# demo-springboot-ai

> 基于 ai4j 的 DeepSeek 对话最小样例：一个同步 `/chat`，一个"名义上流式"的 `/chat/stream`。

## 1. 模块定位

- 演示 ai4j-spring-boot-starter 接入 DeepSeek：注入 `AiService` → 取 `IChatService` → 用 `ChatCompletion` 构造请求 → 同步或 SSE 取回结果。
- 在项目中是唯一的 AI 能力演示，包名独立（`com.csfrez.ai`），与其它模块没有代码或 Maven 依赖关系，只继承父 POM。
- 附带两个"脚手架残留"：`GracefulShutdownListener`（只打日志）与 `static/index.html`（静态页）；端口固定 8080，与 `demo-springboot-server` 冲突，不能同时启动。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java / Spring Boot | 1.8 / 2.7.18 | 编译级别；Boot 由父 POM 管理（`spring-boot-dependencies` import） |
| spring-boot-starter-web | 2.7.18 | 由父 POM 管理，内嵌 Tomcat + MVC |
| ai4j-spring-boot-starter | 1.4.3 | 模块 pom 硬编码版本（**未**纳入父 POM 管理），DeepSeek 客户端与自动装配 |
| hutool-all / lombok | 5.8.33 / 1.18.30 | 均由父 POM 管理；hutool 实际未使用，lombok 提供 `@Slf4j` |
| spring-boot-maven-plugin / starter-test | 2.7.18 | 显式绑定 `repackage` 打出可执行 jar；test 为 JUnit 5 |

## 3. 目录结构

```text
demo-springboot-ai/
├── pom.xml
└── src/
    ├── main/java/com/csfrez/ai/
    │   ├── AiApplication.java
    │   ├── listener/GracefulShutdownListener.java
    │   └── rest/HelloController.java
    ├── main/resources/{application.yml, static/index.html}
    └── test/java/com/csfrez/ws/WsiApplicationTests.java
```

本模块没有 `mvnw`/`.mvn`，与其它模块不一致，需使用系统 Maven 或在父目录执行。

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `AiApplication` | 启动类，仅 `@SpringBootApplication`；ai4j 的自动装配由此触发 |
| `HelloController` | `@RestController`；注入 `AiService`；`/hello` 返回时间戳串；`/chat` 同步调用 `deepseek-chat`；`/chat/stream` 用 `SseListener` 打印增量 |
| `GracefulShutdownListener` | 实现 `ApplicationListener<ContextClosedEvent>`，收到关闭事件后只打印一行 warn，无实际清理动作 |
| `application.yml` | 端口 8080、应用名、`ai.deepseek.api-key`（空值占位） |
| `WsiApplicationTests` | 遗留空测试（包 `com.csfrez.ws`），只做 `contextLoads`，见第 9 节 |

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| 任意方法（`@RequestMapping` 未限定） | `/hello` | — | 返回 `hello world {timestamp}`，不依赖 AI 服务 |
| 任意方法 | `/chat` | — | 同步调用：模型 `deepseek-chat`，问题固定为"鲁迅为什么打周树人"，返回 `response.getObject()` |
| 任意方法 | `/chat/stream` | — | 触发流式调用并把增量打到日志；方法返回 `void`，SSE 内容不会写回 HTTP 响应 |
| GET | `/index.html` | — | `static/index.html` 静态页（hello word!!!） |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` / `spring.application.name` | `8080` / `demo-springboot-ai` | 业务端口（与 server 模块冲突）/ 应用名 |
| `ai.deepseek.api-key` | `""`（空字符串） | DeepSeek 密钥占位符，必须填写才能真实调用 |

- 服务端口：8080
- 依赖中间件：外部 DeepSeek 开放平台 API（HTTPS，由 ai4j 发起）；无数据库、无 Redis、无注册中心

## 7. 启动方式

前置条件：JDK 8+、Maven、可访问 `api.deepseek.com` 的网络，以及一个有效的 DeepSeek API Key（配置文件里是空串）。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-ai
mvn spring-boot:run
```

不改文件覆盖密钥（Spring 松散绑定，环境变量亦可）：在模块目录用 `mvn spring-boot:run -Dspring-boot.run.arguments=--ai.deepseek.api-key=sk-xxxx`。启动后访问 <http://127.0.0.1:8080/hello> 验证 Web 正常，再访问 `/chat` 验证 AI 通道。

## 8. 调用示例

```bash
curl http://127.0.0.1:8080/hello              # 不依赖 AI 的连通性检查
curl http://127.0.0.1:8080/chat               # 同步对话（需有效 api-key）
curl -N http://127.0.0.1:8080/chat/stream     # 流式：客户端拿到空响应体，增量只在服务端日志
```

核心调用片段（真实来自 `HelloController#chat`）：

```java
IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK);
ChatCompletion chatCompletion = ChatCompletion.builder().model("deepseek-chat")
        .message(ChatMessage.withUser("鲁迅为什么打周树人")).build();
ChatCompletionResponse response = chatService.chatCompletion(chatCompletion);
```

## 9. 注意事项

- **API Key 为空**：`ai.deepseek.api-key: ""` 只是占位，不填值时应用能正常启动（starter 不校验密钥），`/chat`、`/chat/stream` 在调用时才抛鉴权/401 异常；请勿把真实 key 提交进仓库。
- **端口冲突**：`server.port: 8080` 与 `demo-springboot-server` 完全相同，两个模块同时启动必有一个 `Port already in use`。
- **`/chat/stream` 并不流给浏览器**：方法声明为 `void`，没有 `HttpServletResponse`/`SseEmitter`/`Flux` 输出，`SseListener` 只把 `getCurrStr()` 写日志，客户端收到的是空响应体（HTTP 200）。
- **函数调用缺定义**：`.functions("queryWeather")` 只指定了函数名，工程里没有对应的 function/tool 定义与回调实现，模型无法真正调用天气函数，该示例不完整。
- **测试必然失败**：`WsiApplicationTests` 在包 `com.csfrez.ws`，启动类在 `com.csfrez.ai`；`@SpringBootTest` 从测试类所在包向上找不到 `@SpringBootConfiguration`，报 `Unable to find a @SpringBootConfiguration`，不跳过测试时构建失败。
- **`GracefulShutdownListener` 名不副实、依赖版本未统一管理**：前者只打印一行日志（未配 `server.shutdown=graceful`，不会延长停机等待）；`ai4j-spring-boot-starter` 版本写死在模块 pom（1.4.3），要统一管理需移入根 `dependencyManagement`。

---

[← 返回项目总览](../README.md)
