package com.csfrez.demospringboot.service;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author
 * @date 2025/2/14 17:50
 * @email
 */
@Service
public class ImageToPdfService {

    // 压缩配置
    private static final float COMPRESSION_QUALITY = 0.8f;
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;

    public byte[] convertImagesToPdf(MultipartFile[] files) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (MultipartFile file : files) {
                // 压缩图片
                BufferedImage compressedImage = compressImage(file.getInputStream());

                // 创建PDF页面
                PDPage page = new PDPage(new PDRectangle(compressedImage.getWidth(), compressedImage.getHeight()));
                document.addPage(page);

                // 将图片写入PDF
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    PDImageXObject pdImage = LosslessFactory.createFromImage(document, compressedImage);
                    contentStream.drawImage(pdImage, 0, 0);
                }
            }

            // 生成最终PDF字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private BufferedImage compressImage(InputStream inputStream) throws IOException {
        // 使用Thumbnailator进行尺寸压缩和质量压缩
        BufferedImage compressedImage = Thumbnails.of(inputStream)
                .size(MAX_WIDTH, MAX_HEIGHT)
                .outputQuality(COMPRESSION_QUALITY)
                .asBufferedImage();

        // 转换图片格式为JPEG（进一步减小体积）
        BufferedImage jpegImage = new BufferedImage(
                compressedImage.getWidth(),
                compressedImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        jpegImage.createGraphics().drawImage(compressedImage, 0, 0, Color.WHITE, null);

        return jpegImage;
    }

    public byte[] convertImagesToPdf2(MultipartFile[] files) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (MultipartFile file : files) {
                try (InputStream is = file.getInputStream()) {
                    // 获取原始图片
                    BufferedImage originalImage = ImageIO.read(is);
                    // 创建与图片等大的PDF页面
                    PDPage page = new PDPage(new PDRectangle(originalImage.getWidth(), originalImage.getHeight()));
                    document.addPage(page);

                    // 写入PDF
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                        PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                                document,
                                file.getBytes(),
                                file.getOriginalFilename()
                        );

                        // 在createFromByteArray后添加透明度处理
                        if (pdImage instanceof PDImageXObject) {
                            PDImageXObject pdImageX = (PDImageXObject) pdImage;
                            if (pdImageX.getColorSpace().equals(PDDeviceRGB.INSTANCE)) {
                                // 添加白色背景处理透明区域
                                contentStream.setNonStrokingColor(Color.WHITE);
                                contentStream.addRect(0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
                                contentStream.fill();
                            }
                        }
                        contentStream.drawImage(pdImage, 0, 0);
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
