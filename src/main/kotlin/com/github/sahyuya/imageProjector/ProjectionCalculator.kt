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
        mode: ProjectionMode = ProjectionMode.FIXED,
        maxGlassLayers: Int = 2 // デフォルト2層
    ): List<BlockPlacement> {
        val placements = mutableListOf<BlockPlacement>()

        val eye = player.eyeLocation
        val world = eye.world ?: return emptyList()

        val origin = eye.toVector()
        val dir = eye.direction.normalize()

        var forward = dir.clone()
        var fixedShading = 1.0

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

        val placedBlocks = mutableSetOf<String>()
        val distBase = distance

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

                val pixelShading = if (mode == ProjectionMode.FIXED) {
                    fixedShading
                } else {
                    val viewDir = ray.clone().multiply(-1.0)
                    val wX = abs(viewDir.x)
                    val wY = abs(viewDir.y)
                    val wZ = abs(viewDir.z)
                    val sum = wX + wY + wZ
                    val fY = if (viewDir.y > 0) 1.0 else 0.5
                    (wX * 0.6 + wY * fY + wZ * 0.8) / sum
                }

                // 最大層数を指定して最適構成を取得
                val combo = palette.getBestCombination(targetColor, pixelShading, maxGlassLayers)

                // 1. 基底ブロック (Base)
                addPlacement(placements, placedBlocks, world, origin, ray, forward, distBase, combo.base)

                // 2. ガラス層群 (1〜N層)
                combo.glasses.forEachIndexed { index, glassMat ->
                    val distGlass = if (index == 0) {
                        // 1層目: 基底の1ブロック手前（微調整用）
                        distance - 1.0
                    } else {
                        // 2層目以降: 全体の20%手前(0.8), 30%手前(0.7)... と遠近法に従って離していく
                        val ratio = 1.0 - (0.1 * (index + 1))
                        val minGap = index + 1.0 // 前の層に埋まらないよう、最低でも (index+1) ブロックは離す
                        Math.min(distance - minGap, distance * ratio)
                    }

                    if (distGlass > 0) {
                        addPlacement(placements, placedBlocks, world, origin, ray, forward, distGlass, glassMat)
                    }
                }
            }
        }
        return placements
    }

    private fun addPlacement(
        placements: MutableList<BlockPlacement>,
        placedBlocks: MutableSet<String>,
        world: org.bukkit.World,
        origin: Vector,
        ray: Vector,
        forward: Vector,
        dist: Double,
        material: Material
    ) {
        val t = dist / ray.dot(forward)
        val point = origin.clone().add(ray.clone().multiply(t))
        val bx = point.blockX
        val by = point.blockY
        val bz = point.blockZ
        val key = "$bx,$by,$bz"

        if (placedBlocks.add(key)) {
            val loc = Location(world, bx.toDouble(), by.toDouble(), bz.toDouble())
            placements.add(BlockPlacement(loc, material))
        }
    }
}