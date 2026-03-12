package com.github.sahyuya.imageProjector

import com.github.sahyuya.imageProjector.core.BlockPlacer
import com.github.sahyuya.imageProjector.core.ImageProcessor
import com.github.sahyuya.imageProjector.core.ProjectionCalculator
import com.github.sahyuya.imageProjector.palette.ConcreteGlassPalette
import com.github.sahyuya.imageProjector.palette.FullBlockPalette
import org.bukkit.plugin.java.JavaPlugin

/**
 * プラグインのメインクラス。
 * 各機能のインスタンスを生成し、コマンドに依存関係として注入（DI）します。
 */
class ImageProjector : JavaPlugin() {
    override fun onEnable() {
        // パレットの初期化
        val fullPalette = FullBlockPalette().apply { initialize() }
        val concretePalette = ConcreteGlassPalette().apply { initialize() }

        // コア機能の初期化
        val imageProcessor = ImageProcessor()
        val calculator = ProjectionCalculator()
        // 分散配置用のタスクを回すために、プラグイン自身のインスタンスを渡す
        val blockPlacer = BlockPlacer(this)

        // コマンドの登録
        getCommand("print")?.setExecutor(
            ProjectImageCommand(this, imageProcessor, calculator, blockPlacer, fullPalette, concretePalette)
        )

        logger.info("ImageProjector (Anamorphic Art Edition) enabled.")
    }
}