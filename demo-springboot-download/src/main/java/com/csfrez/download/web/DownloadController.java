package com.csfrez.download.web;

import com.github.linyuzai.download.core.annotation.Download;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * @author
 * @date 2025/6/3 10:35
 * @email
 */
@Slf4j
@RestController
public class DownloadController {

    private static final String SRC_PATH = "E:/tmp/file";

    @Download
    @GetMapping("/file/{filename:.+}")
    public File file(@PathVariable String filename) {
        File file = new File(SRC_PATH + '/' + filename);
        if (!file.exists()) {
            throw new RuntimeException("下载文件不存在");
        }
        return file;
    }

    @Download(source = "classpath:/download/demo.jpeg")
    @GetMapping("/classpath")
    public void classpath() {
        log.info("下载成功");
    }

    @Download
    @GetMapping("/http")
    public String http() {
        return "http://localhost:8080/file/66ed16159fe6700001bdaf88.pdf";
    }
}
