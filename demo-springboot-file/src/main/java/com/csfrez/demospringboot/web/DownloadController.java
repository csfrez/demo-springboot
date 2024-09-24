package com.csfrez.demospringboot.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

@Controller
public class DownloadController {

    private static final String SRC_PATH = "E:/tmp/file";

    /**
     * 通过文件名获取文件并以流的形式返回给客户端
     *
     * @param filename
     * @param response
     */
    @GetMapping("/download/{filename:.+}")
    public void download(@PathVariable String filename, HttpServletResponse response) throws Exception {
        File file = new File(SRC_PATH + '/' + filename);
        if (!file.exists()) {
            throw new RuntimeException("下载文件不存在");
        }
        long length = file.length();

        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        response.setContentLength((int) length);
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));

        // 使用缓存流，边读边写
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            OutputStream os = response.getOutputStream();
            byte[] buff = new byte[1024];

            int total = (int) length / 1024 + 1;
            int times = 0;
            int i;
            while ((i = bis.read(buff)) != -1) {
                os.write(buff, 0, i);
                os.flush();
                times++;
                double progress = (double) times / total * 100;
                System.out.println(i + ", 下载进度：" + String.format("%.2f", progress) + "%");
            }
        } catch (IOException e) {
            throw new RuntimeException("下载文件失败");
        }
    }
}