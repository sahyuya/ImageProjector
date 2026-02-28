package com.github.sahyuya.imageProjector

import java.awt.Image
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

object ImageProcessor {

    fun fetchAndProcessImage(urlString: String, targetWidth: Int, targetHeight: Int): BufferedImage {
        val url = URL(urlString)
        val image = ImageIO.read(url) ?: throw IllegalArgumentException("画像の読み込みに失敗しました")

        // 1. 画像を短辺に合わせて16:9にクロップ
        val cropped = cropTo16by9(image)

        // 2. 最適なブロック解像度に合わせてスムーズリサイズ
        return resizeSmooth(cropped, targetWidth, targetHeight)
    }

    private fun cropTo16by9(image: BufferedImage): BufferedImage {
        val originalWidth = image.width
        val originalHeight = image.height
        val targetRatio = 16.0 / 9.0
        val currentRatio = originalWidth.toDouble() / originalHeight.toDouble()

        var cropWidth = originalWidth
        var cropHeight = originalHeight
        var x = 0
        var y = 0

        if (currentRatio > targetRatio) {
            // 横長すぎるので左右を削る
            cropWidth = (originalHeight * targetRatio).toInt()
            x = (originalWidth - cropWidth) / 2
        } else if (currentRatio < targetRatio) {
            // 縦長すぎるので上下を削る
            cropHeight = (originalWidth / targetRatio).toInt()
            y = (originalHeight - cropHeight) / 2
        }

        return image.getSubimage(x, y, cropWidth, cropHeight)
    }

    private fun resizeSmooth(original: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
        val resultingImage = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH)
        val outputImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val g2d = outputImage.createGraphics()
        g2d.drawImage(resultingImage, 0, 0, null)
        g2d.dispose()
        return outputImage
    }
}