# demo-springboot-common

> Spring Boot Web 层公共件样板：统一响应体、全局异常、请求日志、防重复提交注解、CORS、Jackson 工具与可重复读请求体。

## 1. 模块定位

本模块把业务系统中反复出现的 Web 层“公共件”集中成一个可直接启动的示例工程（端口 7070）：统一的 `ResponseResult`/`ResultCode` 返回协议、`@RestControllerAdvice` 全局异常兜底、AOP 出入参日志、`@SubmitLimit` 防重复提交、全局 CORS 过滤器、`JacksonUtils` 序列化封装、`CustomRequestWrapper` 请求体多次读取，以及一组用于验证异常与参数校验链路的 `WebController` 示例。

它自带 `DemoSpringbootCommonApplication`，是**独立可运行的 demo**，而非被其它模块依赖的 jar 基座——根 pom 只在 `dependencyManagement` 中登记了它的版本，没有任何兄弟模块真正 import 它。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 2.7.18 | Web / AOP / Validation 自动装配 |
| Java | 8 | 编译目标（`java.version=1.8`） |
| spring-boot-starter-web | 2.7.18 | MVC、内嵌 Tomcat、Jackson |
| spring-boot-starter-aop | 2.7.18 | 支撑 `WebLogAspect`、`SubmitLimitAspect` |
| spring-boot-starter-validation | 2.7.18 | `@Valid` + Hibernate Validator |
| hutool-all | 5.8.33 | JSONUtil、DigestUtil、RandomUtil、StrUtil |
| guava | 33.3.0-jre | `Lists.newArrayList` 等集合工具 |
| spel-validator-javax | 0.5.0-beta | `@SpelValid`/`@SpelAssert`/`@SpelNotNull` 表达式校验 |
| lombok | 1.18.30 | `@Data`、`@Slf4j`（版本由 spring-boot-dependencies 管理） |

## 3. 目录结构

```text
demo-springboot-common/
├── pom.xml
└── src/main
    ├── java/com/csfrez/demospringboot/
    │   ├── DemoSpringbootCommonApplication.java
    │   ├── advice/GlobalExceptionHandler.java
    │   ├── annotation/SubmitLimit.java
    │   ├── aspect/{SubmitLimitAspect,WebLogAspect}.java
    │   ├── base/{ResponseResult,ResultCode}.java
    │   ├── config/GlobalCorsConfig.java
    │   ├── dto/User.java
    │   ├── exception/{CommonException,CustomException}.java
    │   ├── filter/CustomRequestWrapper.java
    │   ├── utils/JacksonUtils.java
    │   └── web/WebController.java
    └── resources/{application.properties,user.json}
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `ResponseResult<T>` | 统一返回体 `{code,message,data}`，提供 `success/failed/failed(code,msg)/isSucceed` 静态工厂 |
| `ResultCode` | 枚举码表：200 成功、900 失败、400 参数错误、401 未认证、404 未找到、500 服务器内部错误 |
| `GlobalExceptionHandler` | `@RestControllerAdvice` 分类处理 404、参数校验、`IllegalArgumentException`、`RuntimeException`、`Exception`；`CustomException` 走 `RuntimeException` 分支取自定义 code |
| `CustomException` | 携带业务 `code` 的运行时异常（如 `new CustomException(501,"自定义异常")`） |
| `CommonException` | 只带 message 的通用业务异常，被 `SubmitLimitAspect` 单独捕获 |
| `WebLogAspect` | 切 `@Controller/@RestController` 类：前置打印 url+参数（跳过文件/请求/响应/BindingResult），正常返回后打印 JSON 化结果 |
| `SubmitLimit` / `SubmitLimitAspect` | 防重复提交注解（`waitTime` 默认 1000ms、`customerHeaders`、`customerTipMsg`）；切面用“应用名:类名:方法名:SHA1(指定header+body)”拼 lockKey，加锁逻辑目前被注释、恒返回放行 |
| `GlobalCorsConfig` | 注册全局 `CorsFilter`：`*` 源、允许凭证、放行全部 header 与方法，作用于 `/**` |
| `JacksonUtils` | 静态 `ObjectMapper`：全字段序列化、忽略未知字段、枚举未知值转 null、大小写不敏感、`Long/long` 序列化为字符串（防前端精度丢失） |
| `CustomRequestWrapper` | 继承 `HttpServletRequestWrapper`，构造时把 body 读入 `byte[]`，重写 `getInputStream()/getReader()` 实现多次读取；对 `MultipartRequest` 解析参数转 JSON；可 `setParameterMap` 注入额外参数 |
| `User` | 校验样例 DTO：`@NotBlank/@NotNull` + `@SpelAssert`（姓名≤5、年龄<18）+ `@SpelNotNull` 条件校验 `audioContent` |

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/hello` | 无 | 返回 `Hello Spring, Time is {timestamp}`，最简存活探针 |
| GET | `/test` | 无 | 随机 0~4 分别触发 `CustomException`、受检 `Exception`、`RuntimeException`、NPE、除零，用于验证全局异常 |
| GET | `/user` | `name`、`age`（`@RequestParam` 必填） | 手写参数校验；缺参抛 `MissingServletRequestParameterException`（见第 9 节，其处理器签名不匹配） |
| GET | `/user2` | Query 绑定 `User`（name/age/sex/switchAudio） | `@Valid` 触发 Bean Validation，`@SpelValid` 触发 SpEL 校验，失败抛 `BindException` |
| POST | `/user3` | JSON Body（`User`） | `@Valid @RequestBody`，校验通过后返回 `ResponseResult.success(user)` |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.application.name` | `demo-springboot-common` | 应用名，同时被 `SubmitLimitAspect` 用作 lockKey 前缀 |
| `server.port` | `7070` | HTTP 服务端口 |
| `logging.level.root` | `INFO` | 根日志级别 |
| `spring.mvc.throw-exception-if-no-handler-found` | `true` | 未匹配到 handler 时抛 `NoHandlerFoundException`，交由全局异常处理 |
| `spring.web.resources.add-mappings` | `false` | 关闭静态资源兜底映射，保证未匹配路径确实走 404 异常 |

- 服务端口：7070
- 依赖中间件：无（纯内存运行；`SubmitLimit` 预留了 Redis 锁，但未启用）

## 7. 启动方式

前置条件：JDK 8、Maven 3.6+，且已能访问到 Spring Boot 2.7.18 依赖；无需数据库或 Redis。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-common
mvn spring-boot:run
```

## 8. 调用示例

```bash
curl "http://localhost:7070/hello"
# 参数校验（姓名超 5 字 / 年龄 >= 18 会被 SpEL 拦下）
curl "http://localhost:7070/user2?name=abcdef&age=20&sex=male&switchAudio=true"
# 请求体校验 + 统一响应体
curl -X POST "http://localhost:7070/user3" \
  -H "Content-Type: application/json" \
  -d '{"name":"张三","age":10,"sex":"男","switchAudio":true}'
# 观察全局异常：多调几次会随机命中不同异常分支
curl "http://localhost:7070/test"
```

## 9. 注意事项

- **`@SubmitLimit` 目前是空转**：`SubmitLimitAspect` 中 Redis 加解锁代码整段被注释，`boolean lock = true` 恒真，`waitTime` 等参数不产生任何效果；同时 `getHttpServletRequest()` 直接强转 `RequestAttributes`，在非 Web 线程（异步、定时任务）调用会 NPE。
- **CORS 配置存在运行期风险**：`setAllowedOrigins("*")` 与 `setAllowCredentials(true)` 同时开启，Spring 5.3+ 的 `CorsConfiguration#checkOrigin` 会抛 `IllegalArgumentException`，带 `Origin` 头的跨域请求将 500；应改用 `setAllowedOriginPatterns` 或具体域名白名单。
- **异常响应的 HTTP 状态恒为 200**：除已注释掉的 `wrapperBindingResult(BindingResult, HttpServletResponse)` 会设 400 外，其余处理只把错误写进 body 的 `code`/`message`；`RuntimeException` 兜底还会把 `ex.getMessage()` 原样回给前端，存在内部信息泄漏风险，`CustomException` 在该分支被单独识别以保留业务 code。
- **缺参场景的处理器签名不匹配**：`@ExceptionHandler({BindException, MethodArgumentNotValidException, MissingServletRequestParameterException})` 方法只接收 `BindException`；`MethodArgumentNotValidException` 是其子类可正常处理，但 `MissingServletRequestParameterException` 与之无继承关系，`/user` 少传参数时不会走这段逻辑（典型表现为 500），应拆成独立处理方法。
- **`CustomRequestWrapper` 尚未接入链路**：模块内没有注册对应的 `Filter`/`OncePerRequestFilter`，该类当前只是可用工具；其 `getInputStream()` 返回的 `ServletInputStream` 的 `isFinished()/isReady()` 固定为 `false`。
- `JacksonUtils` 使用自己私有的 `ObjectMapper`，不会影响 MVC 全局 `ObjectMapper`；`Long` 转字符串的行为只对显式调用方生效。
- 模块虽名为 `common`，但没有任何模块依赖它，改动不会波及其它工程。

---

[← 返回项目总览](../README.md)
