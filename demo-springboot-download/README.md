# demo-springboot-download

> 用 `@Download` 注解完成本地文件、classpath 资源与 HTTP 资源下载的示例。

## 1. 模块定位

演示 concept-download 2.1.0 的声明式下载：Controller 方法只返回下载对象（File/String），由 starter 负责解析源、设置响应头、压缩、缓存、写流和打印进度；断点续传由 starter 的 Range 机制处理，业务侧无需手写 `Content-Range`。适合快速实现文件下载，不涉及上传。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 由父 POM 管理（2.7.18） | Web、内嵌 Tomcat |
| spring-boot-starter-web | 由父 POM 管理（2.7.18） | MVC |
| concept-download-spring-boot-starter | 由父 POM 管理（2.1.0） | @Download、压缩、缓存、Range |
| Hutool | 由父 POM 管理（5.8.33） | 仅声明依赖，源码未直接使用 |
| Lombok | 由父 POM 管理（1.18.30） | @Slf4j |
| JDK | 1.8 | 编译目标 |

## 3. 目录结构

```text
demo-springboot-download/
├── pom.xml
└── src/main/
    ├── java/com/csfrez/download/
    │   ├── DownloadApplication.java
    │   └── web/DownloadController.java
    └── resources/
        ├── application.yml
        └── download/demo.jpeg
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| DownloadApplication | 启动类，加载 concept-download 自动配置 |
| DownloadController#file | `@Download` + 返回 `File`，读取 `E:/tmp/file`；文件不存在抛 RuntimeException |
| DownloadController#classpath | `@Download(source = "classpath:/download/demo.jpeg")` + void 方法，直接下发 classpath 图片 |
| DownloadController#http | `@Download` + 返回 URL 字符串，starter 以 HttpSource 拉取远程文件再写响应，网络源可缓存 |
| @Download 处理链 | 2.x 用 `@ControllerAdvice` 拦截（不依赖 AOP）：创建 Source → 加载（HTTP 可并发）→ 压缩（多源/目录/forceCompress）→ 写响应（Range/进度）→ 清理 |
| 压缩 | 默认 zip；单个文件默认不压缩，多文件或目录才压缩；`compress.password` 需 zip4j 才生效 |
| 缓存 | `source.cache` 缓存 HTTP 源，`compress.cache` 缓存压缩包；`delete=false` 保留文件 |
| 日志 | Standard/Progress/TimeSpent 三类，进度每 500ms 输出一次 |

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | /file/{filename:.+} | path：filename | 下载 E:/tmp/file 下的文件，@Download 自动设置响应头并支持 Range |
| GET | /classpath | 无 | 下载 classpath:/download/demo.jpeg |
| GET | /http | 无 | 返回固定 URL，starter 拉取该 URL 后转发下载（源地址硬编码） |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| server.port | 8080 | HTTP 端口 |
| server.tomcat.basedir | ./tomcat-base | Tomcat 工作目录 |
| spring.servlet.multipart.max-file-size | 200MB | 本模块无上传接口，配置仅占位 |
| spring.servlet.multipart.max-request-size | 200MB | 同上 |
| concept.download.source.cache.enabled | true | 网络资源缓存开关 |
| concept.download.source.cache.path | source-cache | 相对进程工作目录，不是文档默认的 user.home/concept/download |
| concept.download.source.cache.delete | false | 下载结束保留缓存 |
| concept.download.compress.format | zip | 压缩格式 |
| concept.download.compress.password | 123456 | 压缩密码；未引入 zip4j 时加密不生效 |
| concept.download.compress.cache.enabled | true | 压缩包缓存开关 |
| concept.download.compress.cache.path | compress-cache | 相对进程工作目录 |
| concept.download.compress.cache.delete | false | 下载结束保留压缩缓存 |
| concept.download.response.headers.header1/header2 | 1/2 | 附加到下载响应的自定义头 |
| concept.download.logger.* | enabled=true；progress.duration=500；percentage=true | 标准/进度/耗时日志 |

- 服务端口：8080
- 依赖中间件：无；HTTP 源需网络可达，zip 加密需额外引入 `net.lingala.zip4j:zip4j`

## 7. 启动方式

前置条件：JDK 8、Maven；创建 `E:/tmp/file` 并放入待下载文件；`/http` 示例要求同机 8080 上存在目标 PDF；如需加密压缩包，另加 zip4j 依赖。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-download
mvn spring-boot:run
```

## 8. 调用示例

```bash
# 本地文件下载（starter 处理 Range）
curl -OJ http://localhost:8080/file/demo.pdf

# 指定前 1KB，模拟断点续传
curl -H "Range: bytes=0-1023" -o part1 http://localhost:8080/file/demo.pdf

# classpath 资源
curl -OJ http://localhost:8080/classpath

# 网络资源转发
curl -OJ http://localhost:8080/http
```

## 9. 注意事项

- `/http` 返回 `http://localhost:8080/file/66ed16159fe6700001bdaf88.pdf`，端口和文件名硬编码；服务换端口即失效，且该 PDF 必须存在于 `E:/tmp/file`，否则拉取源时 404。
- `SRC_PATH = "E:/tmp/file"` 为 Windows 硬编码路径，Linux 部署需改；文件名未做路径穿越校验。
- `source-cache`、`compress-cache` 是相对路径，实际落在进程工作目录（IDE 下通常是模块目录），不是文档默认的 `user.home/concept/download`；`delete=false` 会持续占用磁盘，需定期清理。
- `compress.password=123456` 只有在发生压缩且 classpath 中存在 zip4j 时才生效；当前 pom 未引入 zip4j，无法生成加密压缩包。
- 单个文件默认不压缩；当前三个接口都只返回一个源，要验证压缩需返回多文件/目录或改为 `@Download(forceCompress = true)`。
- 2.1.0 官方文档仍把 Range/断点续传标为 Beta，压缩包与网络缓存组合场景需实测；缓存损坏时清空对应缓存目录。
- `/classpath` 方法返回 void，只靠注解 `source` 提供资源；若改成有返回值，返回 null 时才回退到 `source`。
- `concept.download.response.headers` 会给所有下载响应加自定义头，生产环境不要放敏感信息。

---

[← 返回项目总览](../README.md)
