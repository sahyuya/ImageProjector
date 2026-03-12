package com.github.sahyuya.imageProjector.core

import com.github.sahyuya.imageProjector.palette.DepthProfile
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.Directional
import org.bukkit.block.data.type.Slab
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class BlockPlacer(private val plugin: Plugin) {

    fun placeBlocks(placements: List<PlacementData>, onComplete: (Int) -> Unit) {
        val placedCoords = mutableSetOf<String>()
        val total = placements.size
        var currentIndex = 0
        val blocksPerTick = 1000
        var actualPlacedCount = 0 // 実際にワールドに書き込んだ数をカウント

        object : BukkitRunnable() {
            override fun run() {
                var count = 0
                while (currentIndex < total && count < blocksPerTick) {
                    val data = placements[currentIndex]
                    val loc = data.location

                    val ix = loc.blockX
                    val iy = loc.blockY
                    val iz = loc.blockZ
                    val key = "$ix,$iy,$iz"

                    if (!placedCoords.contains(key) && iy in -64..319) {
                        placedCoords.add(key)

                        try {
                            val world = loc.world
                            if (world != null) {
                                // チャンクが未ロードの場合、設置が無視されるのを防ぐため強制ロード
                                val chunkX = ix shr 4
                                val chunkZ = iz shr 4
                                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                                    world.loadChunk(chunkX, chunkZ)
                                }

                                val block = world.getBlockAt(ix, iy, iz)
                                block.setType(data.entry.material, false)

                                try {
                                    adjustBlockData(block, data.rayDir, data.entry.profile)
                                } catch (e: Exception) {
                                    // BlockDataの調整に失敗してもブロック自体は置かれるように保護
                                }
                                actualPlacedCount++
                            }
                        } catch (e: Exception) {
                            // Bukkit側のエラーでタスクが死ぬのを防ぐ
                        }
                    }
                    currentIndex++
                    count++
                }

                if (currentIndex >= total) {
                    onComplete(actualPlacedCount)
                    this.cancel()
                }
            }
        }.runTaskTimer(plugin, 1L, 1L)
    }

    private fun adjustBlockData(block: Block, rayDir: Vector, profile: DepthProfile) {
        val data = block.blockData
        when (data) {
            is TrapDoor -> {
                data.isOpen = true
                data.facing = getFacing(rayDir).oppositeFace
                block.setBlockData(data, false)
            }
            is Slab -> {
                data.type = if (profile == DepthProfile.THIN_BACK) Slab.Type.TOP else Slab.Type.BOTTOM
                block.setBlockData(data, false)
            }
            is Directional -> {
                try {
                    data.facing = getFacing(rayDir).oppositeFace
                    block.setBlockData(data, false)
                } catch (e: Exception) {}
            }
        }
    }

    private fun getFacing(v: Vector): BlockFace {
        val ax = Math.abs(v.x); val ay = Math.abs(v.y); val az = Math.abs(v.z)
        return when {
            ax >= ay && ax >= az -> if (v.x > 0) BlockFace.EAST else BlockFace.WEST
            ay >= ax && ay >= az -> if (v.y > 0) BlockFace.UP else BlockFace.DOWN
            else -> if (v.z > 0) BlockFace.SOUTH else BlockFace.NORTH
        }
    }
}