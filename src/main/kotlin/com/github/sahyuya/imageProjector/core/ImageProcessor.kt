package com.github.sahyuya.imageProjector.core

import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

/**
 * 画像のダウンロードやリサイズ等の処理を専門に行います。
 */
class ImageProcessor {
    /**
     * 画像を取得し、マイクラ内で現実的なサイズ（デフォルト幅150ピクセル）にリサイズします。
     * これにより計算量が劇的に減少し、フリーズや計算の長時間化を防ぎます。
     */
    fun fetchImage(urlStr: String, maxWidth: Int = 150): BufferedImage {
        val url = URL(urlStr)
        val original = ImageIO.read(url)
            ?: throw IllegalArgumentException("画像のデコードに失敗しました。URLが直接画像を指しているか確認してください。")

        // 既に小さい画像ならそのまま返す
        if (original.width <= maxWidth) return original

        // アスペクト比を維持して新しい高さを計算
        val ratio = maxWidth.toDouble() / original.width
        val newHeight = (original.height * ratio).toInt()

        val resized = BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
        val g = resized.createGraphics()
        // 縮小時に画質が荒れないよう、バイリニア補間を有効化
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(original, 0, 0, maxWidth, newHeight, null)
        g.dispose()

        return resized
    }
}