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
            suggestion<ProxyCommandSender> { _, _ ->
                newFile(getDataFolder(), "scripts", folder = true).listFiles()!!.map {
                    it.nameWithoutExtension
                }
            }
            execute<ProxyCommandSender> { _, _, argument ->
                GlaiEvaluator.evalFile(File(getDataFolder(), "scripts/${argument}.kit"))
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