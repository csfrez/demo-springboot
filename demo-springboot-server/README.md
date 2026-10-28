# demo-springboot-server

> 全能型提供者：同一个进程里同时暴露 Dubbo 服务、tk.mybatis + PageHelper 的 REST 接口和 Thymeleaf 页面。

## 1. 模块定位

- 演示三件事：Dubbo 服务提供（`@Service`）、tk.mybatis 通用 Mapper 的 CRUD 与两种条件构造（`Example` / `WeekendCriteria`）、Thymeleaf 视图路由。
- 项目中的角色是"被消费的服务端"：`demo-springboot-client` 经 ZooKeeper 找到它暴露的 `DemoService`，它自己把状态上报到 9999 的 SBA 管理台。
- 依赖 `demo-springboot-dao`（Druid/MyBatis/tk.mapper/PageHelper/MySQL 驱动）与 `demo-springboot-dubbo`（Dubbo starter + 过滤器）；另引入 Mongo、Redis、Security 配置但代码未真正使用，见第 9 节。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java / Spring Boot | 1.8 / 2.7.18 | 编译级别；Boot 由父 POM 管理（`spring-boot-dependencies` import） |
| spring-boot-starter-web / thymeleaf / data-mongodb / data-redis | 2.7.18 | 由父 POM 管理；MVC、模板渲染、Mongo 与 Redis（后两者无代码使用） |
| spring-boot-admin-starter-client | 2.7.16 | 由父 POM 管理（`spring-boot-admin-dependencies`），上报 SBA |
| demo-springboot-dao / demo-springboot-dubbo | 2.0.1-SNAPSHOT | `project.version`，持久层依赖集合 / Dubbo starter + 过滤器 |
| mybatis-spring-boot-starter / tk.mapper / druid / pagehelper | 2.3.2 / 4.3.0 / 1.2.24 / 2.1.0 | 均由父 POM 管理，经 dao 模块传递 |
| mysql-connector-j | 8.4.0 | 由父 POM 显式声明（经 dao 传递），驱动 `com.mysql.cj.jdbc.Driver` |
| mybatis-generator-maven-plugin / mapper-generator / mysql-connector-java | 1.3.7 / 1.0.5 / 5.1.47 | 模块内属性管理，仅代码生成插件使用（旧驱动，见第 9 节） |

## 3. 目录结构

```text
demo-springboot-server/
├── pom.xml｜mvnw｜mvnw.cmd｜.mvn/wrapper/
└── src/main/
    ├── java/com/csfrez/demospringboot/
    │   ├── DemoSpringbootServerApplication.java   config/WebMvcConfig.java
    │   ├── dubbo/{DemoService,DefaultDemoService}.java
    │   ├── mapper/CountryMapper.java              model/Country.java
    │   ├── rest/CountryController.java            service/CountryService.java
    │   └── util/MyMapper.java
    └── resources/
        ├── application.properties                   generator/generatorConfig.xml
        ├── mapper/CountryMapper.xml
        └── templates/{home,hello,login}.html
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `DemoSpringbootServerApplication` | 启动类；`@SpringBootApplication` + tk.mybatis 的 `@MapperScan("com.csfrez.demospringboot.mapper")` |
| `CountryController` | `@RestController @RequestMapping("/country")`，5 个端点：分页查询 2 个、按主键查/删、保存（有 id 则更新） |
| `CountryService` | `PageHelper.startPage` + `Example`/`WeekendCriteria` 两套条件查询；`save` 按 id 是否为 null 决定 insert/update |
| `CountryMapper` / `MyMapper` | Mapper 继承 `MyMapper<Country>`；`MyMapper` 组合 `Mapper<T>` + `MySqlMapper<T>`，注释警告其不能被扫描 |
| `Country` | JPA 注解实体：`@Id`、`@Column(name = "Id")`、`@GeneratedValue(IDENTITY)`，字段 `countryname`、`countrycode` |
| `DefaultDemoService` | 用 `org.apache.dubbo.config.annotation.Service` 暴露 Dubbo 服务，`sayHello` 返回带时间戳字符串 |
| `WebMvcConfig` | `addViewControllers` 把 `/`、`/home`、`/hello`、`/login` 映射到 Thymeleaf 模板 |
| `generatorConfig.xml` | MyBatis Generator：tk `MapperPlugin`、`country` 表、`MyBatis3Simple`、目标包 `com.csfrez.demospringboot.{model,mapper}` |

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| 任意方法（`@RequestMapping`） | `/country/getall` | query：`countryname`、`countrycode`、`pageNum`、`pageSize`（后两个 `int`，**必填**） | `Example` 模糊查询 + PageHelper 分页，返回 `List<Country>` |
| 任意方法（`@RequestMapping`） | `/country/getallbyweekend` | 同上 | `WeekendCriteria` 版本，行为一致 |
| 任意方法（`@RequestMapping`） | `/country/view/{id}` | path：`id` | `selectByPrimaryKey`，返回 `Country` |
| 任意方法（`@RequestMapping`） | `/country/delete/{id}` | path：`id` | `deleteByPrimaryKey`，返回 `delete success {id}` |
| 任意方法（`@RequestMapping`） | `/country/save` | query：`id`、`countryname`、`countrycode` | id 为空 insert，否则 `updateByPrimaryKey`，返回 `success {id}` |
| GET | `/`、`/home`、`/hello`、`/login` | — | Thymeleaf 视图路由（非 JSON） |
| Dubbo（dubbo 协议） | `DemoService#sayHello` | `String name` | 服务提供者，端口由 `dubbo.protocol.port=-1` 自动选择 |
| GET | `/actuator/*` | — | 在 8081 管理端口暴露 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.application.name` / `server.port` / `management.server.port` / `exposure.include` | `demo-springboot-server` / `8080` / `8081` / `*` | 应用名、业务端口、管理端点独立端口（全开） |
| `logging.level.root` / `.mapper` | `INFO` / `TRACE` | mapper 包 TRACE 打印 SQL 与参数 |
| `spring.datasource.url` / `username` / `password` / `driver-class-name` | `jdbc:mysql://10.11.0.136:3306/test?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true` / `root` / `123456` / `com.mysql.cj.jdbc.Driver` | 内网 MySQL 的 `test` 库，明文口令 |
| `spring.datasource.druid.*` | `initial-size=1`、`min-idle=10`、`max-active=50`、`test-on-borrow=true`、`filters=stat`、`stat-view-servlet.enabled=true`、`allow=*`、`druid`/`druid@123` | 池参数与 `/druid` 监控页（口令明文，`initial-size` 小于 `min-idle`） |
| `mybatis.type-aliases-package` / `mapper-locations` | `com.csfrez.springboot.model` / `classpath:mapper/*.xml` | 别名包**与实际包名不一致**，见第 9 节 |
| `mapper.mappers` / `not-empty` / `identity` | `com.csfrez.demospringboot.util.MyMapper` / `false` / `MYSQL` | tk.mybatis 基类、空串是否参与查询、主键方言 |
| `spring.data.mongodb.uri` | `mongodb://opengate:123456@10.1.108.122:27017/opengate_sit` | 内网 Mongo，带账号口令 |
| `spring.redis.*` | `10.67.1.33`、`6379`、空口令、`2000ms`、`database=4`、`jedis.pool.max-active=100`、`max-idle=10` | 内网 Redis；jedis 池参数在默认 Lettuce 下不生效 |
| `spring.security.user.*` / `spring.boot.admin.client.url` | `admin` / `admin` / `http://127.0.0.1:9999` | security 未引入故无效；SBA 上报地址 |
| `dubbo.registry.*` / `dubbo.protocol.*` / `dubbo.consumer.*` | `zookeeper://10.67.7.1:2181`、`timeout=10000`、`file=${user.home}/dubbo-cache/...` / `dubbo`、`-1`、`1024`、`UTF-8` / `check=false`、`leastactive`、`60000`、`javassist`、`retries=0` | 注册中心（内网 IP）、协议端口自动挑选、消费端默认值 |

- 服务端口：8080；管理端点 8081；Dubbo 端口自动分配
- 依赖中间件：MySQL `10.11.0.136:3306`、MongoDB `10.1.108.122:27017`、Redis `10.67.1.33:6379`、ZooKeeper `10.67.7.1:2181`、SBA `127.0.0.1:9999`

## 7. 启动方式

前置条件：JDK 8+、Maven；MySQL 可连且 `test` 库存在 `country` 表；ZooKeeper 可达；Redis/Mongo 不通时通常仍能启动，但健康检查与首次访问会暴露问题；SBA 可选。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests（同时安装 dao、dubbo）
cd demo-springboot-server
mvn spring-boot:run
```

访问 <http://127.0.0.1:8080/home>，管理端点在 <http://127.0.0.1:8081/actuator>。

## 8. 调用示例

```bash
curl "http://127.0.0.1:8080/country/getall?pageNum=1&pageSize=10"   # 分页（两参数必填）
curl "http://127.0.0.1:8080/country/getall?countryname=Ch&pageNum=1&pageSize=5"
curl "http://127.0.0.1:8080/country/getallbyweekend?pageNum=1&pageSize=10"
curl http://127.0.0.1:8080/country/view/1
curl "http://127.0.0.1:8080/country/save?countryname=China&countrycode=CN"
curl http://127.0.0.1:8080/country/delete/1
curl http://127.0.0.1:8080/home
curl http://127.0.0.1:8081/actuator/health
```

## 9. 注意事项

- **`mybatis.type-aliases-package` 包名写错**：配置为 `com.csfrez.springboot.model`，实际实体在 `com.csfrez.demospringboot.model`；目前 XML 用全限定名所以不报错，改用短别名就会解析失败。
- **分页参数是原始 `int`，GET 还能删改数据**：不传 `pageNum`/`pageSize` 直接 400（Service 里 `<=0` 才纠正为 1/10）；`delete`、`save` 均为 `@RequestMapping` 未限定 method，浏览器地址栏即可触发写操作，`Country` 作为命令对象绑定，任何 query 参数都会写进实体。
- **代码生成器跑不起来**：generator 插件固定 `mysql-connector-java:5.1.47`，`generatorConfig.xml` 却用 `com.mysql.cj.jdbc.Driver`；5.1.x 只有 `com.mysql.jdbc.Driver`，`mvn mybatis-generator:generate` 会 `ClassNotFoundException`。
- **Redis/Security 配置基本无效、内网明文入库**：默认 Lettuce 却只配 `jedis.pool.*` 且未引入 jedis；配了 `spring.security.user.*` 但无 security 依赖，`/login` 无认证、`hello.html` 的 `remoteUser` 渲染成 `Hello !`；MySQL/Mongo/Redis/ZK 全是 `10.x` 内网 IP 与明文口令，换环境必须整体替换。

---

[← 返回项目总览](../README.md)
