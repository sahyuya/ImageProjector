package com.github.sahyuya.imageProjector.palette

import org.bukkit.Material
import java.awt.Color
import kotlin.math.pow

class BlockPalette {
    private val basePalette = mutableMapOf<Material, Color>()
    private val glassPalette = mutableMapOf<Material, Color>()

    // ブレンドの際のガラスの影響度（0.0〜1.0）
    // ガラスが基底の色をどれくらい変化させるかの近似値。実験で最適値を探ります。
    private val glassAlpha = 0.5

    fun initialize() {
        // 基底ブロック（コンクリート）
        basePalette[Material.WHITE_CONCRETE] = Color(207, 213, 214)
        basePalette[Material.ORANGE_CONCRETE] = Color(224, 97, 0)
        basePalette[Material.MAGENTA_CONCRETE] = Color(169, 48, 159)
        basePalette[Material.LIGHT_BLUE_CONCRETE] = Color(36, 137, 199)
        basePalette[Material.YELLOW_CONCRETE] = Color(240, 175, 21)
        basePalette[Material.LIME_CONCRETE] = Color(94, 169, 50)
        basePalette[Material.PINK_CONCRETE] = Color(213, 101, 142)
        basePalette[Material.GRAY_CONCRETE] = Color(54, 57, 61)
        basePalette[Material.LIGHT_GRAY_CONCRETE] = Color(125, 125, 115)
        basePalette[Material.CYAN_CONCRETE] = Color(21, 119, 136)
        basePalette[Material.PURPLE_CONCRETE] = Color(100, 31, 156)
        basePalette[Material.BLUE_CONCRETE] = Color(44, 46, 143)
        basePalette[Material.BROWN_CONCRETE] = Color(96, 59, 31)
        basePalette[Material.GREEN_CONCRETE] = Color(73, 91, 36)
        basePalette[Material.RED_CONCRETE] = Color(142, 32, 32)
        basePalette[Material.BLACK_CONCRETE] = Color(8, 10, 15)

        // 色付きガラスブロック
        glassPalette[Material.WHITE_STAINED_GLASS] = Color(255, 255, 255)
        glassPalette[Material.ORANGE_STAINED_GLASS] = Color(216, 127, 51)
        glassPalette[Material.MAGENTA_STAINED_GLASS] = Color(178, 76, 216)
        glassPalette[Material.LIGHT_BLUE_STAINED_GLASS] = Color(102, 153, 216)
        glassPalette[Material.YELLOW_STAINED_GLASS] = Color(229, 229, 51)
        glassPalette[Material.LIME_STAINED_GLASS] = Color(127, 204, 25)
        glassPalette[Material.PINK_STAINED_GLASS] = Color(242, 127, 165)
        glassPalette[Material.GRAY_STAINED_GLASS] = Color(76, 76, 76)
        glassPalette[Material.LIGHT_GRAY_STAINED_GLASS] = Color(153, 153, 153)
        glassPalette[Material.CYAN_STAINED_GLASS] = Color(76, 127, 153)
        glassPalette[Material.PURPLE_STAINED_GLASS] = Color(127, 63, 178)
        glassPalette[Material.BLUE_STAINED_GLASS] = Color(51, 76, 178)
        glassPalette[Material.BROWN_STAINED_GLASS] = Color(102, 76, 51)
        glassPalette[Material.GREEN_STAINED_GLASS] = Color(102, 127, 51)
        glassPalette[Material.RED_STAINED_GLASS] = Color(153, 51, 51)
        glassPalette[Material.BLACK_STAINED_GLASS] = Color(25, 25, 25)
    }

    /**
     * 目標色に最も近い「基底ブロック」と「ガラスブロック（任意）」の組み合わせを返す
     */
    fun getBestCombination(target: Color): Pair<Material, Material?> {
        var bestBase = Material.WHITE_CONCRETE
        var bestGlass: Material? = null
        var minDistance = Double.MAX_VALUE

        // 1. まず基底ブロック単体で距離を計算
        for ((baseMat, baseCol) in basePalette) {
            val dist = colorDistance(baseCol, target)
            if (dist < minDistance) {
                minDistance = dist
                bestBase = baseMat
                bestGlass = null
            }

            // 2. 基底ブロック ＋ ガラスブロック1枚の組み合わせで距離を計算
            for ((glassMat, glassCol) in glassPalette) {
                val blended = blendColor(baseCol, glassCol, glassAlpha)
                val blendedDist = colorDistance(blended, target)
                if (blendedDist < minDistance) {
                    minDistance = blendedDist
                    bestBase = baseMat
                    bestGlass = glassMat
                }
            }
        }
        return Pair(bestBase, bestGlass)
    }

    private fun colorDistance(c1: Color, c2: Color): Double {
        return (c1.red - c2.red).toDouble().pow(2.0) +
                (c1.green - c2.green).toDouble().pow(2.0) +
                (c1.blue - c2.blue).toDouble().pow(2.0)
    }

    private fun blendColor(base: Color, glass: Color, alpha: Double): Color {
        val r = (glass.red * alpha + base.red * (1.0 - alpha)).toInt().coerceIn(0, 255)
        val g = (glass.green * alpha + base.green * (1.0 - alpha)).toInt().coerceIn(0, 255)
        val b = (glass.blue * alpha + base.blue * (1.0 - alpha)).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }
}