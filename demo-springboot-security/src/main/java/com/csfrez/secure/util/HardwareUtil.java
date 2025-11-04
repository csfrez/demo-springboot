package com.csfrez.secure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @author
 * @date 2025/11/4 14:37
 * @email
 */
public class HardwareUtil {

    private static final Logger logger = LoggerFactory.getLogger(HardwareUtil.class);

    /**
     * 获取主板序列号，支持Windows和Linux系统
     */
    public static String getMotherboardSerial() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("windows")) {
                return getWindowsMotherboardSerial();
            } else if (os.contains("linux")) {
                return getLinuxMotherboardSerial();
            } else {
                logger.warn("不支持的操作系统: {}", os);
                return "UNKNOWN";
            }
        } catch (Exception e) {
            logger.error("获取主板序列号失败", e);
            return "UNKNOWN";
        }
    }

    /**
     * Windows系统通过WMI命令获取主板序列号
     */
    private static String getWindowsMotherboardSerial() throws Exception {
        Process process = Runtime.getRuntime().exec("wmic baseboard get serialnumber");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.equals("SerialNumber")) {
                logger.debug("Windows主板序列号: {}", line);
                return line;
            }
        }

        reader.close();
        process.waitFor();
        return "UNKNOWN";
    }

    /**
     * Linux系统通过dmidecode命令获取主板序列号
     */
    private static String getLinuxMotherboardSerial() throws Exception {
        try {
            // 优先使用dmidecode命令
            Process process = Runtime.getRuntime().exec("sudo dmidecode -s baseboard-serial-number");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null && !line.trim().isEmpty() && !line.contains("Not Specified")) {
                logger.debug("Linux主板序列号: {}", line.trim());
                return line.trim();
            }

            // 备选方案：读取系统文件
            return getLinuxMotherboardFromSys();

        } catch (Exception e) {
            logger.error("dmidecode命令执行失败", e);
            return getLinuxMotherboardFromSys();
        }
    }

    /**
     * 从/sys/class/dmi/id/board_serial文件读取主板序列号
     */
    private static String getLinuxMotherboardFromSys() {
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/class/dmi/id/board_serial");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null && !line.trim().isEmpty()) {
                logger.debug("Linux主板序列号(从sys读取): {}", line.trim());
                return line.trim();
            }

        } catch (Exception e) {
            logger.warn("从/sys文件读取失败", e);
        }

        return "UNKNOWN";
    }

    /**
     * 获取系统信息摘要，用于调试和展示
     */
    public static String getSystemInfo() {
        return String.format("操作系统: %s %s, 架构: %s, 主板序列号: %s",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                getMotherboardSerial()
        );
    }
}