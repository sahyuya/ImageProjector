package com.github.sahyuya.imageProjector.palette

import org.bukkit.Material
import java.awt.Color

/**
 * ブロックの形状や透過性に基づく「前後感覚」のメタデータ。
 */
enum class DepthProfile(val isFilter: Boolean, val defaultBlendRate: Double, val depthOffset: Double) {
    SOLID(false, 1.0, 0.0),             // 通常のフルブロック
    TRANSPARENT_FULL(true, 0.55, 0.0),  // ガラスや氷など
    TRANSPARENT_THIN(true, 0.40, 0.0),  // ガラス板など
    THIN_FRONT(true, 0.85, -0.4),       // カーペット等（手前オフセット）
    THIN_BACK(false, 1.0, 0.4),         // 上付きスラブ等（奥オフセット）
    WALL_MOUNT(true, 0.75, -0.3)        // トラップドア等（壁面寄り）
}

data class PaletteEntry(
    val material: Material,
    val color: Color,
    val profile: DepthProfile,
    val blendRate: Double
)

data class LayerCombination(
    val base: PaletteEntry,
    val filters: List<PaletteEntry>,
    val blendedColor: Color,
    val score: Double
)

/**
 * ブロックパレットの共通インターフェース
 */
interface BlockPalette {
    fun initialize()
    fun getBestCombination(target: Color, shadingFactor: Double, maxLayers: Int = 2): LayerCombination
}