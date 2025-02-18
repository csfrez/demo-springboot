package com.csfrez.demospringboot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    @Value("${file.storage.location}")
    private String storagePath;

    // 初始化存储目录
    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get(storagePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public Path buildFilePath(String fileName) {
        return Paths.get(storagePath).resolve(fileName);
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

    public byte[] readFileContent(String fileName) throws IOException {
        Path filePath = Paths.get(storagePath).resolve(fileName);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("文件不存在");
        }
        return Files.readAllBytes(filePath);
    }

    public static String detectFileType(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] header = new byte[16];
            int read = is.read(header);

            // PDF检测（前4字节为%PDF）
            if (read >= 4 &&
                    header[0] == 0x25 && // %
                    header[1] == 0x50 && // P
                    header[2] == 0x44 && // D
                    header[3] == 0x46) { // F
                return MediaType.APPLICATION_PDF_VALUE;
            }

            // JPEG检测（前2字节FF D8）
            if (read >= 2 &&
                    header[0] == (byte) 0xFF &&
                    header[1] == (byte) 0xD8) {
                return MediaType.IMAGE_JPEG_VALUE;
            }

            // PNG检测（前8字节）
            if (read >= 8 &&
                    header[0] == (byte) 0x89 &&
                    header[1] == 0x50 &&
                    header[2] == 0x4E &&
                    header[3] == 0x47 &&
                    header[4] == 0x0D &&
                    header[5] == 0x0A &&
                    header[6] == 0x1A &&
                    header[7] == 0x0A) {
                return MediaType.IMAGE_PNG_VALUE;
            }

            // GIF检测（前3字节GIF）
            if (read >= 3 &&
                    header[0] == 0x47 && // G
                    header[1] == 0x49 && // I
                    header[2] == 0x46) { // F
                return MediaType.IMAGE_GIF_VALUE;
            }

            // 添加更多图片类型检测（如WebP）
            if (read >= 12 &&
                    header[8] == 0x57 && // W
                    header[9] == 0x45 && // E
                    header[10] == 0x42 && // B
                    header[11] == 0x50) { // P
                return "image/webp";
            }

            return "unknown";
        }
    }
}