# IFLOW.md - demo-springboot 项目指南

## 项目概述

这是一个基于 Spring Boot 的综合性演示项目，采用多模块架构设计。项目包含了多个子模块，展示了各种 Spring Boot 集成技术，包括但不限于 MyBatis、Dubbo、Redis、WebSocket、AI 集成等。该项目使用 Maven 作为构建工具，Java 版本为 1.8。

## 项目结构

项目包含以下主要模块：

- `demo-springboot-admin`: Spring Boot Admin 服务端和客户端模块
- `demo-springboot-dao`: 数据访问层模块，整合了 MyBatis、Druid、通用 Mapper 等
- `demo-springboot-dubbo`: Dubbo RPC 服务模块，使用 Zookeeper 作为注册中心
- `demo-springboot-server`: 主服务器模块，整合了 MongoDB、Redis、Thymeleaf 等
- `demo-springboot-client`: 客户端模块，与 Admin 和 Dubbo 集成
- `demo-springboot-common`: 公共模块，包含通用工具和配置
- `demo-springboot-security`: 安全模块
- `demo-springboot-websocket`: WebSocket 模块，用于实时通信
- `demo-springboot-ai`: AI 集成模块
- `demo-springboot-file`: 文件处理模块
- `demo-springboot-jsqlparser`: SQL 解析模块
- `demo-springboot-retry`: 重试机制模块
- `demo-springboot-mybatis-plus`: MyBatis-Plus 集成模块
- `demo-springboot-ws`: Web Service 模块
- `demo-springboot-download`: 文件下载模块
- `demo-springboot-token`: Token 管理模块
- `demo-springboot-thread`: 多线程处理模块
- `demo-springboot-redis`: Redis 缓存模块

## 核心技术栈

- **Spring Boot**: 2.7.18 - 作为基础框架
- **MyBatis/MyBatis-Plus**: 用于数据库操作
- **Dubbo**: 2.7.23 - RPC 框架
- **Redis**: 缓存和会话管理
- **MongoDB**: NoSQL 数据库
- **MySQL**: 关系型数据库
- **Spring Security**: 安全框架
- **Spring WebSocket**: 实时通信
- **Spring AMQP**: RabbitMQ 集成
- **Lombok**: 减少样板代码
- **Hutool**: Java 工具类库
- **Guava**: Google Java 工具库
- **Netty**: WebSocket 底层网络框架
- **Spring Boot Admin**: 应用监控

## 构建与运行

### 环境要求
- Java 1.8+
- Maven 3.6.0+

### 构建项目
```bash
mvn clean install
```

### 运行模块
根据需要运行不同的模块:

1. **Admin 服务**:
   ```bash
   cd demo-springboot-admin
   mvn spring-boot:run
   ```

2. **Server 服务**:
   ```bash
   cd demo-springboot-server
   mvn spring-boot:run
   ```

3. **Client 服务**:
   ```bash
   cd demo-springboot-client
   mvn spring-boot:run
   ```

### MyBatis 代码生成
项目支持 MyBatis 代码自动生成：
1. 配置 `generator/generatorConfig.xml` 文件
2. 设置编码（解决中文乱码）:
   ```bash
   set MAVEN_OPTS="-Dfile.encoding=UTF-8"
   ```
3. 执行生成命令:
   ```bash
   mvn mybatis-generator:generate
   ```

### 版本管理
- 升级版本: `mvn versions:set -DnewVersion=2.0.0-SNAPSHOT`
- 确认版本: `mvn versions:commit`

## 开发规范

- 代码风格遵循 Java 编码规范
- 使用 Lombok 减少样板代码
- 项目使用 UTF-8 编码
- 依赖管理统一在父 POM 中进行
- 模块间通过 Maven 依赖进行解耦

## 测试

每个模块都包含了测试依赖，可以运行：
```bash
mvn test
```

## 注意事项

1. 项目使用了多种数据库和技术栈，运行相关模块前确保相应服务已启动
2. 部分模块可能需要配置外部服务，如 Redis、MongoDB、Zookeeper 等
3. 项目中的 Dubbo 部分使用 Zookeeper 作为注册中心，需要先启动 Zookeeper 服务
4. WebSocket 模块使用了 Netty 和 Redis，用于分布式 WebSocket 通信