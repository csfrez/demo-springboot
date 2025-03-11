package com.csfrez.demospringboot.web;

import cn.hutool.core.util.IdUtil;
import com.csfrez.demospringboot.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author
 * @date 2025/2/18 9:06
 * @email
 */
@RestController
@Slf4j
@RequestMapping("/api/image")
public class ImageController {

//    @Value("${image.storage.location}")
//    private String imageStorageLocation;
//
    @Value("${file.download.base-url}")
    private String downloadBaseUrl;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("请选择要上传的文件");
        }

        try {
            // 生成唯一文件名
            String compressedFilename = IdUtil.getSnowflakeNextIdStr() + ".jpg";
            Path outputPath = fileStorageService.buildFilePath(compressedFilename);
            //Path outputPath = Paths.get(imageStorageLocation).resolve(compressedFilename);

            try (InputStream inputStream = file.getInputStream()){
                // 压缩图片并保存
                Thumbnails.of(inputStream)
//                        .scale(1)
                        .size(2000, 1500)
                        .outputFormat("jpeg")
                        .outputQuality(1)
                        .toFile(outputPath.toFile());
            }
            String downloadUrl = downloadBaseUrl + compressedFilename;
            // 生成下载链接
//            String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
//                    .path("/download/")
//                    .path(compressedFilename)
//                    .toUriString();

            return ResponseEntity.ok().body(Collections.singletonMap("url", downloadUrl));

        } catch (IOException e) {
            log.error("文件处理失败", e); // 记录错误日志
            return ResponseEntity.internalServerError().body("文件处理失败");
        }
    }

    @PostMapping("/compress")
    public ResponseEntity<?> compressImage(@RequestParam("files") MultipartFile[] files) {
        if (files.length == 0) {
            return ResponseEntity.badRequest().body("请选择要上传的文件");
        }
        List<String> downloadUrlList = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                // 生成唯一文件名
                String compressedFilename = IdUtil.getSnowflakeNextIdStr() + ".jpg";
                Path outputPath = fileStorageService.buildFilePath(compressedFilename);
                try (InputStream inputStream = file.getInputStream()){
                    // 压缩图片并保存
                    Thumbnails.of(inputStream)
//                        .scale(1)
                            .size(2000, 1500)
                            .outputFormat("jpeg")
                            .outputQuality(1)
                            .toFile(outputPath.toFile());
                }
                String downloadUrl = downloadBaseUrl + compressedFilename;
                downloadUrlList.add(downloadUrl);
            }
            return ResponseEntity.ok().body(downloadUrlList);
        } catch (IOException e) {
            log.error("文件处理失败", e); // 记录错误日志
            return ResponseEntity.internalServerError().body("文件处理失败");
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
//            Path filePath = Paths.get(imageStorageLocation).resolve(filename).normalize();
            Path filePath = fileStorageService.loadFile(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException | FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}