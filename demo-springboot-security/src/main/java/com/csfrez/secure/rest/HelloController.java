package com.csfrez.secure.rest;

import cn.hutool.core.lang.Pair;
import com.csfrez.secure.util.HardwareUtil;
import com.csfrez.secure.util.RsaUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author
 * @date 2025/11/4 14:42
 * @email
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    public String hello() {
        return "hello world";
    }

    /**
     * 获取当前硬件信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getHardwareInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        Map<String, String> hardwareInfo = new HashMap<>();
        hardwareInfo.put("motherboardSerial", HardwareUtil.getMotherboardSerial());
        hardwareInfo.put("systemInfo", HardwareUtil.getSystemInfo());
        hardwareInfo.put("osName", System.getProperty("os.name"));
        hardwareInfo.put("osVersion", System.getProperty("os.version"));
        hardwareInfo.put("osArch", System.getProperty("os.arch"));

        response.put("data", hardwareInfo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/genKey")
    public ResponseEntity<Pair<String, String>> genKey() throws Exception {
        Pair<String, String> pair = RsaUtil.genKey();
        return ResponseEntity.ok(pair);
    }
}
