package com.csfrez.demospringboot.web;

import com.csfrez.demospringboot.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
public class FilePreviewController {

    @Autowired
    private FileStorageService fileStorageService;

    // 支持的文件类型映射
    private static final Map<String, MediaType> MEDIA_TYPES = new HashMap<>();

    static {
        MEDIA_TYPES.put("jpg", MediaType.IMAGE_JPEG);
        MEDIA_TYPES.put("jpeg", MediaType.IMAGE_JPEG);
        MEDIA_TYPES.put("png", MediaType.IMAGE_PNG);
        MEDIA_TYPES.put("gif", MediaType.IMAGE_GIF);
        MEDIA_TYPES.put("pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/preview/{fileName}")
    public ResponseEntity<Resource> preview(@PathVariable String fileName) throws IOException {
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

    // 图片预览
    @GetMapping("/preview/image/{fileName:.+}")
    public ResponseEntity<FileSystemResource> previewImage(@PathVariable String fileName) throws FileNotFoundException {
        Path filePath = fileStorageService.loadFile(fileName);
        File file = filePath.toFile();
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // 根据实际类型调整
                .body(new FileSystemResource(file));
    }

    // PDF预览
    @GetMapping("/preview/pdf/{fileName:.+}")
    public ResponseEntity<byte[]> previewPdf(@PathVariable String fileName) throws IOException {
        Path filePath = fileStorageService.loadFile(fileName);
        File file = filePath.toFile();
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] contents = Files.readAllBytes(file.toPath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
//        headers.setContentDispositionFormData("inline", fileName);
        headers.setContentLength(contents.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(contents);
    }

    @GetMapping("/preview/file/{fileName:.+}")
    public ResponseEntity<?> previewFile(@PathVariable String fileName) throws IOException {
        Resource resource = validateAndGetResource(fileName);
        String fileExtension = getFileExtension(fileName).toLowerCase();
        MediaType mediaType = MEDIA_TYPES.get(fileExtension);

        if (mediaType == null) {
            return ResponseEntity.status(415).body("Unsupported file type");
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    private ResponseEntity<FileSystemResource> handleImagePreview(File file, MediaType mediaType) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new FileSystemResource(file));
    }

    private ResponseEntity<byte[]> handlePdfPreview(File file, String fileName, MediaType mediaType)
            throws IOException {
        byte[] contents = Files.readAllBytes(file.toPath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
//        headers.setContentDispositionFormData("inline", fileName);
        headers.setContentLength(contents.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(contents);
    }

    private Resource validateAndGetResource(String fileName) throws FileNotFoundException, MalformedURLException {
        if (!StringUtils.hasText(fileName) || fileName.contains("..")) {
            return null;
        }
        Path filePath = fileStorageService.loadFile(fileName);
        return new UrlResource(filePath.toUri());
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
}