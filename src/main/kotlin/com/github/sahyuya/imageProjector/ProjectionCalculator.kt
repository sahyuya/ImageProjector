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

enum class ProjectionMode {
    FIXED, FREE
}

class ProjectionCalculator(private val palette: BlockPalette) {

    data class BlockPlacement(val location: Location, val material: Material)

    fun calculate(
        player: Player,
        image: BufferedImage,
        distance: Double,
        fovDegrees: Double,
        verticalOffset: Double = 0.0,
        mode: ProjectionMode = ProjectionMode.FIXED
    ): List<BlockPlacement> {
        val placements = mutableListOf<BlockPlacement>()

        val eye = player.eyeLocation
        val origin = eye.toVector()
        val dir = eye.direction.normalize()

        var forward = dir.clone()
        var fixedShading = 1.0

        // FIXEDモードの場合は視線を最も近い軸にスナップさせる
        if (mode == ProjectionMode.FIXED) {
            val ax = abs(dir.x)
            val ay = abs(dir.y)
            val az = abs(dir.z)

            if (ay > ax && ay > az) {
                forward = Vector(0, if (dir.y > 0) 1 else -1, 0)
            } else if (ax > az) {
                forward = Vector(if (dir.x > 0) 1 else -1, 0, 0)
            } else {
                forward = Vector(0, 0, if (dir.z > 0) 1 else -1)
            }

            // ブロックの見える面（プレイヤーから見た法線）は forward の逆方向
            val normal = forward.clone().multiply(-1.0)
            fixedShading = if (abs(normal.x) > 0.5) 0.6
            else if (abs(normal.z) > 0.5) 0.8
            else if (normal.y > 0.5) 1.0
            else 0.5
        }

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
                val py = -((y.toDouble() - imgHeight / 2.0 + 0.5) / imgHeight) * screenHeightPhysical + verticalOffset

                val pixelPos = origin.clone()
                    .add(forward.clone().multiply(distance))
                    .add(right.clone().multiply(px))
                    .add(up.clone().multiply(py))

                val ray = pixelPos.clone().subtract(origin).normalize()

                // 見える面に対するシェーディング係数の計算
                val pixelShading = if (mode == ProjectionMode.FIXED) {
                    fixedShading
                } else {
                    // FREEモード：視線レイによる見え方の面積比率を加重平均
                    val viewDir = ray.clone().multiply(-1.0)
                    val wX = abs(viewDir.x)
                    val wY = abs(viewDir.y)
                    val wZ = abs(viewDir.z)
                    val sum = wX + wY + wZ
                    val fY = if (viewDir.y > 0) 1.0 else 0.5
                    (wX * 0.6 + wY * fY + wZ * 0.8) / sum
                }

                // 目標色に一番近い基底とガラスの組み合わせを取得（シェーディングを考慮）
                val (baseMaterial, glassMaterial) = palette.getBestCombination(targetColor, pixelShading)

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