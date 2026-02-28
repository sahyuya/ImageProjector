package com.github.sahyuya.imageProjector.task

import com.github.sahyuya.imageProjector.ProjectionCalculator
import org.bukkit.ChatColor
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
            placement.location.block.type = placement.material
            index++
        }

        if (index >= placements.size) {
            player.sendMessage("${ChatColor.GREEN}全ブロックの配置が完了しました！ (配置ブロック数: ${placements.size})")
            cancel()
        }
    }
}