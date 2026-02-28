package com.github.sahyuya.imageProjector

import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.net.URL
import javax.imageio.ImageIO

object ImageProcessor {

    fun fetchAndProcessImage(urlString: String, targetWidth: Int, targetHeight: Int): BufferedImage {
        val url = URL(urlString)
        val image = ImageIO.read(url) ?: throw IllegalArgumentException("画像の読み込みに失敗しました")

        // 1. 画像を短辺に合わせて16:9にクロップ
        val cropped = cropTo16by9(image)

        // 2. 最適なブロック解像度に合わせてスムーズリサイズ
        val resized = resizeSmooth(cropped, targetWidth, targetHeight)

        // 3. ノイズを抑える水彩画風の平滑化フィルタを適用
        return applyWatercolorEffect(resized)
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
            cropWidth = (originalHeight * targetRatio).toInt()
            x = (originalWidth - cropWidth) / 2
        } else if (currentRatio < targetRatio) {
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

    /**
     * 3x3の平滑化フィルタを用いて、ピクセル単位の鋭い色の落差（ノイズ）をぼかし、
     * マイクラのガラスと相性の良い水彩画のような滑らかさを生み出す。
     */
    private fun applyWatercolorEffect(image: BufferedImage): BufferedImage {
        val weight = 1.0f / 9.0f
        val matrix = floatArrayOf(
            weight, weight, weight,
            weight, weight, weight,
            weight, weight, weight
        )
        val kernel = Kernel(3, 3, matrix)
        val op = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)

        val outputImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        op.filter(image, outputImage)
        return outputImage
    }
}