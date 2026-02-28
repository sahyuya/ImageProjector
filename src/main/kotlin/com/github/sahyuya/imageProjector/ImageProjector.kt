package com.github.sahyuya.imageProjector

import com.github.sahyuya.imageProjector.palette.BlockPalette
import org.bukkit.plugin.java.JavaPlugin

class ImageProjector : JavaPlugin() {
    private lateinit var palette: BlockPalette

    override fun onEnable() {
        logger.info("ImageProjector Plugin Enabled (Kotlin / Refactored).")

        // パレットの初期化
        palette = BlockPalette()
        palette.initialize()

        // コマンドの登録
        getCommand("projectimage")?.setExecutor(ProjectImageCommand(this, palette))
    }
}