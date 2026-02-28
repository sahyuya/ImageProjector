package com.github.sahyuya.imageProjector.palette

import org.bukkit.Material
import java.awt.Color
import kotlin.math.pow
import kotlin.math.sqrt

// 可変長のガラス層を持てるように修正
data class LayerCombination(
    val base: Material,
    val glasses: List<Material>,
    val blendedColor: Color,
    val score: Double
)

class BlockPalette {
    private val basePalette = mutableMapOf<Material, Color>()
    private val glassPalette = mutableMapOf<Material, Color>()

    // 実験データに基づく指数減衰モデルの減衰率（透過率）
    private val rRate = 0.55

    fun initialize() {
        // --- コンクリート ---
        basePalette[Material.WHITE_CONCRETE] = Color(205, 210, 211)
        basePalette[Material.ORANGE_CONCRETE] = Color(222, 97, 2)
        basePalette[Material.MAGENTA_CONCRETE] = Color(169, 47, 157)
        basePalette[Material.LIGHT_BLUE_CONCRETE] = Color(35, 135, 197)
        basePalette[Material.YELLOW_CONCRETE] = Color(236, 172, 21)
        basePalette[Material.LIME_CONCRETE] = Color(93, 167, 24)
        basePalette[Material.PINK_CONCRETE] = Color(210, 100, 141)
        basePalette[Material.GRAY_CONCRETE] = Color(54, 57, 61)
        basePalette[Material.LIGHT_GRAY_CONCRETE] = Color(125, 125, 115)
        basePalette[Material.CYAN_CONCRETE] = Color(21, 119, 136)
        basePalette[Material.PURPLE_CONCRETE] = Color(100, 31, 156)
        basePalette[Material.BLUE_CONCRETE] = Color(44, 46, 143)
        basePalette[Material.BROWN_CONCRETE] = Color(96, 59, 31)
        basePalette[Material.GREEN_CONCRETE] = Color(73, 91, 36)
        basePalette[Material.RED_CONCRETE] = Color(142, 32, 32)
        basePalette[Material.BLACK_CONCRETE] = Color(8, 10, 15)

        // --- コンクリートパウダー ---
        // ConcurrentModificationExceptionを防ぐため、一時リストに溜めてから追加する
        val powderEntries = mutableListOf<Pair<Material, Color>>()
        basePalette.forEach { (mat, col) ->
            val powderMat = Material.getMaterial(mat.name + "_POWDER")
            if (powderMat != null) {
                val pr = (col.red + 15).coerceIn(0, 255)
                val pg = (col.green + 15).coerceIn(0, 255)
                val pb = (col.blue + 15).coerceIn(0, 255)
                powderEntries.add(Pair(powderMat, Color(pr, pg, pb)))
            }
        }
        powderEntries.forEach { (mat, col) ->
            basePalette[mat] = col
        }

        // --- 色付きガラス ---
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
     * ビームサーチを用いて、多層（指定された最大層まで）の最適構成を探索する
     */
    fun getBestCombination(target: Color, shadingFactor: Double, maxGlassLayers: Int = 2): LayerCombination {
        val beamWidth = 5
        val penaltyPerGlass = 15.0 // 無駄にガラスを重ねないためのペナルティ

        // 1. 基底層(Base)の探索
        var beam = mutableListOf<LayerCombination>()
        for ((baseMat, baseCol) in basePalette) {
            val shadedBase = applyShading(baseCol, shadingFactor)
            val dist = colorDistance(shadedBase, target)
            beam.add(LayerCombination(baseMat, emptyList(), shadedBase, dist))
        }
        beam = beam.sortedBy { it.score }.take(beamWidth).toMutableList()

        if (maxGlassLayers <= 0) return beam.first()

        // 2. ガラス層の探索 (指定された最大層数まで動的にループ)
        for (layer in 1..maxGlassLayers) {
            val nextBeam = mutableListOf<LayerCombination>()
            for (state in beam) {
                // そのまま（これ以上ガラスを追加しない）の選択肢を残す
                nextBeam.add(state)

                // すでに前のステップでガラス追加を止めている場合は追加処理をスキップ
                if (state.glasses.size < layer - 1) continue

                for ((glassMat, glassCol) in glassPalette) {
                    val blended = applyExponentialBlend(state.blendedColor, glassCol)
                    val newGlasses = state.glasses + glassMat
                    val penalty = penaltyPerGlass * newGlasses.size
                    val dist = colorDistance(blended, target) + penalty
                    nextBeam.add(LayerCombination(state.base, newGlasses, blended, dist))
                }
            }
            // 同じ構成の重複を除きつつスコア順で絞る
            beam = nextBeam.distinctBy { it.base.name + ":" + it.glasses.joinToString { g -> g.name } }
                .sortedBy { it.score }
                .take(beamWidth).toMutableList()
        }

        return beam.first()
    }

    private fun applyExponentialBlend(innerColor: Color, glassColor: Color): Color {
        val r = glassColor.red + (innerColor.red - glassColor.red) * rRate
        val g = glassColor.green + (innerColor.green - glassColor.green) * rRate
        val b = glassColor.blue + (innerColor.blue - glassColor.blue) * rRate
        return Color(r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))
    }

    private fun applyShading(color: Color, factor: Double): Color {
        val r = (color.red * factor).toInt().coerceIn(0, 255)
        val g = (color.green * factor).toInt().coerceIn(0, 255)
        val b = (color.blue * factor).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }

    private fun colorDistance(c1: Color, c2: Color): Double {
        val dr = (c1.red - c2.red).toDouble()
        val dg = (c1.green - c2.green).toDouble()
        val db = (c1.blue - c2.blue).toDouble()
        return sqrt(2.0 * dr.pow(2.0) + 4.0 * dg.pow(2.0) + 3.0 * db.pow(2.0))
    }
}