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
import java.util.List;
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
}