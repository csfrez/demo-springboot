# demo-springboot-mybatis-plus

> MyBatis-Plus + Druid + MySQL 的单表 CRUD、自动填充与代码生成示例。

## 1. 模块定位

演示 MyBatis-Plus 3.5.12 在 Spring Boot 2.7 下的最小落地：`BaseMapper`/`ServiceImpl` 零 SQL CRUD、`@TableField(fill=...)` + `MetaObjectHandler` 自动填充、Druid 连接池与监控页，以及 `FastAutoGenerator` 代码生成器（含自定义 FreeMarker 模板）。只有 list/add/update 三个 REST 接口，删除和条件查询留给扩展。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 由父 POM 管理（2.7.18） | Web、数据源自动配置 |
| spring-boot-starter-web | 由父 POM 管理（2.7.18） | REST 接口 |
| mybatis-plus-boot-starter | 由父 POM 管理（3.5.12） | BaseMapper、ServiceImpl、自动填充 |
| druid-spring-boot-starter | 由父 POM 管理（1.2.24） | 连接池、SQL/URI 监控 |
| mysql-connector-j | 由父 POM 管理（8.4.0） | MySQL 驱动 |
| mybatis-plus-generator | 由父 POM 管理（3.5.12），provided | FastAutoGenerator |
| FreeMarker | 由父 POM 管理（2.3.32），provided | 代码生成模板引擎 |
| Lombok | 由父 POM 管理（1.18.30） | @Getter/@Setter/@Slf4j |
| JDK | 1.8 | 编译目标 |

## 3. 目录结构

```text
demo-springboot-mybatis-plus/
├── pom.xml
└── src/
    ├── main/java/com/csfrez/mybatis/
    │   ├── MybatisPlusApplication.java
    │   ├── dao/entity/User.java
    │   ├── dao/handler/AutoFillHandler.java
    │   ├── dao/mapper/UserMapper.java
    │   ├── rest/UserController.java
    │   └── service/UserService.java
    ├── main/resources/
    │   ├── application.properties
    │   └── mapper/UserMapper.xml
    └── test/
        ├── java/com/csfrez/mybatis/CodeGenerator.java
        └── resources/
            ├── sql/user.sql
            └── templates/service.java.ftl
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| MybatisPlusApplication | 启动类；`@MapperScan("com.csfrez.mybatis.dao.mapper")` 扫描 Mapper |
| User | `@TableId(type = AUTO)`；`createTime` 标记 INSERT、`updateTime`/`version` 标记 INSERT_UPDATE；`is_delete` 未加 @TableLogic |
| UserMapper | 继承 `BaseMapper<User>`，零 SQL 获得单表 CRUD；XML 只定义 BaseResultMap |
| UserService | 继承 `ServiceImpl<UserMapper, User>`，list/save/updateById 均由 MP 提供 |
| AutoFillHandler | insertFill 填 createTime/updateTime=now、version=1；updateFill 将 version 手动 +1 并填 updateTime |
| UserController | `GET /user/list`、`POST /user/add`、`POST /user/update`，直接返回实体或列表 |
| CodeGenerator | test 源码中的 main 方法；FastAutoGenerator 连库生成 entity/mapper/controller/service，禁用 Mapper/ServiceImpl 生成，使用自定义 FTL |

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | /user/list | 无 | 查询全部用户，返回 JSON 数组 |
| POST | /user/add | JSON body：User（nickname、sex 等） | 插入并回填自增 id，返回含 id 的 User |
| POST | /user/update | JSON body：User（必须含 id） | updateById，返回请求体；不判断影响行数 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| server.port | 8080 | HTTP 端口 |
| spring.datasource.url | jdbc:mysql://10.11.0.142:3306/demo?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true | 内网 MySQL，库名 demo |
| spring.datasource.username / password | root / 123456 | 明文硬编码 |
| spring.datasource.driver-class-name | com.mysql.cj.jdbc.Driver | Connector/J 8 驱动 |
| spring.datasource.druid.initial-size | 1 | 初始连接数 |
| spring.datasource.druid.min-idle | 10 | 最小空闲；大于 initial-size，语义不一致 |
| spring.datasource.druid.max-active | 50 | 最大连接数 |
| spring.datasource.druid.test-on-borrow | true | 每次借连接都校验，开销较高 |
| spring.datasource.druid.filters | stat | 开启 StatFilter |
| spring.datasource.druid.stat-view-servlet.enabled | true | 开启 /druid 监控页 |
| spring.datasource.druid.stat-view-servlet.login-username / login-password | druid / druid@123 | 监控页登录账号 |
| spring.datasource.druid.stat-view-servlet.allow | * | 允许所有来源访问 |
| logging.level.com.csfrez.mybatis.dao | TRACE | 输出 Mapper SQL 与参数 |

- 服务端口：8080
- 依赖中间件：MySQL 8（10.11.0.142:3306/demo）；Druid 内置连接池；监控页 http://localhost:8080/druid

## 7. 启动方式

前置条件：JDK 8、Maven、可访问的 MySQL 8；先执行 `src/test/resources/sql/user.sql` 建表；按环境修改数据源；`mybatis-plus-generator` 与 FreeMarker 为 provided，代码生成仅在编译/测试类路径可用。

```bash
# 方式一（推荐）：在 IDE 中运行 MybatisPlusApplication
# 方式二：显式指定插件启动（首次需先在仓库根目录执行 mvn clean install -DskipTests）
mvn -pl demo-springboot-mybatis-plus org.springframework.boot:spring-boot-maven-plugin:2.7.18:run
```

> 本模块 pom 未声明 `spring-boot-maven-plugin`，直接执行 `mvn spring-boot:run` 会因无法解析 `spring-boot` 前缀而失败，默认 `package` 产物也不是可执行 jar。

## 8. 调用示例

```bash
# 查询
curl http://localhost:8080/user/list

# 新增
curl -X POST http://localhost:8080/user/add \
  -H "Content-Type: application/json" \
  -d '{"nickname":"张三","sex":true}'

# 更新（version 传入后会由 AutoFillHandler 递增）
curl -X POST http://localhost:8080/user/update \
  -H "Content-Type: application/json" \
  -d '{"id":1,"nickname":"李四","sex":false,"version":1}'

# Druid 监控页
curl -I http://localhost:8080/druid
```

## 9. 注意事项

- 数据源 URL、账号密码硬编码内网 `10.11.0.142` 与 root/123456；换环境必须改，生产应使用环境变量/配置中心并限制权限。
- `sex` 建表注释为“1男2女”，实体却是 `Boolean`：2 会被 JDBC 映射为 true，性别语义丢失；建议改为 Integer 或枚举。
- `version` 只是普通字段 + 自动填充递增，没有 `@Version` 和 `OptimisticLockerInnerInterceptor`；`updateById` 的 WHERE 只有 id，并发会覆盖。请求未传 version 时 updateFill 不会递增。
- `is_delete` 未加 `@TableLogic`，也没有删除接口，逻辑删除配置当前不生效；需要软删除时应加注解并配置 `logic-delete-field`。
- `AutoFillHandler.updateFill` 用 `strictUpdateFill` 填 updateTime，字段已有值时不覆盖；前端回传旧 updateTime 会原样入库。
- `UserController.update` 不校验 id 和影响行数，id 不存在也返回 200；add 没有参数校验。
- `CodeGenerator` 的 outputDir 是 `user.dir + "/demo-springboot-mybatis-plus/..."`：在模块目录直接运行会生成到 `demo-springboot-mybatis-plus/demo-springboot-mybatis-plus/...`，应从仓库根目录运行或改用绝对路径；数据库连接同样硬编码。
- 生成器位于 `src/test/java`，不是 JUnit 用例，需要手动运行 main；`mybatis-plus-generator`/FreeMarker 为 provided，打包后不可用。
- `UserMapper.xml` 仅保留 BaseResultMap，BaseMapper 的 SQL 不依赖它；XML 位于默认扫描路径 `classpath*:/mapper/**/*.xml`，改目录需显式配置 `mybatis-plus.mapper-locations`。
- MySQL 8.4 下 `INT(11)` 显示宽度已废弃（仅告警），建表脚本可改为 `INT`；Druid 监控页 `allow=*` 且口令为 `druid/druid@123`，不要暴露到公网。

---

[← 返回项目总览](../README.md)
