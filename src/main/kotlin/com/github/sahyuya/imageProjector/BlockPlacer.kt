package com.github.sahyuya.imageProjector

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class BlockPlacer(
    private val player: Player,
    private val placements: List<ProjectionCalculator.BlockPlacement>,
    private val blocksPerTick: Int = 500
) : BukkitRunnable() {

    private var index = 0

    override fun run() {
        for (i in 0 until blocksPerTick) {
            if (index >= placements.size) break
            val placement = placements[index]
            val block = placement.location.block

            block.type = placement.material

            // コンクリートパウダー落下対策：直下が空気ならバリアを敷く
            if (placement.material.name.endsWith("_POWDER")) {
                val belowBlock = block.getRelative(BlockFace.DOWN)
                if (belowBlock.type.isAir) {
                    belowBlock.type = Material.BARRIER
                }
            }

            index++
        }

        if (index >= placements.size) {
            player.sendMessage("${ChatColor.GREEN}全ブロックの配置が完了しました！ (配置ブロック数: ${placements.size})")
            cancel()
        }
    }
}