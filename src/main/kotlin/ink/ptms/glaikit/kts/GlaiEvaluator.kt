package ink.ptms.glaikit.kts

import kotlinx.coroutines.runBlocking
import taboolib.common.io.digest
import taboolib.common.io.newFile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.*
import taboolib.module.lang.asLangText
import taboolib.module.lang.sendLang
import java.io.*
import java.util.concurrent.CompletableFuture
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * GlaiKit
 * ink.ptms.glaikit.kts.GlaiEvaluator
 *
 * @author 坏黑
 * @since 2021/12/28 2:37 AM
 */
object GlaiEvaluator {

    val scriptingHost = BasicJvmScriptingHost()

    /**
     * 运行所有脚本文件
     */
    fun setupScriptFiles() {
        // 创建环境目录
        newFile(getDataFolder(), "scripts/.lazy", folder = true)
        newFile(getDataFolder(), "scripts/.build", folder = true)
        newFile(getDataFolder(), "scripts/.include", folder = true)
        // 加载脚本
        fun load(file: File) {
            if (file.name.startsWith(".")) {
                return
            }
            file.listFiles()?.forEach {
                if (it.isDirectory) {
                    load(it)
                } else if (it.extension == "kts") {
                    evalAndReport(it)
                }
            }
        }
        load(newFile(getDataFolder(), "scripts", folder = true))
    }

    /**
     * 运行脚本文件，无需进行任何操作
     * 重新编译的条件：
     * 1. 文件被修改
     * 2. providedProperties 中属性的数量、名称或类型被修改
     *
     * @param useCache 在缓存文件存在时是否使用缓存，同时关闭后将不会产生缓存文件
     * @param logging 是否向控制台发送脚本运行信息
     * @param props 运行参数
     * @param sender 日志接受者
     * @param compileAsync 是否在异步编译脚本
     */
    fun eval(
        scriptFile: File,
        useCache: Boolean = true,
        logging: Boolean = true,
        props: ScriptRuntimeProperty = ScriptRuntimeProperty(),
        sender: ProxyCommandSender = console(),
        compileAsync: Boolean = true,
    ): CompletableFuture<ResultWithDiagnostics<EvaluationResult>> {
        val future = CompletableFuture<ResultWithDiagnostics<EvaluationResult>>()
        val digest = arrayOf(scriptFile.digest("sha-1"), props.digest)

        /**
         * 运行脚本
         */
        fun run(file: File, warning: Boolean): Boolean {
            val compiledFile = GlaiScriptFile.loadFromFile(file)
            return if (compiledFile.digest[1] != digest[1]) {
                // 启用警告则告知用户参数错误
                if (warning) {
                    sender.sendLang("script-compile-property-not-match", scriptFile.name, compiledFile.propsSize, compiledFile.propsDescription)
                }
                false
            } else {
                future.complete(compiledFile.eval(props))
                true
            }
        }

        // 直接运行编译文件
        if (scriptFile.extension == "kit") {
            run(scriptFile, true)
            return future
        }

        // 使用缓存
        val cacheFile = File(getDataFolder(), "scripts/.build/${scriptFile.nameWithoutExtension}.kit")
        if (cacheFile.exists() && useCache && run(cacheFile, false)) {
            return future
        }

        // 异步编译
        // 但在插件加载时将会在主线程中编译，以确保命令能够有效的注册
        submit(async = compileAsync) {
            runBlocking {
                if (logging) {
                    info(console().asLangText("script-compile", scriptFile.name))
                }
                val time = System.currentTimeMillis()
                val configuration = GlaiCompilationConfiguration(props)
                GlaiCompiler.compile(configuration, scriptFile, if (useCache) cacheFile else null, digest) {
                    if (logging) {
                        info(console().asLangText("script-compile-success", scriptFile.name, System.currentTimeMillis() - time))
                    }
                    // 编译完成后自动运行脚本
                    it.eval(props).also { r -> future.complete(r) }
                }.onFailure { r -> future.complete(r) }
            }
        }
        return future
    }

    /**
     * 运行文件并打印运行日志
     */
    fun evalAndReport(scriptFile: File): CompletableFuture<ResultWithDiagnostics<EvaluationResult>> {
        return eval(scriptFile).also {
            it.thenApply { result ->
                result.reports.forEach { rea ->
                    if (rea.severity > ScriptDiagnostic.Severity.DEBUG && !rea.message.contains("never used")) {
                        info(": ${scriptFile.name} : ${rea.message}" + if (rea.exception == null) "" else ": ${rea.exception}")
                    }
                }
            }
        }
    }
}