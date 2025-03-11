package com.csfrez.demospringboot.web;

import com.csfrez.demospringboot.service.PdfToImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author
 * @date 2025/2/14 14:26
 * @email
 */
@RestController
@RequestMapping("/api/pdf")
@Slf4j
public class PdfConversionController {

    @Autowired
    private PdfToImageService pdfToImageService;

    @PostMapping("/convert-to-images")
    public ResponseEntity<byte[]> convertPdfToImages(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dpi", defaultValue = "300") int dpi) throws IOException {
        log.info("Received PDF file: {}", file.getOriginalFilename());
        Long startTime = System.currentTimeMillis();
        List<byte[]> images = pdfToImageService.convertPdfToImages(file, dpi);
        Long duration = System.currentTimeMillis() - startTime;
        log.info("Conversion completed in {} ms", duration);
        // 将多个图片打包成ZIP返回
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < images.size(); i++) {
                ZipEntry entry = new ZipEntry("page_" + (i + 1) + ".jpeg");
                zos.putNextEntry(entry);
                zos.write(images.get(i));
                zos.closeEntry();
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = System.currentTimeMillis() + ".zip";
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    // 新增存储到本地的端点
    @PostMapping("/save-images-local")
    public ResponseEntity<Map<String, Object>> saveImagesToLocal(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", defaultValue = "jpg") String format,
            @RequestParam(value = "dpi", defaultValue = "300") int dpi,
            @RequestParam(value = "rotation", defaultValue = "0") int rotation,
            @RequestParam(value = "flag", defaultValue = "false") boolean flag) {
        log.info("Received PDF file: {}, format: {}, dpi: {}, rotation: {}, flag: {}", file.getOriginalFilename(), format, dpi, rotation, flag);

        // 验证旋转参数有效性
        if (rotation % 90 != 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "无效的旋转角度，只允许 0, 90, 180, 270");
            return ResponseEntity.badRequest().body(error);
        }
        Long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> savedPaths = pdfToImageService.savePdfImagesToLocal(file, dpi, format, rotation, flag);
            Long duration = System.currentTimeMillis() - startTime;
            log.info("saveImagesToLocal Conversion completed in {} ms", duration);
            response.put("success", true);
            response.put("savedFiles", savedPaths);
            response.put("message", "成功保存 " + savedPaths.size() + " 张图片");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "转换失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}