package com.csfrez.demospringboot.service;

import com.csfrez.demospringboot.tool.ImageRotator;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author
 * @date 2025/2/14 14:20
 * @email
 */
@Service
@Slf4j
public class PdfToImageService {

    // 默认DPI（分辨率）
    private static final int DEFAULT_DPI = 300;

    @Value("${file.storage.location}")
    private String storagePath;

    @Value("${pdf.image.auto-create-dir:true}")
    private boolean autoCreateDir;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 初始化时检查存储目录
    @PostConstruct
    public void init() throws IOException {
        if (autoCreateDir) {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        }
    }

    public List<byte[]> convertPdfToImages(MultipartFile pdfFile) throws IOException {
        return convertPdfToImages(pdfFile, DEFAULT_DPI);
    }

    public List<byte[]> convertPdfToImages(MultipartFile pdfFile, int dpi) throws IOException {
        List<byte[]> imageBytesList = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, dpi);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpeg", baos);
                imageBytesList.add(baos.toByteArray());
            }
        }

        return imageBytesList;
    }

    public List<String> savePdfImagesToLocal(MultipartFile pdfFile, int dpi, String format, int rotation, boolean flag) throws IOException {
        List<String> savedPaths = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile.getInputStream())) {
            PDFRenderer renderer = new PDFRenderer(document);
            String baseFileName = generateUniqueFileName(pdfFile.getOriginalFilename());
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                Path outputPath = generateUniqueFilePath(baseFileName, page, format);
                // 获取页面旋转角度
//                int pageRotation = document.getPage(page).getRotation();
                BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                int pageRotation = rotation;
                if (flag && page % 2 != 0) {
                    pageRotation = pageRotation - 180;
                }
                log.info("PDF页面{}旋转角度：{}", page, pageRotation);
                // 应用PDF页面自带旋转
                image = ImageRotator.rotate(image, pageRotation);

                String filePath = saveImageToDisk(image, format, outputPath);
                savedPaths.add(filePath);
            }
        }
        return savedPaths;
    }

    private String generateUniqueFileName(String originalName) {
        String timeStamp = LocalDateTime.now().format(DATE_FORMATTER);
        String baseName = originalName != null ? originalName.substring(0, originalName.lastIndexOf('.')) : "unknown";
        return baseName + "_" + timeStamp;
    }

    private Path generateUniqueFilePath(String basePath, int page, String format) {
        String fileName = String.format("%s_page%03d.%s", basePath, page + 1, format);
        return Paths.get(storagePath, fileName);
    }

    private String saveImageToDisk(BufferedImage image, String format, Path outputPath) throws IOException {
        if ("png".equals(format)) {
            ImageIO.write(image, format, outputPath.toFile());
        } else if ("jpg".equals(format) || "jpeg".equals(format)) {
            saveAsJpeg(image, outputPath, format);
        }
        return outputPath.toAbsolutePath().toString();
    }

    private void saveAsJpeg(BufferedImage image, Path path, String format) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        ImageWriter writer = writers.next();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.9f); // 0-1质量系数
            writer.write(null, new IIOImage(image, null, null), param);
        }
        writer.dispose();
    }
}