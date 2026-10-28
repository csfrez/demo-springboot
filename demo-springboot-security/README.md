# demo-springboot-security

> 密码学与设备指纹工具集示例：AES/CFB 对称加解密、RSA 分段加解密、主板序列号采集；**不是** Spring Security 集成。

## 1. 模块定位

模块名里的 “security” 指**密码学安全**而非 Spring Security 框架：它只用 `spring-boot-starter-web` + hutool + Lombok，提供三件工具类——`AesUtil`（AES/CFB 对称加解密）、`RsaUtil`（RSA 密钥对生成与分段加解密）、`HardwareUtil`（跨平台读取主板序列号作为机器码），并由 `HelloController` 暴露 `/hello/info`（硬件信息）与 `/hello/genKey`（临时生成 RSA 密钥对）两个演示接口。

pom 中**没有** `spring-boot-starter-security` 或任何 `spring-security-*` 依赖，全模块无认证、无授权、无安全过滤器链，所有接口匿名可访问。

## 2. 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 2.7.18 | Web 容器与自动装配 |
| Java | 8 | 编译目标（`java.version=1.8`） |
| spring-boot-starter-web | 2.7.18 | `@RestController`、内嵌 Tomcat |
| hutool-all | 5.8.33 | `Base64`、`Pair`、`RandomUtil`、`JSONObject` |
| lombok | 1.18.30 | `@Slf4j`（版本由 spring-boot-dependencies 管理） |
| JDK JCE | 1.8 内置 | `Cipher`/`KeyPairGenerator`/`KeyFactory`，实现 AES 与 RSA |
| Spring Security | 未引入 | 本模块与 Spring Security 无关，接口无鉴权 |

## 3. 目录结构

```text
demo-springboot-security/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/wrapper        # 自带 Maven Wrapper
└── src/main
    ├── java/com/csfrez/secure/
    │   ├── SecurityApplication.java
    │   ├── rest/HelloController.java
    │   └── util/{AesUtil,RsaUtil,HardwareUtil}.java
    └── resources/application.properties
```

## 4. 核心实现

| 类 | 职责 |
| --- | --- |
| `SecurityApplication` | 启动类，`@SpringBootApplication`，无额外配置 |
| `HelloController` | `@RequestMapping("/hello")` 下两个 GET：`/info` 采集硬件信息、`/genKey` 生成 RSA 密钥对；另有未加映射注解的 `hello()` 死代码 |
| `AesUtil` | `AES/CFB/NoPadding`，密钥 16 字符随机串（AES-128），**IV 直接复用密钥字节**，密文 Base64 输出；`getAESRandomKey()` 生成 16 位字母数字密钥 |
| `RsaUtil` | `Cipher.getInstance("RSA")`（即 RSA/ECB/PKCS1Padding）；`genKey()` 固定 2048 位，公钥 X.509/SPKI、私钥 PKCS#8，均 Base64；`encrypt/decrypt` 支持超长内容分段（加密块 245B、解密块 256B） |
| `HardwareUtil` | `getMotherboardSerial()` 按 OS 分支取机器码；`getSystemInfo()` 拼装“系统+架构+主板序列号”摘要 |

以下加解密参数均按源码逐项核对：

- **AES**：算法/模式/填充 = `AES/CFB/NoPadding`；密钥长度 = 128 bit（16 字节 ASCII 串）；IV = 密钥本身（无独立随机 IV）；无 MAC、无认证标签。
- **RSA**：转换串仅写 `"RSA"`，SunJCE 默认展开为 `RSA/ECB/PKCS1Padding`；密钥长度 = 2048 bit；单块明文上限 = 2048/8 − 11 = **245 字节**，超过则按 245 字节切块分别 `doFinal` 后拼接；解密按 256 字节切块。
- **机器码**：Windows 执行 `wmic baseboard get serialnumber`；Linux 优先 `sudo dmidecode -s baseboard-serial-number`，失败则退化为 `cat /sys/class/dmi/id/board_serial`；两者都失败返回 `"UNKNOWN"`。

## 5. 接口清单

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/hello/info` | 无 | 返回 `{success:true, data:{motherboardSerial, systemInfo, osName, osVersion, osArch}}`，其中前两项来自 `HardwareUtil`，后三项读 `System.getProperty` |
| GET | `/hello/genKey` | 无 | 调用 `RsaUtil.genKey()`，以 `Pair` 序列化返回 `{"first":"<公钥Base64>","second":"<私钥Base64>"}`（公钥在前、私钥在后） |

## 6. 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | HTTP 服务端口 |
| `spring.application.name` | `demo-springboot-security` | 应用名，仅用于标识 |

- 服务端口：8080
- 依赖中间件：无（不依赖数据库、Redis、注册中心）

## 7. 启动方式

前置条件：JDK 8、Maven 3.6+（模块内也自带 `mvnw`）；`/hello/info` 的机器码采集需要能执行 `wmic` 或具备 `dmidecode`/`/sys` 读权限，否则返回 `UNKNOWN`。

```bash
# 首次使用：先在仓库根目录执行 mvn clean install -DskipTests
cd demo-springboot-security
mvn spring-boot:run
```

## 8. 调用示例

```bash
# 采集机器码与系统信息
curl "http://localhost:8080/hello/info"

# 生成一对 2048 位 RSA 密钥（公钥在前，私钥在后）
curl "http://localhost:8080/hello/genKey"

# 想验证加解密闭环，可直接运行工具类 main：
#   AesUtil.main  —— 随机 16 位密钥做 encrypt/decrypt 自测
#   RsaUtil.main  —— 用硬编码的示例公私钥对 JSON 报文加解密
```

## 9. 注意事项

- **私钥硬编码入库**：`RsaUtil.main()` 中写死了一对完整的 2048 位公私钥（PKCS#8 私钥 + X.509 公钥），属于典型的密钥泄漏场景，真实项目必须移除并改为配置中心/KMS 注入。
- **私钥会被打日志**：`genKey()` 用 `log.info` 打印公私钥全文，且 `/hello/genKey` 无任何鉴权，任何人可反复调用——既泄漏密钥又消耗 CPU 生成密钥对（可被用于资源耗尽攻击）。
- **RSA 填充方式未显式声明**：`Cipher.getInstance("RSA")` 依赖 JCE 默认值（PKCS#1 v1.5），存在 padding oracle 风险，新项目建议显式写 `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`；当前分段加密只解决长度问题，不提供完整性/签名校验。
- **分段逻辑的边界**：加密块按 `2048/8-11=245` 字节切分，解密按 `256` 字节切分；`getMaxBlockSize()` 的私钥分支返回 `bitLength/8`，仅在“私钥加密”这种非常规用法下才会走到，目前未被调用。
- **AES 的 IV 复用密钥**：`IvParameterSpec` 直接用 `key.getBytes()`，相同明文每次加密结果一致（确定性密文，可做重放/比对分析）；CFB 无填充但**无 MAC**，密文可被按位篡改；`getBytes()` 依赖平台默认字符集，密钥含非 ASCII 字符时长度会偏离 16 字节并抛 `InvalidKeyException`。`AesUtil` 目前只在 `main()` 中演示，未接入任何接口。
- **HardwareUtil 的平台局限**：Windows 依赖 `wmic`（Win10 21H1 起弃用、Win11 精简镜像常缺失，新版应改用 PowerShell CIM）；Linux 的 `sudo dmidecode` 在无免密 sudo 时会因等待密码而阻塞或失败，兜底读 `/sys/class/dmi/id/board_serial` 同样需要 root；容器/虚拟机中序列号常为 `None`、`Not Specified` 或空。代码未重定向 `stderr`，`process.waitFor()` 前未消费错误流，极端情况下子进程可能因缓冲区写满而卡住。主板序列号本身可被伪造，不适合单独作为可信设备指纹。
- **无 Spring Security**：模块没有安全过滤器链、没有 CSRF/CORS 配置、没有认证入口，`/hello/genKey` 等接口默认对公网裸奔；若需鉴权须自行引入框架或网关层拦截。

---

[← 返回项目总览](../README.md)
