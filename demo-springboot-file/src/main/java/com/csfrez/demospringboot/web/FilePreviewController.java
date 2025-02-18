package com.csfrez.demospringboot.web;

import com.csfrez.demospringboot.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FilePreviewController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/preview/{fileName:.+}")
    public ResponseEntity<ByteArrayResource> previewFile(@PathVariable String fileName) {
        try {
            byte[] fileContent = fileStorageService.readFileContent(fileName);
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}