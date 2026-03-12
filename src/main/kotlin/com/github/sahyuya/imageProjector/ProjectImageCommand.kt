package com.github.sahyuya.imageProjector

import com.github.sahyuya.imageProjector.core.BlockPlacer
import com.github.sahyuya.imageProjector.core.ImageProcessor
import com.github.sahyuya.imageProjector.core.ProjectionCalculator
import com.github.sahyuya.imageProjector.palette.BlockPalette
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.concurrent.thread

class ProjectImageCommand(
    private val plugin: Plugin,
    private val imageProcessor: ImageProcessor,
    private val calculator: ProjectionCalculator,
    private val blockPlacer: BlockPlacer,
    private val fullPalette: BlockPalette,
    private val concretePalette: BlockPalette
) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("プレイヤーのみ実行可能です。")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("${ChatColor.RED}使用法: /print <画像URL> <描画距離> [-f] [-c]")
            return true
        }

        val urlStr = args[0]
        val distance = args[1].toDoubleOrNull()
        if (distance == null || distance <= 0) {
            sender.sendMessage("${ChatColor.RED}描画距離は正の数値で指定してください。")
            return true
        }

        val options = args.drop(2).map { it.lowercase() }
        val isFreeMode = options.contains("-f")
        val useConcrete = options.contains("-c")
        val activePalette = if (useConcrete) concretePalette else fullPalette

        // 【修正】非同期処理に入る"前"（メインスレッド）で、プレイヤーの座標・視点を安全に取得・固定する
        // 起点をプレイヤーの目の高さ(+1.5)に設定
        val originLoc = sender.location.clone().apply { add(0.0, 1.5, 0.0) }
        val yaw = originLoc.yaw
        val pitch = originLoc.pitch

        sender.sendMessage("${ChatColor.AQUA}画像をダウンロード・解析中...")

        thread {
            try {
                // 負荷軽減のため画像を150ピクセルにリサイズ
                val image = imageProcessor.fetchImage(urlStr, 150)

                // 【修正】過去の設計通り、座標の衝突（自滅）を防ぐため「1ピクセル ＝ 1ブロック」の等倍スケールに戻す
                val widthScale = image.width.toDouble()
                val heightScale = image.height.toDouble()

                sender.sendMessage("${ChatColor.GREEN}投影計算を開始します...")

                // 計算自体は非同期で行い、サーバーのフリーズを防ぐ
                val placements = calculator.calculatePlacements(
                    palette = activePalette,
                    image = image,
                    origin = originLoc,
                    yaw = yaw,
                    pitch = pitch,
                    widthScale = widthScale,
                    heightScale = heightScale,
                    baseDistance = distance,
                    freeMode = isFreeMode
                )

                if (placements.isEmpty()) {
                    sender.sendMessage("${ChatColor.RED}配置できるブロックがありませんでした。透過画像か確認してください。")
                    return@thread
                }

                // ブロックの実際の配置はメインスレッドに戻して安全に実行
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage("${ChatColor.YELLOW}約${placements.size}個のブロック配置を開始しました。完了までお待ちください...")

                    blockPlacer.placeBlocks(placements) { actualPlaced ->
                        sender.sendMessage("${ChatColor.GOLD}アートの生成が完了しました！ (実際に置かれたブロック数: $actualPlaced)")
                        if (actualPlaced == 0) {
                            sender.sendMessage("${ChatColor.RED}※配置数が0でした。描画距離が遠すぎるか、空中に置けない設定になっている可能性があります。")
                        }
                    }
                })

            } catch (e: Exception) {
                sender.sendMessage("${ChatColor.RED}エラーが発生しました: ${e.message}")
                e.printStackTrace()
            }
        }
        return true
    }
}