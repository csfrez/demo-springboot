package com.csfrez.demospringboot.web;

import com.csfrez.demospringboot.service.FileStorageService;
import com.csfrez.demospringboot.service.ImageToPdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * @author
 * @date 2025/2/14 17:52
 * @email
 */
@RestController
@RequestMapping("/api/file")
@Slf4j
public class FileController {

    @Value("${pdf.download.base-url}")
    private String downloadBaseUrl;

    @Autowired
    private ImageToPdfService imageToPdfService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/convert-to-pdf")
    public ResponseEntity<Resource> convertToPdf(
            @RequestParam("files") MultipartFile[] files) throws IOException {
        Long startTime = System.currentTimeMillis();
        // 调用服务处理文件
        byte[] pdfBytes = imageToPdfService.convertImagesToPdf(files);
        Long duration = System.currentTimeMillis() - startTime;
        log.info("convertToPdf {} files to PDF in {} ms", files.length, duration);
        // 创建返回资源
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);
        String filename =  System.currentTimeMillis() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @PostMapping("/convert-to-pdf2")
    public ResponseEntity<Resource> convertToPdf2(
            @RequestParam("files") MultipartFile[] files) throws IOException {
        Long startTime = System.currentTimeMillis();
        // 调用服务处理文件
        byte[] pdfBytes = imageToPdfService.convertImagesToPdf2(files);
        Long duration = System.currentTimeMillis() - startTime;
        log.info("convertImagesToPdf2 {} files to PDF in {} ms", files.length, duration);
        // 创建返回资源
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);
        String filename =  System.currentTimeMillis() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @PostMapping("/convert-and-save")
    public ResponseEntity<Map<String, String>> convertAndSave(
            @RequestParam("files") MultipartFile[] files) throws IOException {
        Long startTime = System.currentTimeMillis();
        byte[] pdfBytes = imageToPdfService.convertImagesToPdf2(files);

        // 存储到本地
        String fileName = "converted_" + System.currentTimeMillis() + ".pdf";
        String downloadName = fileStorageService.storeFile(pdfBytes, fileName);
        String downloadUrl = downloadBaseUrl + downloadName;

        Long duration = System.currentTimeMillis() - startTime;
        log.info("convertAndSave {} files to PDF in {} ms", files.length, duration);

        return ResponseEntity.ok(Collections.singletonMap("downloadUrl", downloadUrl));
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("下载失败: " + e.getMessage());
        }
    }

}