package com.csfrez.demospringboot.tool;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * @author
 * @date 2025/3/11 10:07
 * @email
 */
public class ImageRotator {

    /**
     * 旋转图片（支持90度倍数旋转）
     *
     * @param originalImage 原始图片
     * @param degrees       旋转角度（仅支持 90, 180, 270）
     * @return 旋转后的新图片
     */
    public static BufferedImage rotate(BufferedImage originalImage, int degrees) {
        // 验证旋转角度有效性
        if (degrees % 90 != 0) {
            throw new IllegalArgumentException("只支持90度的整数倍旋转");
        }

        // 计算新图片尺寸
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int newWidth = degrees % 180 == 0 ? width : height;
        int newHeight = degrees % 180 == 0 ? height : width;

        // 创建新画布
        BufferedImage rotatedImage = new BufferedImage(
                newWidth, newHeight, originalImage.getType());

        Graphics2D g2d = rotatedImage.createGraphics();
        AffineTransform transform = new AffineTransform();

        // 计算旋转中心点并执行旋转
        transform.translate(
                (newWidth - width) / 2.0,
                (newHeight - height) / 2.0
        );
        transform.rotate(
                Math.toRadians(degrees),
                width / 2.0,
                height / 2.0
        );

        g2d.drawRenderedImage(originalImage, transform);
        g2d.dispose();

        return rotatedImage;
    }
}
