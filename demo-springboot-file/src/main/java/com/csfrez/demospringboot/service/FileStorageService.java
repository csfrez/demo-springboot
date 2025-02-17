package com.csfrez.demospringboot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author
 * @date 2025/2/17 15:49
 * @email
 */
@Service
public class FileStorageService {

    @Value("${pdf.storage.location}")
    private String storagePath;

    // 初始化存储目录
    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get(storagePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public Path storeFile(byte[] content, String fileName) {
        Path targetPath = Paths.get(storagePath).resolve(fileName);
        try {
            return Files.write(targetPath, content);
        } catch (IOException e) {
            throw new RuntimeException("文件存储失败: " + e.getMessage());
        }
    }

    public Path loadFile(String fileName) throws FileNotFoundException {
        Path filePath = Paths.get(storagePath).resolve(fileName);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("文件未找到: " + fileName);
        }
        return filePath;
    }
}