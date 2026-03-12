package com.github.sahyuya.imageProjector.core

import com.github.sahyuya.imageProjector.palette.BlockPalette
import com.github.sahyuya.imageProjector.palette.PaletteEntry
import org.bukkit.Location
import org.bukkit.util.Vector
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.sin

data class PlacementData(
    val location: Location,
    val rayDir: Vector,
    val entry: PaletteEntry
)

class ProjectionCalculator(
    private val maxLayers: Int = 4,
    private val layerSpacing: Double = 1.0 // 各層（コンクリートやガラス）の間隔（1ブロック）
) {
    fun calculatePlacements(
        palette: BlockPalette,
        image: BufferedImage,
        origin: Location,
        yaw: Float,
        pitch: Float,
        widthScale: Double,
        heightScale: Double,
        baseDistance: Double,
        freeMode: Boolean
    ): List<PlacementData> {
        val placements = mutableListOf<PlacementData>()

        val centerDir = getDirectionVector(yaw, pitch)
        val snappedDir = if (freeMode) centerDir else getSnappedVector(centerDir)

        // スクリーンの軸（右方向と上方向）を計算
        var rightDir = snappedDir.clone().crossProduct(Vector(0, 1, 0))
        if (rightDir.lengthSquared() == 0.0) rightDir = Vector(1.0, 0.0, 0.0)
        rightDir = rightDir.normalize()
        val upDir = rightDir.clone().crossProduct(snappedDir).normalize()

        val imgWidth = image.width
        val imgHeight = image.height

        for (y in 0 until imgHeight) {
            for (x in 0 until imgWidth) {
                val rgb = image.getRGB(x, y)
                if ((rgb shr 24 and 0xFF) < 128) continue // 透過ピクセルは無視

                val targetColor = Color(rgb, true)
                val nx = (x.toDouble() / imgWidth) - 0.5
                val ny = 0.5 - (y.toDouble() / imgHeight)

                // 【修正】1ピクセル＝1ブロックのスケールで、キャンバス上の正しい実座標を求める
                val canvasPoint = origin.clone()
                    .add(snappedDir.clone().multiply(baseDistance))
                    .add(rightDir.clone().multiply(nx * widthScale))
                    .add(upDir.clone().multiply(ny * heightScale))

                // 視点（origin）からキャンバス上のピクセルに向かう「レイ（視線）」を計算
                val rayVector = canvasPoint.toVector().subtract(origin.toVector())
                val rayDistance = rayVector.length()
                val rayDir = rayVector.normalize()

                // 過去の設計の核である「色を合わせるためのブロックの組み合わせ（層）」を取得
                val combination = palette.getBestCombination(targetColor, 1.0, maxLayers)

                // 【復活】層の重ね合わせロジック
                // 1. 基底層（コンクリートなどの不透明ブロック）をキャンバス位置（一番奥）に配置
                val baseLoc = origin.clone().add(rayDir.clone().multiply(rayDistance + combination.base.profile.depthOffset))
                placements.add(PlacementData(baseLoc, rayDir, combination.base))

                // 2. フィルター層（ガラスなど）を、レイに沿って視点側（手前）に向かって順に配置
                combination.filters.forEachIndexed { index, filterEntry ->
                    // 基底層から、設定した間隔（layerSpacing）分だけ手前へずらす
                    val filterDist = rayDistance - ((index + 1) * layerSpacing) + filterEntry.profile.depthOffset

                    // 視点より後ろ（背後）に行ってしまうのを防ぐ安全策
                    if (filterDist > 1.0) {
                        val filterLoc = origin.clone().add(rayDir.clone().multiply(filterDist))
                        placements.add(PlacementData(filterLoc, rayDir, filterEntry))
                    }
                }
            }
        }
        return placements
    }

    private fun getDirectionVector(yaw: Float, pitch: Float): Vector {
        val rotX = yaw * (Math.PI / 180)
        val rotY = pitch * (Math.PI / 180)
        return Vector(-sin(rotX) * cos(rotY), -sin(rotY), cos(rotX) * cos(rotY))
    }

    private fun getSnappedVector(vector: Vector): Vector {
        val x = vector.x; val y = vector.y; val z = vector.z
        val ax = Math.abs(x); val ay = Math.abs(y); val az = Math.abs(z)
        return when {
            ax >= ay && ax >= az -> Vector(if (x > 0) 1.0 else -1.0, 0.0, 0.0)
            ay >= ax && ay >= az -> Vector(0.0, if (y > 0) 1.0 else -1.0, 0.0)
            else -> Vector(0.0, 0.0, if (z > 0) 1.0 else -1.0)
        }
    }
}