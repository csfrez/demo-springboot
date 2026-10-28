# demo-springboot-dao

> 只承载持久层依赖的"空壳"聚合模块：把 MyBatis、Druid、tk.mybatis、PageHelper、MySQL 驱动收口给上层，自身没有一行 Java 代码。

## 1. 模块定位

- 演示"依赖聚合模块"的做法：不写实体、不写 Mapper、不写配置，只用 pom 把一整套持久层依赖打包成可复用的 jar。
- 在项目中的角色是"持久层依赖入口"：`demo-springboot-server` 通过引入本模块，间接获得 MyBatis 自动装配、Druid 连接池、tk.mybatis 通用 Mapper、PageHelper 分页和 MySQL 驱动，无需在 server 里逐个声明。
- 依赖关系：被 `demo-springboot-server` 依赖；本模块不依赖任何其它业务模块；其依赖版本全部由父 POM 的 `dependencyManagement` 决定。
- 与常见 dao 模块的最大区别：这里**没有** `src` 目录，实体（`Country`）、Mapper（`CountryMapper`）、`MyMapper` 都在 server 模块里，所以不要指望从这里拿到可复用的数据模型。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Java | 1.8 | 编译级别（父 POM `java.version`） |
| mybatis-spring-boot-starter | 2.3.2 | 由父 POM 管理（`mybatis.springboot.version`），提供 `SqlSessionFactory` 自动装配 |
| druid-spring-boot-starter | 1.2.24 | 由父 POM 管理，连接池 + 监控 Servlet |
| mapper-spring-boot-starter（tk.mybatis） | 4.3.0 | 由父 POM 管理，通用 `Mapper<T>` / `Example` / `Weekend` |
| pagehelper-spring-boot-starter | 2.1.0 | 由父 POM 管理，`PageHelper.startPage` 物理分页 |
| mysql-connector-j | 8.4.0 | 由父 POM 显式声明（覆盖 Boot BOM 原生的 8.0.33），运行时驱动 `com.mysql.cj.jdbc.Driver` |

## 3. 目录结构

```text
demo-springboot-dao/
├── pom.xml
├── mvnw / mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
└── （没有 src 目录：无 Java 源码、无 resources、无测试）
```

整个模块实际只有 5 个文件：`pom.xml`、两个 wrapper 脚本、wrapper jar 与 `maven-wrapper.properties`。

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| （无 Java 类） | 模块不含任何源码，`mvn install` 产物是一个只带 `MANIFEST.MF` 的空 jar |
| `pom.xml` 的 `<dependencies>` | 唯一的"实现"：声明 mybatis、druid、tk.mapper、pagehelper、mysql 五个依赖，靠 Maven 传递性向使用方生效 |
| `pom.xml` 的 `<description>` | `Demo project for Spring Boot`，参与父 POM enforcer 的 `requireProperty` 校验（缺失会导致构建失败） |

## 5. 接口清单

本模块不对外提供 HTTP 接口，也没有 Dubbo 服务、过滤器或工具类。它提供的是"依赖能力"，清单如下：

| 能力 | 来源依赖 | 说明 |
| --- | --- | --- |
| 数据源与连接池 | druid-spring-boot-starter 1.2.24 | 使用方配置 `spring.datasource.druid.*` 后生效，含 StatViewServlet |
| MyBatis 自动装配 | mybatis-spring-boot-starter 2.3.2 | 使用方需提供 `mybatis.mapper-locations` 等配置 |
| 通用 Mapper | mapper-spring-boot-starter 4.3.0 | 提供 `Mapper<T>`、`MySqlMapper<T>`、`Example`、`WeekendCriteria` |
| 分页 | pagehelper-spring-boot-starter 2.1.0 | 提供 `PageHelper.startPage(pageNum, pageSize)` |
| JDBC 驱动 | mysql-connector-j 8.4.0 | 运行时驱动类为 `com.mysql.cj.jdbc.Driver` |

## 6. 配置说明

本模块**没有** `application.properties` / `application.yml`，不定义任何配置项；所有配置由使用方提供。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| （本模块无配置文件） | — | 依赖引入后，配置由 `demo-springboot-server` 的 `application.properties` 提供 |
| `spring.datasource.*` | 使用方定义 | 由 druid starter 读取，本模块不设默认值 |
| `mybatis.*` | 使用方定义 | 由 mybatis starter 读取 |
| `mapper.*` | 使用方定义 | tk.mybatis 通用 Mapper 配置项（如 `mapper.mappers`、`mapper.identity`） |

- 服务端口：不启动独立服务（无主类、无配置文件）
- 依赖中间件：无（自身不连库，只把 MySQL 驱动与连接池依赖传递出去）

## 7. 启动方式

前置条件：JDK 8+、Maven；无需数据库，因为模块没有可执行代码。

```bash
# 在 demo-springboot 根目录执行：作为库安装到本地仓库
mvn -pl demo-springboot-dao -am install
```

本模块没有主类，执行 `spring-boot:run` 会直接失败（`Unable to find a suitable main class`），只能 `install` 后由 `demo-springboot-server` 以依赖方式使用。

## 8. 调用示例

引入方式（真实片段，来自 `demo-springboot-server/pom.xml`，版本由父 POM `dependencyManagement` 提供 `2.0.1-SNAPSHOT`）：

```xml
<dependency>
    <groupId>com.csfrez</groupId>
    <artifactId>demo-springboot-dao</artifactId>
</dependency>
```

引入后即可直接写 tk.mybatis 的 Mapper（真实片段，来自 server 的 `CountryMapper` / `MyMapper`）：

```java
public interface MyMapper<T> extends Mapper<T>, MySqlMapper<T> { }

public interface CountryMapper extends MyMapper<Country> { }
```

分页与条件查询（真实片段，来自 server 的 `CountryService#getAll`）：

```java
PageHelper.startPage(pageNum, pageSize);
Example example = new Example(Country.class);
example.createCriteria().andLike("countryname", "%" + name + "%");
return countryMapper.selectByExample(example);
```

## 9. 注意事项

- **无 Java 源码**：install 出的 jar 里没有任何 `.class`。如果以为这里能拿到实体/BaseMapper 复用，会落空——`Country`、`CountryMapper`、`MyMapper` 都在 server 模块内，换模块无法共享。
- **依赖传染性强**：任何引入本模块的应用都会被动获得 Druid + MyBatis + tk.mybatis + PageHelper。项目里的 `demo-springboot-mybatis-plus`（MyBatis-Plus 3.5.12）同样基于 MyBatis，两套 Mapper 生态混在一个 classpath 时，`MapperScan` 的注解来源（`tk.mybatis.spring.annotation.MapperScan` 与 MyBatis-Plus 的 `@MapperScan`）以及分页插件极易冲突。
- **驱动版本跳变**：mysql-connector-j 被父 POM 固定为 8.4.0，而 Boot 2.7.18 原生管理 8.0.33。8.4 仍提供 `com.mysql.cj.jdbc.Driver`，但连接串若未带 `serverTimezone`，在服务器时区无法识别时会抛时区解析异常。
- **不是一个真正的 starter**：模块内没有 `spring.factories` / `AutoConfiguration.imports`，不会带来任何自动配置，只是依赖集合；想做成 starter 必须自己补齐自动配置类。
- **父 POM enforcer 依赖**：根 pom 的 `maven-enforcer-plugin` 要求 `project.name` 与 `project.description` 必须存在，本模块 pom 已声明；新增同类聚合模块时漏写会直接构建失败。

---

[← 返回项目总览](../README.md)
