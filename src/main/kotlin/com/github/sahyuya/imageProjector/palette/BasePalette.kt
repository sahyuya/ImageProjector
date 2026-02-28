package com.github.sahyuya.imageProjector.palette

import org.bukkit.Material
import java.awt.Color
import kotlin.math.pow

class BlockPalette {
    private val basePalette = mutableMapOf<Material, Color>()
    private val glassPalette = mutableMapOf<Material, Color>()

    // ブレンドの際のガラスの影響度（0.0〜1.0）
    // ガラスが基底の色をどれくらい変化させるかの近似値。実験で最適値を探ります。
    private val glassAlpha = 0.6

    fun initialize() {
        // 基底ブロック（コンクリート）
        basePalette[Material.WHITE_CONCRETE] = Color(205, 210, 211)
        basePalette[Material.ORANGE_CONCRETE] = Color(222, 97, 2)
        basePalette[Material.MAGENTA_CONCRETE] = Color(169, 47, 157)
        basePalette[Material.LIGHT_BLUE_CONCRETE] = Color(35, 135, 197)
        basePalette[Material.YELLOW_CONCRETE] = Color(236, 172, 21)
        basePalette[Material.LIME_CONCRETE] = Color(93, 167, 24)
        basePalette[Material.PINK_CONCRETE] = Color(210, 100, 141)
        basePalette[Material.GRAY_CONCRETE] = Color(54, 57, 61)
        basePalette[Material.LIGHT_GRAY_CONCRETE] = Color(124, 124, 114)
        basePalette[Material.CYAN_CONCRETE] = Color(21, 118, 134)
        basePalette[Material.PURPLE_CONCRETE] = Color(100, 32, 155)
        basePalette[Material.BLUE_CONCRETE] = Color(44, 46, 142)
        basePalette[Material.BROWN_CONCRETE] = Color(95, 58, 31)
        basePalette[Material.GREEN_CONCRETE] = Color(72, 90, 36)
        basePalette[Material.RED_CONCRETE] = Color(140, 32, 32)
        basePalette[Material.BLACK_CONCRETE] = Color(9, 11, 16)

        // （コンクリートパウダー）
        basePalette[Material.WHITE_CONCRETE_POWDER] = Color(222, 223, 224)
        basePalette[Material.ORANGE_CONCRETE_POWDER] = Color(231, 134, 34)
        basePalette[Material.MAGENTA_CONCRETE_POWDER] = Color(200, 93, 193)
        basePalette[Material.LIGHT_BLUE_CONCRETE_POWDER] = Color(69, 174, 208)
        basePalette[Material.YELLOW_CONCRETE_POWDER] = Color(234, 210, 75)
        basePalette[Material.LIME_CONCRETE_POWDER] = Color(125, 189, 40)
        basePalette[Material.PINK_CONCRETE_POWDER] = Color(236, 163, 191)
        basePalette[Material.GRAY_CONCRETE_POWDER] = Color(89, 97, 100)
        basePalette[Material.LIGHT_GRAY_CONCRETE_POWDER] = Color(167, 167, 163)
        basePalette[Material.CYAN_CONCRETE_POWDER] = Color(37, 147, 155)
        basePalette[Material.PURPLE_CONCRETE_POWDER] = Color(138, 58, 185)
        basePalette[Material.BLUE_CONCRETE_POWDER] = Color(73, 76, 168)
        basePalette[Material.BROWN_CONCRETE_POWDER] = Color(123, 81, 51)
        basePalette[Material.GREEN_CONCRETE_POWDER] = Color(102, 123, 53)
        basePalette[Material.RED_CONCRETE_POWDER] = Color(159, 49, 47)
        basePalette[Material.BLACK_CONCRETE_POWDER] = Color(28, 30, 34)

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
     * shadingFactor: プレイヤーから見た際の色調暗化係数 (0.5 ~ 1.0)
     */
    fun getBestCombination(target: Color, shadingFactor: Double): Pair<Material, Material?> {
        var bestBase = Material.WHITE_CONCRETE
        var bestGlass: Material? = null
        var minDistance = Double.MAX_VALUE

        // 1. まず基底ブロック単体で距離を計算
        for ((baseMat, baseCol) in basePalette) {
            val shadedBase = applyShading(baseCol, shadingFactor)
            val dist = colorDistanceHSV(shadedBase, target)
            if (dist < minDistance) {
                minDistance = dist
                bestBase = baseMat
                bestGlass = null
            }

            // 2. 基底ブロック ＋ ガラスブロック1枚の組み合わせで距離を計算
            for ((glassMat, glassCol) in glassPalette) {
                // 乗算的な単調減少を模したブレンド計算
                val blended = blendColor(baseCol, glassCol, glassAlpha)
                val shadedBlended = applyShading(blended, shadingFactor)
                val blendedDist = colorDistanceHSV(shadedBlended, target)
                if (blendedDist < minDistance) {
                    minDistance = blendedDist
                    bestBase = baseMat
                    bestGlass = glassMat
                }
            }
        }
        return Pair(bestBase, bestGlass)
    }

    private fun applyShading(color: Color, factor: Double): Color {
        val r = (color.red * factor).toInt().coerceIn(0, 255)
        val g = (color.green * factor).toInt().coerceIn(0, 255)
        val b = (color.blue * factor).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }

    /**
     * 水彩画のような滑らかな色のつながりを生むため、RGBではなくHSV色空間で比較する
     */
    private fun colorDistanceHSV(c1: Color, c2: Color): Double {
        val hsv1 = FloatArray(3)
        val hsv2 = FloatArray(3)
        Color.RGBtoHSB(c1.red, c1.green, c1.blue, hsv1)
        Color.RGBtoHSB(c2.red, c2.green, c2.blue, hsv2)

        // 色相(Hue)は円環(0.0と1.0が同じ赤)なので、最短距離を計算
        var dh = Math.abs(hsv1[0] - hsv2[0]).toDouble()
        if (dh > 0.5) dh = 1.0 - dh

        val ds = Math.abs(hsv1[1] - hsv2[1]).toDouble()
        val dv = Math.abs(hsv1[2] - hsv2[2]).toDouble()

        // 重み付け: 色相(H)のズレを一番嫌う(ノイズを減らす)
        // 彩度(S)と明度(V)はある程度の誤差を許容し、ガラス特有のくすみをアートとして受け入れる
        val weightH = 3.0
        val weightS = 1.0
        val weightV = 1.5

        return (dh * weightH).pow(2.0) + (ds * weightS).pow(2.0) + (dv * weightV).pow(2.0)
    }

    private fun blendColor(base: Color, glass: Color, alpha: Double): Color {
        val r = (glass.red * alpha + base.red * (1.0 - alpha)).toInt().coerceIn(0, 255)
        val g = (glass.green * alpha + base.green * (1.0 - alpha)).toInt().coerceIn(0, 255)
        val b = (glass.blue * alpha + base.blue * (1.0 - alpha)).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }
}