package com.github.sahyuya.imageProjector.palette

import org.bukkit.Material
import java.awt.Color
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 以前のコンクリートとガラスのみを使用し、派手な逆算ブーストを行うパレット（-c オプション用）
 */
class ConcreteGlassPalette : BlockPalette {
    private val basePalette = mutableMapOf<Material, PaletteEntry>()
    private val glassPalette = mutableMapOf<Material, PaletteEntry>()
    private val rRate = 0.55

    override fun initialize() {
        // --- コンクリート ---
        addBase(Material.WHITE_CONCRETE, Color(205, 210, 211))
        addBase(Material.ORANGE_CONCRETE, Color(222, 97, 2))
        addBase(Material.MAGENTA_CONCRETE, Color(169, 47, 157))
        addBase(Material.LIGHT_BLUE_CONCRETE, Color(35, 135, 197))
        addBase(Material.YELLOW_CONCRETE, Color(236, 172, 21))
        addBase(Material.LIME_CONCRETE, Color(93, 167, 24))
        addBase(Material.PINK_CONCRETE, Color(210, 100, 141))
        addBase(Material.BLACK_CONCRETE, Color(8, 10, 15))

        // --- 色付きガラス ---
        addGlass(Material.WHITE_STAINED_GLASS, Color(255, 255, 255))
        addGlass(Material.ORANGE_STAINED_GLASS, Color(216, 127, 51))
        addGlass(Material.MAGENTA_STAINED_GLASS, Color(178, 76, 216))
        addGlass(Material.LIGHT_BLUE_STAINED_GLASS, Color(102, 153, 216))
        addGlass(Material.YELLOW_STAINED_GLASS, Color(229, 229, 51))
        addGlass(Material.BLACK_STAINED_GLASS, Color(25, 25, 25))
    }

    private fun addBase(mat: Material, col: Color) {
        basePalette[mat] = PaletteEntry(mat, col, DepthProfile.SOLID, 1.0)
        val powderMat = Material.getMaterial(mat.name + "_POWDER")
        if (powderMat != null) {
            val pr = (col.red + 15).coerceIn(0, 255)
            val pg = (col.green + 15).coerceIn(0, 255)
            val pb = (col.blue + 15).coerceIn(0, 255)
            basePalette[powderMat] = PaletteEntry(powderMat, Color(pr, pg, pb), DepthProfile.SOLID, 1.0)
        }
    }

    private fun addGlass(mat: Material, col: Color) {
        glassPalette[mat] = PaletteEntry(mat, col, DepthProfile.TRANSPARENT_FULL, rRate)
    }

    override fun getBestCombination(target: Color, shadingFactor: Double, maxLayers: Int): LayerCombination {
        val beamWidth = 5
        val layerBoost = (maxLayers - 2).coerceAtLeast(0).toDouble()
        val penaltyPerGlass = 15.0 / 2.0.pow(layerBoost)
        val baseTarget = if (maxLayers > 2) boostColorForBase(target, maxLayers) else target

        var beam = basePalette.values.map { entry ->
            val dist = colorDistance(entry.color, baseTarget)
            LayerCombination(entry, emptyList(), entry.color, dist)
        }.sortedBy { it.score }.take(beamWidth).toMutableList()

        beam = beam.map { it.copy(score = colorDistance(it.blendedColor, target)) }.toMutableList()

        if (maxLayers <= 0) return beam.first()

        for (layer in 1..maxLayers) {
            val nextBeam = mutableListOf<LayerCombination>()
            for (state in beam) {
                val currentDist = colorDistance(state.blendedColor, target) + (penaltyPerGlass * state.filters.size)
                nextBeam.add(state.copy(score = currentDist))

                if (state.filters.size < layer - 1) continue

                for (glassEntry in glassPalette.values) {
                    val blended = applyExponentialBlend(state.blendedColor, glassEntry.color)
                    val newFilters = state.filters + glassEntry
                    val penalty = penaltyPerGlass * newFilters.size
                    val dist = colorDistance(blended, target) + penalty
                    nextBeam.add(LayerCombination(state.base, newFilters, blended, dist))
                }
            }
            beam = nextBeam.distinctBy { it.base.material.name + ":" + it.filters.joinToString { g -> g.material.name } }
                .sortedBy { it.score }
                .take(beamWidth).toMutableList()
        }
        return beam.first()
    }

    private fun boostColorForBase(original: Color, maxLayers: Int): Color {
        val hsb = FloatArray(3)
        Color.RGBtoHSB(original.red, original.green, original.blue, hsb)
        val boostFactor = (maxLayers - 2) * 0.15f
        var s = hsb[1]
        var v = hsb[2]
        if (s > 0.05f) s += boostFactor * (0.5f + s * 0.5f)
        v += boostFactor * 0.8f
        val rgb = Color.HSBtoRGB(hsb[0], s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
        return Color(rgb)
    }

    private fun applyExponentialBlend(inner: Color, glass: Color): Color {
        return Color(
            (glass.red + (inner.red - glass.red) * rRate).toInt().coerceIn(0, 255),
            (glass.green + (inner.green - glass.green) * rRate).toInt().coerceIn(0, 255),
            (glass.blue + (inner.blue - glass.blue) * rRate).toInt().coerceIn(0, 255)
        )
    }

    private fun colorDistance(c1: Color, c2: Color): Double {
        return sqrt(2.0 * (c1.red - c2.red).toDouble().pow(2) + 4.0 * (c1.green - c2.green).toDouble().pow(2) + 3.0 * (c1.blue - c2.blue).toDouble().pow(2))
    }
}