package com.github.sahyuya.imageProjector

import com.github.sahyuya.imageProjector.palette.BlockPalette
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.logging.Level
import kotlin.math.tan

class ProjectImageCommand(
    private val plugin: Plugin,
    private val palette: BlockPalette
) : CommandExecutor {

    // プラグイン側の基準FOVを70に設定
    private val targetFovDegrees = 70.0

    // 限界高度見切れ対策のため0に設定
    private val verticalOffsetPhysical = 0.0

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}このコマンドはプレイヤーのみ実行可能です。")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}使い方: /projectimage <画像のURL> <投影距離> [fixed|free] [ガラス最大層数(0-7)]")
            return true
        }

        val imageUrl = args[0]
        val distance: Double

        try {
            distance = args[1].toDouble()
        } catch (e: NumberFormatException) {
            sender.sendMessage("${ChatColor.RED}距離には数値を指定してください。")
            return true
        }

        val modeStr = args.getOrNull(2)?.lowercase() ?: "fixed"
        val mode = if (modeStr == "free") ProjectionMode.FREE else ProjectionMode.FIXED

        // 追加: ガラスの最大層数を引数から取得（デフォルト2層、最大7層に制限）
        val maxLayers = args.getOrNull(3)?.toIntOrNull()?.coerceIn(0, 7) ?: 2

        sender.sendMessage("${ChatColor.AQUA}画像のダウンロードと解析を開始します (FOV: $targetFovDegrees, モード: $modeStr, 最大層数: $maxLayers)...")

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                // 1. 距離とFOVから、投影面の物理サイズ（メートル＝ブロック数）を逆算
                val fovRadians = Math.toRadians(targetFovDegrees)
                val screenHeightPhysical = 2.0 * distance * tan(fovRadians / 2.0)
                val screenWidthPhysical = screenHeightPhysical * (16.0 / 9.0)

                // 2. 隙間を防ぐため、物理サイズより少し高密度(1.5倍)でサンプリングする
                val targetWidth = (screenWidthPhysical * 1.5).toInt()
                val targetHeight = (screenHeightPhysical * 1.5).toInt()

                if (targetWidth <= 0 || targetHeight <= 0) {
                    sender.sendMessage("${ChatColor.RED}距離が短すぎるか計算エラーです。")
                    return@Runnable
                }

                // 3. 画像の取得、16:9クロップ、リサイズ
                val processedImage = ImageProcessor.fetchAndProcessImage(imageUrl, targetWidth, targetHeight)

                // 4. 配置ブロックの計算 (モードと最大層数を渡す)
                val calculator = ProjectionCalculator(palette)
                val placements = calculator.calculate(sender, processedImage, distance, targetFovDegrees, verticalOffsetPhysical, mode, maxLayers)

                sender.sendMessage("${ChatColor.YELLOW}計算完了。ブロックの配置を開始します... (総ブロック数: ${placements.size})")

                // メインスレッドで配置タスクを実行
                BlockPlacer(sender, placements).runTaskTimer(plugin, 0L, 1L)

            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "投影処理中にエラーが発生しました", e)
                sender.sendMessage("${ChatColor.RED}エラーが発生しました: ${e.message}")
            }
        })

        return true
    }
}