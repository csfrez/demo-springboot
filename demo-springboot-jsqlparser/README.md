# demo-springboot-jsqlparser

> 基于 MyBatis 拦截器 + JSqlParser 的 SQL 改写示例：在语句执行前自动追加 `env` 隔离条件（模块名中的“数据权限隔离”）。

## 1. 模块定位

- 拦截 MyBatis 即将执行的 SQL，用 JSqlParser 解析语法树后向 SELECT/INSERT/UPDATE/DELETE 追加 `env` 条件，按运行环境（profile）隔离数据。
- 附带 Thymeleaf 页面 `/user-types`，读取 `app.user-type` 配置展示五种用户类型描述；用户类型只用于展示，**没有**参与 SQL 改写。
- 隔离值是 `spring.profiles.active`，因此实现的是“环境隔离”，不是按登录用户/角色的行级数据权限。
- 模块内没有 Mapper、实体或 Service，拦截器虽被注册，但当前没有 SQL 经过它，示例链路并不完整。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 2.7.18 | 父工程统一版本，内嵌 Tomcat 9 与 Spring MVC |
| mybatis-spring-boot-starter | 2.3.2 | MyBatis 3.5.13 自动配置，自动收集 `Interceptor` Bean |
| JSqlParser | 4.6 | SQL 解析与抽象语法树改写 |
| mysql-connector-j | 8.4.0 | MySQL 驱动（连接池为 Boot 默认 HikariCP 4.0.3） |
| Hutool | 5.8.33 | `StrUtil.isNotBlank` 判断表别名 |
| spring-boot-starter-thymeleaf | 2.7.18 | Thymeleaf 3.0.15 服务端模板 |
| Lombok | 1.18.30 | `@Data` 生成配置类读写方法 |
| Java | 8 | 编译级别（`java.version=1.8`） |

## 3. 目录结构

```text
demo-springboot-jsqlparser/
├── pom.xml / mvnw / mvnw.cmd / .mvn/wrapper/
└── src/main/
    ├── java/com/csfrez/demospringboot/jsqlparser/
    │   ├── DemoSpringbootJsqlparserApplication.java
    │   └── {config/AppConfig,controller/UserController,enums/UserTypeEnum,intercept/DataIsolationInterceptor}.java
    └── resources/{application.yml,templates/index.html}
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| DataIsolationInterceptor | `@Component` + `@Intercepts` 拦截 `StatementHandler.prepare(Connection, Integer)`；解析 `BoundSql.sql`，按语句类型改写后通过 `MetaObject` 写回 |
| AppConfig | `@ConfigurationProperties(prefix = "app")` 绑定 `app.user-type`；内含 `Database`、`Server` 子配置，但 yml 未提供值，也未被使用 |
| UserController | `@Controller`，`/user-types` 将五个枚举描述放入 `Model`，返回 Thymeleaf 视图 `index` |
| UserTypeEnum | `ADMIN`/`USER`/`GUEST`/`VIP`/`MODERATOR` 及其中文 `description` |
| index.html | Thymeleaf 表格渲染五种角色描述，Bootstrap/jQuery 走 CDN |
| DemoSpringbootJsqlparserApplication | 启动类，`@SpringBootApplication` |

**拦截器注册**：`DataIsolationInterceptor` 本身是 `@Component`，`mybatis-spring-boot-starter` 的 `MybatisAutoConfiguration` 会把容器中所有 `Interceptor` Bean 加入 `SqlSessionFactory` 插件链，无需额外配置类。`AppConfig` 只做属性绑定，**不负责**注册拦截器（其中没有任何 `@Bean` 方法）；`@Intercepts` 也只声明拦截签名。

**SQL 改写规则**（`setEnvToStatement`）：
- SELECT：仅处理 `PlainSelect`。无 JOIN 时在原 where 后 `AND env = 'dev'`（不带表名）；有 JOIN 时要求主表和每个 JOIN 右表都有别名，逐表追加 `别名.env = 'dev'`；FROM 为子查询时只改一层子查询的 where，且不带别名。
- INSERT：向列清单追加 `env` 列，并向 VALUES 的每一行追加 `'dev'`，依赖显式列名与 JSqlParser 4.6 的 `SetOperationList/ValuesStatement` 结构；UPDATE/DELETE 则直接在原 where 上 `AND env = 'dev'`（单表写法，不带别名）。
- 其他语句（DDL 等）原样返回；`UNION`、`WITH` 属于 Select 但取不到 `PlainSelect`，会在 `getFromItem()` 处抛异常；解析失败抛 `RuntimeException`，直接中断该次数据库操作。`env` 值来自 `@Value("${spring.profiles.active}")`（即 `dev`），字段名 `env` 和各表表名全部硬编码。

**用户类型映射**：`app.user-type.admin=ADMIN` 等值通过松散绑定转成 `UserTypeEnum`，`UserController` 调用 `getDescription()` 渲染页面。源码中不存在“用户类型 → where 条件”的分支；若要实现真正的数据权限，需要新增登录态解析、角色到条件（如 `user_id`/`dept_id`）的映射，并在拦截器中按表注入。

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/user-types` | 无 | `@Controller` 返回 Thymeleaf 视图 `index`，渲染五种用户类型描述；模块没有 REST 查询接口 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.profiles.active` | `dev` | 激活环境，同时被拦截器当作隔离值 `env` |
| `spring.datasource.url` | `jdbc:mysql://10.11.0.142:3306/demo` | 内网 MySQL 地址，明文硬编码 |
| `spring.datasource.username` / `password` | `root` / `123456` | 明文账号密码 |
| `spring.server.port` | `8080`（无效键） | Spring Boot 不识别，实际不生效；正确键为 `server.port` |
| `app.user-type.admin/user/guest/vip/moderator` | `ADMIN`/`USER`/`GUEST`/`VIP`/`MODERATOR` | 绑定 `UserTypeEnum`，仅页面展示 |
| `app.server.port` / `app.database.*` | 未配置 | `AppConfig` 的死代码，无绑定也无使用 |
| `server.port` | `8080`（Spring Boot 默认） | 实际端口来源，yml 未显式配置 |

- 服务端口：实际为 8080（默认值；`spring.server.port` 不生效，改它不会改变端口）
- 依赖中间件：MySQL（`10.11.0.142:3306/demo`）；但无 Mapper/初始化脚本，启动和访问页面通常不会建立数据库连接

## 7. 启动方式

前置条件：JDK 8+、Maven 3.6+（或模块自带 `mvnw`，wrapper 指定 Maven 3.5.4）；若后续补充 Mapper，需要可访问的 MySQL 8 实例及含 `env` 列的表。注意本模块 pom 未声明 `spring-boot-maven-plugin`，替代命令见第 9 节。

```bash
# 方式一（推荐）：在 IDE 中运行 DemoSpringbootJsqlparserApplication
# 方式二：显式指定插件版本启动（首次需先在仓库根目录执行 mvn clean install -DskipTests）
mvn -pl demo-springboot-jsqlparser org.springframework.boot:spring-boot-maven-plugin:2.7.18:run
```

## 8. 调用示例

```bash
# 查看用户类型页面（返回渲染后的 HTML，不是 JSON）
curl http://localhost:8080/user-types

# 模块没有查询接口；若自行补充 Mapper，下面这条单表 SQL
# SELECT id, name FROM sys_user WHERE status = 1
# 会被改写为
# SELECT id, name FROM sys_user WHERE status = 1 AND env = 'dev'
```

## 9. 注意事项

- 端口配置写错：`spring.server.port: 8080` 不是 Spring Boot 属性（正确为 `server.port`），该行被忽略；当前 8080 只是默认值，修改这行不会改变端口。`AppConfig` 中另有一个未绑定的 `Server.port`，容易混淆。
- 名不副实：条件恒为 `env = '当前 profile'`，与登录用户/角色无关，`UserTypeEnum` 仅用于页面展示；它不是行级数据权限方案。
- 表名/字段名硬编码 `env`，无表白名单或黑名单；所有经过 MyBatis 的 SQL（含字典表、日志表）都会被追加，表没有 `env` 列会直接报错。JOIN 场景要求所有表都有别名，否则 `getAlias()` 抛 NPE。
- 复杂 SQL 局限：只支持单层 `PlainSelect` 和单层 FROM 子查询；UNION/CTE、WHERE 内子查询、`INSERT ... SELECT`、无列名 INSERT 均不支持；给外连接追加 WHERE 条件会使其退化为内连接；多表 UPDATE/DELETE 未处理别名。
- 凭据明文：`root/123456` 和内网 IP 提交在 `application.yml`，应改为环境变量/配置中心并限制账号权限；同时缺少 `driver-class-name`、时区/SSL、连接池和 MyBatis 配置。解析失败会抛 `RuntimeException` 并携带原始 SQL，可能泄漏 SQL 到日志。
- 缺少 `spring-boot-maven-plugin`，无法直接 `spring-boot:run` 或打出可执行 jar；可在 IDE 运行启动类，或在根目录执行 `mvn -pl demo-springboot-jsqlparser org.springframework.boot:spring-boot-maven-plugin:2.7.18:run`（父 POM 已安装时）。模块没有 Mapper/实体/Service，拦截器被注册却无 SQL 可拦，无法端到端验证；与 design-pattern 默认端口同为 8080，不能同时启动。

---

[← 返回项目总览](../README.md)
