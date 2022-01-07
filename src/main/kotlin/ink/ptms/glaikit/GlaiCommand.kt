package ink.ptms.glaikit

import taboolib.common.io.newFile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.getDataFolder
import taboolib.expansion.createHelper
import java.io.File

@CommandHeader(name = "glaikit", permission = "*", aliases = ["kit"])
object GlaiCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val eval = subCommand {
        // 运行
        execute<ProxyCommandSender> { _, _, _ ->
            GlaiEvaluator.setupScriptFiles()
        }
        dynamic(commit = "file") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                newFile(getDataFolder(), "scripts", folder = true).listFiles()
                    ?.filter { it.extension == "kts" || it.extension == "kit" }
                    ?.map { it.name }
            }
            execute<ProxyCommandSender> { sender, _, argument ->
                val file = File(getDataFolder(), "scripts/${argument}")
                if (file.exists() && (file.extension == "kts" || file.extension == "kit")) {
                    GlaiEvaluator.evalFileAndReport(file)
                } else {
                    sender.sendMessage("§cScript file not found")
                }
            }
        }
    }

    @CommandBody
    val release = subCommand {
        // 释放
    }

    @CommandBody
    val compile = subCommand {
        // 编译
    }
}