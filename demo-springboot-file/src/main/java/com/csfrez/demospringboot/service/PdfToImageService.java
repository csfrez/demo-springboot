package com.csfrez.demospringboot.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author
 * @date 2025/2/14 14:20
 * @email
 */
@Service
public class PdfToImageService {

    // 默认DPI（分辨率）
    private static final int DEFAULT_DPI = 300;

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
}