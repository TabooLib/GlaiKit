package ink.ptms.glaikit

import ink.ptms.glaikit.kts.GlaiEvaluator
import taboolib.common.io.newFile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.expansion.createHelper
import taboolib.module.lang.asLangText
import taboolib.module.lang.sendLang
import java.io.File

@CommandHeader(name = "glaikit", permission = "*", aliases = ["kit"])
object GlaiCommand {

    val scriptFile = newFile(getDataFolder(), "scripts", folder = true)

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val eval = subCommand {
        // 运行
        execute<ProxyCommandSender> { _, _, _ ->
            GlaiEnv.setupGlobalImports()
            GlaiEvaluator.setupScriptFiles()
        }
        dynamic(commit = "file") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                scriptFile.listFiles()?.filter { it.extension == "kts" || it.extension == "kit" }?.map { it.name }
            }
            execute<ProxyCommandSender> { sender, _, argument ->
                val file = File(getDataFolder(), "scripts/${argument}")
                if (file.exists() && (file.extension == "kts" || file.extension == "kit")) {
                    GlaiEvaluator.eval(file).thenAccept {
                        info(console().asLangText("script-eval", file.name))
                    }
                } else {
                    sender.sendLang("script-file-not-found", argument)
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