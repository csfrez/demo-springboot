# demo-springboot-design-pattern

> 以商品创建参数校验为例演示责任链模式：多个校验 Handler 按配置串成链，逐级校验并在失败处短路。

## 1. 模块定位

- 纯内存示例，不依赖数据库、缓存或消息队列；业务是“创建商品前校验参数”，不包含落库逻辑，模块无 `src/test`。
- 责任链在运行时由 `HandlerClient` 根据一段 JSON 配置递归装配，每个 Handler 只负责一条校验规则。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 2.7.18 | 父工程统一版本，内嵌 Tomcat 9 |
| spring-boot-starter-web | 2.7.18 | 提供 REST 接口与 MVC 能力 |
| Lombok | 1.18.30 | `@Data`、`@Builder`、`@Slf4j` 简化代码 |
| Hutool | 5.8.33 | `JSONUtil` 解析链配置、`StrUtil` 判空 |
| Guava | 33.3.0-jre | pom 已引入，源码中未使用 |
| spring-boot-starter-test | 2.7.18 | 测试依赖，模块内无测试类 |
| Java | 8 | 编译级别（`java.version=1.8`） |

## 3. 目录结构

```text
demo-springboot-design-pattern/
├── pom.xml
└── src/main/
    ├── java/com/csfrez/demospringboot/
    │   ├── DemoSpringbootDesignPatternApplication.java
    │   ├── chain/handler/{Abstract,NullValue,Price,Stock}CheckHandler.java
    │   ├── chain/client/HandlerClient.java
    │   ├── chain/config/ProductCheckHandlerConfig.java
    │   ├── chain/vo/ProductVO.java
    │   ├── chain/dto/{Result,ErrorCode}.java
    │   └── web/WebController.java
    └── resources/application.properties
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| AbstractCheckHandler | 抽象处理器：持有 `nextHandler` 与 `config`，定义 `handle(ProductVO)`；`next()` 在链尾直接返回成功 |
| NullValueCheckHandler | 校验 `param`、`skuId`、`price`、`stock` 非空，失败返回 301/302/303/304；`down=true` 时整节点跳过 |
| PriceCheckHandler | 价格 `<= 0` 返回 305（与 `BigDecimal.ZERO` 比较） |
| StockCheckHandler | 库存 `< 0` 返回 306 |
| ProductCheckHandlerConfig | 链节点配置：Handler 的 Bean 名、下一个节点、是否降级 `down` |
| HandlerClient | 注入 `Map<String, AbstractCheckHandler>`，解析硬编码 JSON，递归设置 config/nextHandler 并执行链 |
| ProductVO | 商品参数：`skuId`、`skuName`、`Path`、`price`、`stock`，`@Builder` 构造 |
| Result / ErrorCode | 统一结果（`success` + `code`）与静态错误码常量 |
| WebController | 暴露 `/hello`、`/test`，后者组装参数并调用 HandlerClient |

**装配方式**：项目里没有 `@Configuration` 配置类，也没有使用 `@Order` 或 `List` 注入；Spring 仅负责把三个 Handler 子类注册为单例 Bean，默认 Bean 名为 `nullValueCheckHandler`、`priceCheckHandler`、`stockCheckHandler`。`HandlerClient` 通过 `@Resource Map<String, AbstractCheckHandler>` 获得“Bean 名 → 实例”映射，执行顺序与 Bean 注入顺序无关，完全由 `getHandlerConfigFile()` 中硬编码 JSON 的 `next` 指针决定：`nullValueCheckHandler → priceCheckHandler → stockCheckHandler → null`。`getHandler()` 递归解析 `next`，对每个节点执行 `setConfig()` 与 `setNextHandler()`；配置为 `null`、Handler 名为空或 Bean 不存在时返回 `null`，链被静默截断。

**新增 Handler**：新建类继承 `AbstractCheckHandler` 并标注 `@Component`（默认 Bean 名为类名首字母小写），实现 `handle()` 并通过 `super.next(param)` 传递；再把 Bean 名写进 `HandlerClient` 的 JSON（作为某节点的 `handler` 或 `next`）即可，无需改动其他类。若要支持真正的动态变更，应把该 JSON 外置到配置中心或配置文件。

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/hello` | 无 | `@RestController`，返回纯文本 `Hello Spring, Time is <当前毫秒时间戳>` |
| GET | `/test` | 无（服务端构造 `ProductVO`） | `@RestController`，返回 `Result` 的 JSON 字符串；固定 `skuId=null`，当前配置下命中 302 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | 内嵌 Tomcat 端口，模块内显式配置且生效 |
| `spring.application.name` | `demo-springboot-design-pattern` | 应用名，仅用于标识 |
| `logging.level.root` | `INFO` | 根日志级别 |
| `spring.mvc.throw-exception-if-no-handler-found` | `true` | 找不到 Handler 时抛 `NoHandlerFoundException` |
| `spring.web.resources.add-mappings` | `false` | 关闭静态资源映射，配合上一项让 404 进入异常流程 |

- 服务端口：8080
- 依赖中间件：无（仅内嵌 Tomcat，不需要 MySQL/Redis/MQ）

## 7. 启动方式

前置条件：JDK 8+、Maven 3.6+；父工程可解析（本地仓库已有 `demo-springboot:2.0.1-SNAPSHOT`，或先构建父工程）。无需启动外部中间件。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-design-pattern
mvn spring-boot:run
```

## 8. 调用示例

```bash
# 1. 探活
curl http://localhost:8080/hello
# Hello Spring, Time is 1730000000000

# 2. 触发责任链：skuId 固定为 null，空值校验失败
curl http://localhost:8080/test
# {"success":false,"code":"302"}
```

当前 `/test` 不接受请求参数，要验证成功分支需临时把代码中的 `skuId` 改为非空值。

## 9. 注意事项

- 三个 Handler 都是 Spring 单例，而 `setConfig()`/`setNextHandler()` 在每次请求时改写共享字段，并发请求会互相覆盖链状态；当前配置固定所以结果碰巧一致，但这属于明确的线程安全隐患。应改为启动时构建不可变链，或按请求克隆。
- 链配置硬编码在 `HandlerClient.getHandlerConfigFile()`，注释所称“配置中心存储、支持动态变更”并未实现，改链必须改代码并重新编译；`handler` 为空或 Bean 名不存在时链路被静默截断，可能漏掉校验，也没有环检测。
- 顺序存在隐式依赖：`NullValueCheckHandler` 必须先执行，否则 `PriceCheckHandler` 的 `param.getPrice().compareTo(...)` 会 NPE；`down` 是 `Boolean` 包装类型，JSON 未给出该字段而反序列化为 `null` 时，`if (getConfig().getDown())` 拆箱会 NPE。
- `/test` 写死 `skuId=null`，只能演示失败分支；返回的是 `Result.toString()` 字符串，Content-Type 为 `text/plain` 而非 `application/json`。
- pom 引入 Guava 但源码未使用，`spring-boot-starter-test` 也无测试类；端口与 jsqlparser 模块同为 8080，两个模块不能同时启动。

---

[← 返回项目总览](../README.md)
