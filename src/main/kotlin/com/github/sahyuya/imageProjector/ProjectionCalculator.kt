package com.github.sahyuya.imageProjector

import com.github.sahyuya.imageProjector.palette.BlockPalette
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.tan

class ProjectionCalculator(private val palette: BlockPalette) {

    data class BlockPlacement(val location: Location, val material: Material)

    fun calculate(
        player: Player,
        image: BufferedImage,
        distance: Double,
        fovDegrees: Double,
        verticalOffset: Double = 0.0 // 上が切れる問題対策のオフセット
    ): List<BlockPlacement> {
        val placements = mutableListOf<BlockPlacement>()

        val eye = player.eyeLocation
        val origin = eye.toVector()
        val forward = eye.direction.normalize()

        val right = if (abs(forward.y) > 0.999) {
            Vector(1, 0, 0)
        } else {
            forward.clone().crossProduct(Vector(0, 1, 0)).normalize()
        }
        val up = right.clone().crossProduct(forward).normalize()

        val imgWidth = image.width
        val imgHeight = image.height

        val fovRadians = Math.toRadians(fovDegrees)
        val screenHeightPhysical = 2.0 * distance * tan(fovRadians / 2.0)
        val screenWidthPhysical = screenHeightPhysical * (16.0 / 9.0)

        // 重複配置を避けるためのSet
        val placedBlocks = mutableSetOf<String>()

        // ガラスを配置するレイヤーの距離（基底より1ブロック手前）
        val glassDistance = distance - 1.0

        for (y in 0 until imgHeight) {
            for (x in 0 until imgWidth) {
                val targetColor = Color(image.getRGB(x, y))

                val px = ((x.toDouble() - imgWidth / 2.0 + 0.5) / imgWidth) * screenWidthPhysical
                // verticalOffset を足して画像全体を上下にずらせるようにする
                val py = -((y.toDouble() - imgHeight / 2.0 + 0.5) / imgHeight) * screenHeightPhysical + verticalOffset

                val pixelPos = origin.clone()
                    .add(forward.clone().multiply(distance))
                    .add(right.clone().multiply(px))
                    .add(up.clone().multiply(py))

                val ray = pixelPos.clone().subtract(origin).normalize()

                // 目標色に一番近い基底とガラスの組み合わせを取得
                val (baseMaterial, glassMaterial) = palette.getBestCombination(targetColor)

                // 1. 基底ブロックの計算と配置
                val tBase = distance / ray.dot(forward)
                val basePoint = origin.clone().add(ray.clone().multiply(tBase))

                val baseBlockX = basePoint.blockX
                val baseBlockY = basePoint.blockY
                val baseBlockZ = basePoint.blockZ
                val baseKey = "$baseBlockX,$baseBlockY,$baseBlockZ"

                if (placedBlocks.add(baseKey)) {
                    val baseLoc = Location(eye.world, baseBlockX.toDouble(), baseBlockY.toDouble(), baseBlockZ.toDouble())
                    placements.add(BlockPlacement(baseLoc, baseMaterial))
                }

                // 2. ガラスブロックの計算と配置（存在する場合）
                if (glassMaterial != null && glassDistance > 0) {
                    val tGlass = glassDistance / ray.dot(forward)
                    val glassPoint = origin.clone().add(ray.clone().multiply(tGlass))

                    val glassBlockX = glassPoint.blockX
                    val glassBlockY = glassPoint.blockY
                    val glassBlockZ = glassPoint.blockZ
                    val glassKey = "$glassBlockX,$glassBlockY,$glassBlockZ"

                    if (placedBlocks.add(glassKey)) {
                        val glassLoc = Location(eye.world, glassBlockX.toDouble(), glassBlockY.toDouble(), glassBlockZ.toDouble())
                        placements.add(BlockPlacement(glassLoc, glassMaterial))
                    }
                }
            }
        }
        return placements
    }
}