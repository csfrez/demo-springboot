# demo-springboot-file

> 基于 Spring Boot 2.7 的本地文件上传、下载、预览与图片/PDF 互转示例。

## 1. 模块定位

演示以本地磁盘为存储后端的一站式文件处理：Multipart 上传、Thumbnailator 图片压缩、PDFBox 图片转 PDF 与 PDF 转图片、inline 预览/附件下载、Apache Commons FileUpload 传统上传，以及 Thymeleaf 上传页面。模块偏演示，没有鉴权、去重、分片和对象存储。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 由父 POM 管理（2.7.18） | Web、内嵌 Tomcat、配置 |
| spring-boot-starter-web | 由父 POM 管理（2.7.18） | MVC 与 multipart |
| spring-boot-starter-thymeleaf | 由父 POM 管理（2.7.18） | 渲染 templates/upload.html |
| Apache PDFBox | 2.0.33 | 图片合成 PDF、PDF 渲染为图片 |
| Thumbnailator | 0.4.20 | 图片缩放、质量压缩 |
| commons-fileupload | 1.5 | ServletFileUpload 传统上传 |
| commons-io | 2.7 | IOUtils 流拷贝 |
| Hutool | 由父 POM 管理（5.8.33） | 雪花 ID、随机数 |
| Guava | 由父 POM 管理（33.3.0-jre） | 仅声明依赖，源码未直接使用 |
| Lombok | 由父 POM 管理（1.18.30） | @Slf4j |
| JDK | 1.8 | 编译目标 |

## 3. 目录结构

```text
demo-springboot-file/
├── pom.xml
└── src/main/
    ├── java/com/csfrez/demospringboot/
    │   ├── DemoSpringbootFileApplication.java
    │   ├── config/FileExceptionHandler.java
    │   ├── service/{FileStorageService,ImageToPdfService,PdfToImageService}.java
    │   ├── tool/ImageRotator.java
    │   └── web/{ApacheFile,Download,File,FilePreview,Image,PdfConversion,Web}Controller.java
    └── resources/
        ├── application.properties
        └── templates/upload.html
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| FileStorageService | `@PostConstruct` 创建 `file.storage.location`；按文件名读写；`detectFileType` 用魔数识别 PDF/JPEG/PNG/GIF/WebP（当前无调用） |
| ImageToPdfService | `convertImagesToPdf` 先用 Thumbnailator 限 1920×1080、再以 `LosslessFactory` 嵌入；`convertImagesToPdf2` 直接嵌入原图字节，页面尺寸等于像素尺寸 |
| PdfToImageService | `PDFRenderer.renderImageWithDPI` 逐页渲染，默认 300 DPI；JPEG 质量 0.9 或 PNG 写盘，文件名为 `原名_yyyyMMddHHmmss_pageNNN.ext` |
| ImageRotator | 仅支持 90° 倍数旋转，按新画布尺寸平移并绕中心旋转 |
| ImageController | 上传/批量压缩图片，统一输出 JPEG，文件名用 Hutool 雪花 ID；返回 `file.download.base-url + 文件名` |
| FileController | 图片转 PDF 后直接下载或落盘；按扩展名设置 Content-Type，`Content-Disposition: attachment` |
| PdfConversionController | PDF 转多页 JPEG 后打包 ZIP；或逐页落盘并返回绝对路径；校验 `rotation % 90` |
| FilePreviewController | inline 预览图片/PDF，扩展名白名单，未知类型返回 415 |
| ApacheFileController | `DiskFileItemFactory` + `ServletFileUpload` 解析表单，文件落到 `E:/tmp/file`，临时目录 `E:/tmp/file/cache` |
| DownloadController | 原生 `HttpServletResponse` 流式下载 `E:/tmp/file`，1KB 缓冲边读边 flush 并打印进度 |
| FileExceptionHandler | `FileNotFoundException` 返回 404，其余异常统一 500 JSON |
| upload.html | 双文件 + 文本字段表单，POST `/apacheFileUpload`，`enctype=multipart/form-data` |

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| POST | /api/file/convert-to-pdf | multipart/form-data：files（多文件） | 压缩后合成 PDF，附件返回 |
| POST | /api/file/convert-to-pdf2 | multipart/form-data：files | 原图字节合成 PDF，附件返回 |
| POST | /api/file/convert-and-save | multipart/form-data：files | 合成 PDF 存入 file-storage，返回 `downloadUrl` |
| GET | /api/file/download/{fileName:.+} | path：fileName | 附件下载，扩展名决定 Content-Type |
| POST | /api/pdf/convert-to-images | multipart/form-data：file；query：dpi=300 | 逐页转 JPEG 并打包 ZIP |
| POST | /api/pdf/save-images-local | multipart/form-data：file；query：format=jpg、dpi=300、rotation=0、flag=false | 逐页落盘，返回 savedFiles |
| POST | /api/image/upload | multipart/form-data：file | 压缩到 2000×1500 内的 JPEG，返回 url |
| POST | /api/image/compress | multipart/form-data：files | 批量压缩，返回 url 数组 |
| GET | /api/image/download/{filename} | path：filename | 以 image/jpeg 附件下载 |
| GET | /download/{filename:.+} | path：filename | 从 E:/tmp/file 流式下载 |
| GET | /preview/{fileName} | path：fileName | 按扩展名 inline 预览 |
| GET | /preview/image/{fileName:.+} | path：fileName | 图片预览（固定 image/jpeg） |
| GET | /preview/pdf/{fileName:.+} | path：fileName | PDF inline 预览（整文件读入内存） |
| GET | /preview/file/{fileName:.+} | path：fileName | 白名单预览，未知类型 415 |
| GET | /upload | 无 | 返回 upload 视图 |
| POST | /apacheFileUpload | multipart/form-data：files、name | Commons FileUpload 保存到 E:/tmp/file |
| GET | /hello | 无 | 连通性测试 |
| GET | /test | 无 | 随机抛异常，验证全局异常处理 |
| GET | /user | query：name、age | 参数校验示例 |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| server.port | 8080 | HTTP 端口 |
| spring.servlet.multipart.max-file-size | 200MB | 单文件上限 |
| spring.servlet.multipart.max-request-size | 200MB | 单请求上限 |
| spring.servlet.multipart.enabled | true（配置中被注释） | 关闭后由 Commons FileUpload 直接解析原始请求 |
| file.storage.location | ./file-storage | 文件存储根目录，启动时自动创建 |
| file.download.base-url | http://10.96.2.190:8080/api/file/download/ | 拼接返回给前端的下载地址（内网硬编码） |
| pdf.image.auto-create-dir | true（@Value 代码默认） | PDF 转图片落盘前是否创建目录 |
| server.tomcat.basedir | ./tomcat-base | Tomcat 工作/临时目录 |

- 服务端口：8080
- 依赖中间件：无；使用本地磁盘，不依赖数据库、缓存或 MQ

## 7. 启动方式

前置条件：JDK 8、Maven；`./file-storage` 可写；使用 `/download` 或 Apache 上传时预先创建 `E:/tmp/file`、`E:/tmp/file/cache`；把 `file.download.base-url` 改为本机地址。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-file
mvn spring-boot:run
```

## 8. 调用示例

```bash
# 图片压缩上传
curl -F "file=@demo.jpg" http://localhost:8080/api/image/upload

# 多图合成 PDF 并落盘
curl -F "files=@1.jpg" -F "files=@2.png" http://localhost:8080/api/file/convert-and-save

# PDF 转图片 ZIP
curl -F "file=@demo.pdf" "http://localhost:8080/api/pdf/convert-to-images?dpi=300" -o pages.zip

# 查看预览响应头
curl -I http://localhost:8080/preview/pdf/demo.pdf
```

## 9. 注意事项

- `file.download.base-url` 硬编码内网 `10.96.2.190:8080`，换机或换端口后返回的 URL 不可用；建议改为配置注入或 `ServletUriComponentsBuilder` 动态生成。
- `DownloadController`、`ApacheFileController` 硬编码 `E:/tmp/file`，且不会自动创建目录；父目录或 `E:/tmp/file/cache` 不存在时 `FileOutputStream` 直接失败。
- Apache Commons FileUpload 与 Spring 默认 multipart 解析共存：`spring.servlet.multipart.enabled=false` 目前被注释，Spring 会先解析 multipart，Controller 中 `upload.parseRequest(request)` 可能得到空列表；两种上传方式应只保留一种。
- `FileStorageService.loadFile`/`buildFilePath` 直接 `resolve` 文件名，未 normalize 或校验根目录前缀；`ApacheFileController` 直接用客户端 `FileItem.getName()` 拼路径，存在目录穿越和同名覆盖风险。
- 图片上传/压缩无论源格式一律输出 `.jpg` 且 `outputQuality(1)`，PNG 透明区域可能变黑；`size(2000,1500)` 只等比缩放、默认不放大。
- `ImageToPdfService.convertImagesToPdf` 的 `outputQuality(0.8)` 在 `asBufferedImage()` 阶段不参与编码，最终用 `LosslessFactory` 无损嵌入，PDF 体积不会明显减小；页面尺寸按像素点（72 DPI）设置，不是 A4。
- `PdfToImageService` 默认 300 DPI，ZIP 接口会把所有页图像同时放在内存；页数多或尺寸大时容易 OOM。`format` 只处理 png/jpg/jpeg，其他值返回路径但未写文件；原文件名无扩展名时 `lastIndexOf('.') == -1` 会抛 `StringIndexOutOfBoundsException`。
- `FilePreviewController.previewFile` 对含 `..` 的入参返回 `null` 后仍执行 `body(null)`，实际得到 200 空响应；`preview/{fileName}` 没有扩展名白名单。
- `DownloadController` 用 `(int) file.length()` 设置 Content-Length，超过 2GB 会溢出；中文文件名用 `URLEncoder.encode` 拼在未加引号的 `filename=` 中，部分浏览器会乱码。
- `FileExceptionHandler` 捕获所有 `Exception` 并返回 500，会覆盖参数错误、404 等更精确的状态码。

---

[← 返回项目总览](../README.md)
