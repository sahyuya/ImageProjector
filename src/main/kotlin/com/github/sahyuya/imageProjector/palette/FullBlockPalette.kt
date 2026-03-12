package com.github.sahyuya.imageProjector.palette

import org.bukkit.Material
import java.awt.Color
import kotlin.math.*

/**
 * 確実に設置できるブロック（コンクリート、テラコッタ、羊毛）のみを安全にリスト化するパレット
 */
class FullBlockPalette : BlockPalette {
    private val basePaletteLab = mutableListOf<EntryWithLab>()

    private data class EntryWithLab(val entry: PaletteEntry, val lab: DoubleArray)

    override fun initialize() {
        // 色の名前と、それに対応する近似RGB値の固定リスト
        val colorData = listOf(
            Pair("WHITE", Color(249, 255, 254)),
            Pair("ORANGE", Color(249, 128, 29)),
            Pair("MAGENTA", Color(199, 78, 189)),
            Pair("LIGHT_BLUE", Color(58, 179, 218)),
            Pair("YELLOW", Color(254, 216, 61)),
            Pair("LIME", Color(128, 199, 31)),
            Pair("PINK", Color(243, 139, 170)),
            Pair("GRAY", Color(71, 79, 82)),
            Pair("LIGHT_GRAY", Color(157, 157, 153)),
            Pair("CYAN", Color(22, 156, 156)),
            Pair("PURPLE", Color(137, 50, 184)),
            Pair("BLUE", Color(60, 68, 170)),
            Pair("BROWN", Color(131, 84, 50)),
            Pair("GREEN", Color(94, 124, 22)),
            Pair("RED", Color(176, 46, 38)),
            Pair("BLACK", Color(29, 29, 33))
        )

        for ((colorName, rgb) in colorData) {
            val lab = rgbToLab(rgb)

            // コンクリートを安全に取得
            Material.getMaterial("${colorName}_CONCRETE")?.let { mat ->
                basePaletteLab.add(EntryWithLab(PaletteEntry(mat, rgb, DepthProfile.SOLID, 1.0), lab))
            }
            // テラコッタを安全に取得
            Material.getMaterial("${colorName}_TERRACOTTA")?.let { mat ->
                basePaletteLab.add(EntryWithLab(PaletteEntry(mat, rgb, DepthProfile.SOLID, 1.0), lab))
            }
            // 羊毛を安全に取得
            Material.getMaterial("${colorName}_WOOL")?.let { mat ->
                basePaletteLab.add(EntryWithLab(PaletteEntry(mat, rgb, DepthProfile.SOLID, 1.0), lab))
            }
        }
    }

    override fun getBestCombination(target: Color, shadingFactor: Double, maxLayers: Int): LayerCombination {
        if (basePaletteLab.isEmpty()) {
            return LayerCombination(PaletteEntry(Material.STONE, Color.GRAY, DepthProfile.SOLID, 1.0), emptyList(), Color.GRAY, 0.0)
        }

        val targetLab = rgbToLab(target)
        val bestBase = basePaletteLab.minByOrNull { cieDE2000(it.lab, targetLab) }!!

        return LayerCombination(bestBase.entry, emptyList(), bestBase.entry.color, 0.0)
    }

    // --- 色変換ユーティリティ ---
    private fun rgbToLab(rgb: Color): DoubleArray {
        var r = rgb.red / 255.0; var g = rgb.green / 255.0; var b = rgb.blue / 255.0
        r = if (r > 0.04045) ((r + 0.055) / 1.055).pow(2.4) else r / 12.92
        g = if (g > 0.04045) ((g + 0.055) / 1.055).pow(2.4) else g / 12.92
        b = if (b > 0.04045) ((b + 0.055) / 1.055).pow(2.4) else b / 12.92
        val x = (r * 0.4124 + g * 0.3576 + b * 0.1805) * 100.0
        val y = (r * 0.2126 + g * 0.7152 + b * 0.0722) * 100.0
        val z = (r * 0.0193 + g * 0.1192 + b * 0.9505) * 100.0
        var xn = x / 95.047; var yn = y / 100.000; var zn = z / 108.883
        xn = if (xn > 0.008856) xn.pow(1.0 / 3.0) else (7.787 * xn) + (16.0 / 116.0)
        yn = if (yn > 0.008856) yn.pow(1.0 / 3.0) else (7.787 * yn) + (16.0 / 116.0)
        zn = if (zn > 0.008856) zn.pow(1.0 / 3.0) else (7.787 * zn) + (16.0 / 116.0)
        return doubleArrayOf((116.0 * yn) - 16.0, 500.0 * (xn - yn), 200.0 * (yn - zn))
    }

    private fun cieDE2000(lab1: DoubleArray, lab2: DoubleArray): Double {
        return sqrt((lab1[0] - lab2[0]).pow(2) + (lab1[1] - lab2[1]).pow(2) + (lab1[2] - lab2[2]).pow(2))
    }
}