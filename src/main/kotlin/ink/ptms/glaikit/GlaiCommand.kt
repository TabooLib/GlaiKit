package ink.ptms.glaikit

import ink.ptms.glaikit.kts.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import taboolib.common.io.newFile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.submit
import taboolib.expansion.createHelper
import taboolib.module.lang.sendLang
import java.io.File

@CommandHeader(name = "glaikit", permission = "*", aliases = ["kit"])
object GlaiCommand {

    private val scriptFolder = newFile(getDataFolder(), "scripts", folder = true)

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val eval = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            val scripts = scriptFolder.findScripts().filter { !GlaiEvaluator.isScriptRunning(it) }
            if (scripts.isEmpty()) {
                sender.sendLang("script-eval-empty")
            } else {
                GlaiEnv.setupGlobalImports()
                scripts.forEach { GlaiEvaluator.eval(it, messageReceiver = sender) }
            }
        }
        dynamic(commit = "file") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                scriptFolder.findScripts().map { it.name }.toSet().toList()
            }
            execute<ProxyCommandSender> { sender, _, argument ->
                val file = script(argument)
                if (file != null) {
                    GlaiEvaluator.eval(file, messageReceiver = sender)
                } else {
                    sender.sendLang("script-file-not-found", argument)
                }
            }
        }
    }

    @CommandBody
    val compile = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            val scripts = scriptFolder.findScripts()
            if (scripts.isEmpty()) {
                sender.sendLang("script-compile-empty")
            } else {
                asyncBlocking {
                    scripts.forEach { compile(sender, it) }
                }
            }
        }
        dynamic(commit = "file") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                scriptFolder.findScripts().map { it.name }.toSet().toList()
            }
            execute<ProxyCommandSender> { sender, _, argument ->
                asyncBlocking {
                    val file = script(argument)
                    if (file != null) {
                        compile(sender, file)
                    } else {
                        sender.sendLang("script-file-not-found", argument)
                    }
                }
            }
        }
    }

    @CommandBody
    val release = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            val containers = GlaiScriptManager.getScriptContainers()
            if (containers.isEmpty()) {
                sender.sendLang("script-release-empty")
            } else {
                containers.forEach { it.release() }
                sender.sendLang("script-release-all")
            }
        }
        dynamic(commit = "file") {
            suggestion<ProxyCommandSender> { _, _ ->
                GlaiScriptManager.getScriptContainers().map { it.baseId }
            }
            execute<ProxyCommandSender> { sender, _, argument ->
                GlaiScriptManager.getScriptContainer(argument)!!.release()
                sender.sendLang("script-release", argument)
            }
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            GlaiKit.conf.reload()
            GlaiEnv.setupGlobalImports()
            sender.sendLang("script-reload")
        }
        dynamic(commit = "script") {
            suggestion<ProxyCommandSender> { _, _ ->
                GlaiScriptManager.getScriptContainers().map { it.baseId }
            }
            execute<ProxyCommandSender> { sender, _, argument ->
                asyncBlocking {
                    val file = script(argument)
                    if (file != null) {
                        // 先编译再释放
                        if (compile(sender, file)) {
                            GlaiScriptManager.getScriptContainer(argument)!!.release()
                            GlaiEvaluator.eval(file, messageReceiver = sender)
                        }
                    } else {
                        sender.sendLang("script-file-not-found", argument)
                    }
                }
            }
        }
    }

    @CommandBody
    val info = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendLang("script-info", GlaiScriptManager.getScriptContainers().map { it.baseId }.toString())
        }
    }

    fun script(name: String): File? {
        return scriptFolder.findScripts().firstOrNull { it.name == name || it.nameWithoutExtension == name }
    }

    suspend fun compile(sender: ProxyCommandSender, file: File): Boolean {
        val name = file.nameWithoutExtension
        sender.sendLang("script-compile", name)
        val time = System.currentTimeMillis()
        val compiledFile = File(getDataFolder(), "scripts/.build/$name.kit")
        val configuration = GlaiCompilationConfiguration(ScriptRuntimeProperty())
        val script = GlaiCompiler.compileToScript(configuration, file, compiledFile, sender)
        if (script != null) {
            sender.sendLang("script-compile-success", name, System.currentTimeMillis() - time)
            return true
        }
        return false
    }

    fun asyncBlocking(block: suspend CoroutineScope.() -> Unit) {
        submit(async = true) { runBlocking { block() } }
    }
}