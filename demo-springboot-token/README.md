# demo-springboot-token

> Sa-Token 1.44.0 全景示例：登录/注销、路由级与注解级鉴权、权限角色、会话、踢人下线，token 与会话落 Redis。

## 1. 模块定位

以 Sa-Token 官方示例的写法，把「登录 → 鉴权 → 会话 → 踢人」整条链路拆成 6 个 Controller：`/acc` 基础登录、`/user` 免登录入口与 Redis 连通性、`/auth` 编码式权限/角色校验、`/check` 注解式鉴权（`@SaCheckLogin`/`@SaCheckPermission`/`@SaCheckRole`/`@SaIgnore`）、`/kickout` 强制注销与踢人、`/session` 会话存取。全局拦截器在 `SaTokenConfigure` 中注册，权限数据由 `StpInterfaceImpl` 提供；token 与 Session 经 `sa-token-redis-jackson` 持久化，并用 `sa-token-alone-redis` 与业务缓存分库隔离。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 2.7.18 | Web 与 Redis 自动装配 |
| Java | 8 | 编译目标（`java.version=1.8`） |
| sa-token-spring-boot-starter | 1.44.0 | `StpUtil`、注解鉴权、`SaInterceptor`、`SaResult` |
| sa-token-redis-jackson / sa-token-alone-redis | 1.44.0 | token/Session 持久化（Jackson 序列化）+ 独立 Redis 连接（db2） |
| spring-boot-starter-data-redis | 2.7.18 | 业务侧 `StringRedisTemplate`（Lettuce + commons-pool2） |
| hutool-all / lombok | 5.8.33 / 1.18.30 | 工具类 / `@Data`、`@Slf4j` |

## 3. 目录结构

```text
demo-springboot-token/
├── pom.xml
├── src/main/java/com/csfrez/token/{TokenApplication, config/SaTokenConfigure, inter/StpInterfaceImpl}
├── src/main/java/com/csfrez/token/{rest/{Login,User,Auth,Check,Kickout,Session}Controller, entity/SysUser, handler/GlobalExceptionHandler}
├── src/main/resources/{application.yml, static/index.html}
└── test/java/com/csfrez/ws/TokenApplicationTests.java
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `TokenApplication` / `GlobalExceptionHandler` | 启动类（启动后打印 `SaManager.getConfig()` 全量配置）+ `@RestControllerAdvice` 兜底 `Exception` 转 `SaResult.error` |
| `SaTokenConfigure` | `WebMvcConfigurer` 注册 `SaInterceptor`：`/**` 排除 `/user/doLogin` 后统一 `StpUtil.checkLogin()`；再对 `/admin/**`、`/goods/**`、`/orders/**`、`/notice/**`、`/comment/**` 分别 `checkPermission` |
| `StpInterfaceImpl` | 实现 `StpInterface`，为任意 `loginId` 返回固定权限 `101,user.add,user.update,user.get,user.delete,art.*` 与角色 `admin,super-admin` |
| `LoginController` / `UserController` | 两套登录入口：`/acc/doLogin`（zhang/123456 → `StpUtil.login(10001)`）与 `/user/doLogin`（唯一被排除登录校验的路径）；另含 `isLogin`、`tokenInfo`、`logout`、Redis 读写 `test` |
| `AuthController` | 编码式鉴权：`getPermissionList/getRoleList`、`hasPermission*`/`checkPermission*`（And/Or）、`hasRole*`/`checkRole*`、`art.*` 通配符演示 |
| `CheckController` | 注解式鉴权：`@SaCheckLogin`、`@SaCheckPermission`(AND/OR)、`@SaCheckRole`、`orRole`、`@SaIgnore` |
| `KickoutController` / `SessionController` | `StpUtil.logout(userId)` 强制注销、`kickout(userId)`/`kickoutByTokenValue(token)` 踢人；Session 存取、`getModel`、`SaSessionCustomUtil` 自定义会话 |

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET/POST | `/acc/doLogin` | `name`、`pwd` | **需登录才能进**（未在拦截器排除列表）；zhang/123456 → 登录 10001 并返回 `SaResult.ok` |
| GET/POST | `/acc/isLogin` | 无 | 返回 `StpUtil.isLogin()` 的布尔值 |
| GET/POST | `/acc/tokenInfo` | 无 | 返回 `StpUtil.getTokenInfo()`（token 名、值、有效期等） |
| GET/POST | `/acc/logout` | 无 | `StpUtil.logout()` 注销当前会话 |
| GET/POST | `/user/doLogin` | `username`、`password` | **唯一免登录接口**（拦截器 `notMatch`）；zhang/123456 → `login(10001)`，返回纯文本 |
| GET/POST | `/user/isLogin` | 无 | 纯文本输出当前会话登录状态 |
| GET/POST | `/user/test` | 无 | 通过 `StringRedisTemplate` 写读 `hello`，验证业务 Redis（db0） |
| GET/POST | `/auth/getPermission` | 无 | 列出当前账号权限与角色；未登录返回空集合 |
| GET/POST | `/auth/checkPermission` | 无 | `hasPermission/And/Or` 判断 + `checkPermission/And/Or` 校验，失败抛 `NotPermissionException` |
| GET/POST | `/auth/checkRole` | 无 | `hasRole/And/Or` 判断 + `checkRole/And/Or` 校验，失败抛 `NotRoleException` |
| GET/POST | `/auth/wildcardPermission` | 无 | 演示 `art.*`、`*.delete`、`shop.*.user` 通配与 `*` 上帝权限 |
| GET/POST | `/check/checkLogin` | 无 | `@SaCheckLogin`：登录后才可进入 |
| GET/POST | `/check/checkPermission` | 无 | `@SaCheckPermission("user.add")`：需该权限码 |
| GET/POST | `/check/checkPermission2` | 无 | `@SaCheckPermission({user.add,user.delete,user.update}, mode=AND)` |
| GET/POST | `/check/checkPermission3` | 无 | 同上但 `mode=OR`：命中任一即可 |
| GET/POST | `/check/checkRole` | 无 | `@SaCheckRole("super-admin")`：需该角色 |
| GET/POST | `/check/userAdd` | 无 | `@SaCheckPermission(value="user.add", orRole="admin")`：权限或角色任一满足 |
| GET/POST | `/check/ignore` | 无 | 同时标 `@SaIgnore` 与 `@SaCheckLogin`：`@SaIgnore` 优先生效，可匿名访问 |
| GET/POST | `/kickout/logout` | `userId` | `StpUtil.logout(userId)` 强制注销，对方再访问提示 Token 无效 |
| GET/POST | `/kickout/kickout` | `userId` | `StpUtil.kickout(userId)` 踢下线，保留 token 并打标记 |
| GET/POST | `/kickout/kickoutByTokenValue` | `tokenValue` | 按 token 值精确踢人 |
| GET/POST | `/session/getValue` | 无 | 当前账号 `SaSession` 存取与多类型读取（含默认值、lambda 兜底） |
| GET/POST | `/session/getModel` | 无 | 往 Session 写 `SysUser`，再用 `getModel` 反序列化取回 |
| GET/POST | `/session/customSession` | 无 | `SaSessionCustomUtil.getSessionById("role-1001")` 自定义会话 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` / `spring.application.name` | `8080` / `demo-springboot-token` | HTTP 端口与应用名 |
| `spring.redis.host` / `port` | `10.11.0.136` / `6379` | 业务 Redis（默认 db0），`/user/test` 使用 |
| `sa-token.token-name` | `satoken` | token 名，同时是 cookie 名与请求头名 |
| `sa-token.timeout` / `active-timeout` | `2592000` / `-1` | token 有效期 30 天；-1 表示永不冻结 |
| `sa-token.is-concurrent` / `is-share` | `true` / `false` | 同账号允许多地同时登录；不共用 token，每次登录新建 |
| `sa-token.token-style` / `is-log` | `simple-uuid` / `true` | token 风格；输出 Sa-Token 操作日志 |
| `sa-token.alone-redis.database` | `2` | Sa-Token 独立库，与业务 db0 隔离 |
| `sa-token.alone-redis.host` / `port` / `timeout` | `10.11.0.136` / `6379` / `10s` | 独立连接；`password` 为空 |

- 服务端口：8080
- 依赖中间件：Redis（业务 db0 + Sa-Token db2，同实例 `10.11.0.136:6379`，无密码）

## 7. 启动方式

前置条件：JDK 8、Maven 3.6+，且 `10.11.0.136:6379` 的 Redis 可连通（否则 `sa-token-redis-jackson` 初始化即失败）；无需数据库。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-token
mvn spring-boot:run
```

## 8. 调用示例

```bash
# 先走唯一免登录入口拿 token（响应体或 Set-Cookie 中的 satoken）
curl -i "http://localhost:8080/user/doLogin?username=zhang&password=123456"
curl -H "satoken: <token>" "http://localhost:8080/check/checkLogin"
curl -H "satoken: <token>" "http://localhost:8080/auth/getPermission"
curl -H "satoken: <token>" "http://localhost:8080/session/getValue"
curl -H "satoken: <token>" "http://localhost:8080/kickout/kickout?userId=10001"
```

## 9. 注意事项

- **`/acc/doLogin` 被自己的拦截器拦住**：`SaTokenConfigure` 只 `notMatch("/user/doLogin")`，`/acc/doLogin` 命中 `/**` 的 `StpUtil.checkLogin()`，未登录时直接抛 `NotLoginException`，`LoginController` 实际不可用，应把 `/acc/doLogin` 一并排除；此外除 `/user/doLogin` 外所有路径（含 `/index.html` 等静态资源）都会要求登录，只有 `@SaIgnore`（优先级最高，可同时忽略注解与路由两层校验）标记的 `/check/ignore` 例外。
- **权限数据源是死数据**：`StpInterfaceImpl` 对任意 `loginId` 返回同一份权限/角色，未查库、无缓存，等于所有登录用户都是 `admin` + `super-admin`；而路由级规则要求的 `/admin`、`/goods`、`/orders`、`/notice`、`/comment` 前缀权限码它并未提供，真实项目中这类请求会被拒。
- **`is-concurrent: true` + `is-share: false` 的组合**：同账号允许多端同时在线且每次登录新建独立 token；`StpUtil.logout(userId)` / `kickout(userId)` 会遍历注销该账号的全部 token。若希望“后登录挤掉前登录”，需改为 `is-concurrent: false`（此时 `is-share` 失效）。
- **Redis 地址硬编码且无鉴权**：`10.11.0.136:6379` 无密码、无 TLS，`spring.redis` 与 `sa-token.alone-redis` 两处重复配置，换环境必须同时改；两套缓存仅靠 db 0/2 区分。
- **异常处理掩盖状态语义**：`GlobalExceptionHandler` 用 `@ExceptionHandler(Exception.class)` 吞掉 `NotLoginException`/`NotPermissionException`/`NotRoleException`，统一返回 `SaResult.error(msg)`（code 500），前端无法区分 401 未登录与 403 无权限。
- **有效期偏长**：`timeout=2592000`（30 天）叠加 `active-timeout=-1` 永不冻结，token 泄漏后 30 天内持续可用；`is-log: true` 在生产会产生大量鉴权日志。
- **测试与注释问题**：`TokenApplicationTests` 位于 `com.csfrez.ws`，与启动类 `com.csfrez.token` 不同分支，`@SpringBootTest` 向上找不到 `@SpringBootConfiguration`，`mvn test` 会失败，且启动强依赖外网 Redis；`CheckController` 注释写 `8081` 与 `/at-check/*`、`AuthController` 注释写 `/jur/*`，实际端口 8080、路径 `/check/*` 与 `/auth/*`。

---

[← 返回项目总览](../README.md)
