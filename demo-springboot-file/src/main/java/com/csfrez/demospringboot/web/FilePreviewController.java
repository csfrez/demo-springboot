package com.csfrez.demospringboot.web;

import com.csfrez.demospringboot.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.file.Path;

@Controller
public class FilePreviewController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/preview/{fileName}")
    public ResponseEntity<Resource> previewFile(@PathVariable String fileName) throws IOException {
        Path filePath = fileStorageService.loadFile(fileName);
        Resource resource = new UrlResource(filePath.toUri());

        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else if ("png".equals(extension)) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if ("gif".equals(extension)) {
            contentType = MediaType.IMAGE_GIF_VALUE;
        } else if ("pdf".equals(extension)) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }
}