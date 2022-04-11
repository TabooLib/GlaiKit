package ink.ptms.glaikit.kts

import kotlinx.coroutines.runBlocking
import taboolib.common.io.digest
import taboolib.common.io.newFile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.module.lang.asLangText
import taboolib.module.lang.sendLang
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.onFailure
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
    fun setupScriptFiles(compileAsync: Boolean = true) {
        // 创建环境目录
        // 目录前带有 "." 将不会自动运行
        newFile(getDataFolder(), "scripts/.lazy", folder = true)
        newFile(getDataFolder(), "scripts/.build", folder = true)
        // 加载脚本
        fun load(file: File) {
            if (file.name.startsWith(".")) {
                return
            }
            file.listFiles()?.forEach {
                if (it.isDirectory) {
                    load(it)
                } else if (it.extension == "kts") {
                    eval(it, compileAsync = compileAsync)
                }
            }
        }
        load(newFile(getDataFolder(), "scripts", folder = true))
    }

    /**
     * 运行脚本文件，重新编译的条件：
     * 1. 文件被修改
     * 2. providedProperties 中属性的数量、名称或类型被修改
     *
     * @param cache 在缓存文件存在时是否使用缓存，同时关闭后将不会产生缓存文件
     * @param logging 是否向控制台发送脚本运行信息
     * @param messageReceiver 日志接收者
     * @param runtimeProperty 运行参数
     * @param compileAsync 是否在异步编译脚本
     * @param report 是否报告运行结果
     */
    fun eval(
        file: File,
        cache: Boolean = true,
        logging: Boolean = true,
        messageReceiver: ProxyCommandSender = console(),
        runtimeProperty: ScriptRuntimeProperty = ScriptRuntimeProperty(),
        compileAsync: Boolean = true,
        report: Boolean = true,
    ): CompletableFuture<ResultWithDiagnostics<EvaluationResult>> {
        val future = CompletableFuture<ResultWithDiagnostics<EvaluationResult>>()
        val digest = arrayOf(file.digest("sha-1"), runtimeProperty.digest)

        /**
         * 运行脚本
         */
        fun run(file: File, warning: Boolean): Boolean {
            val compiledFile = GlaiScriptFile.loadFromFile(file)
            return if (compiledFile.digest[1] != digest[1]) {
                // 启用警告则告知用户参数错误
                if (warning) {
                    messageReceiver.sendLang("script-compile-property-not-match", file.name, compiledFile.propsSize, compiledFile.propsDescription)
                }
                false
            } else {
                future.complete(compiledFile.eval(runtimeProperty))
                true
            }
        }

        // 直接运行编译文件
        if (file.extension == "kit") {
            run(file, true)
            return future
        }

        // 使用缓存
        val cacheFile = File(getDataFolder(), "scripts/.build/${file.nameWithoutExtension}.kit")
        if (cacheFile.exists() && cache && run(cacheFile, false)) {
            return future
        }

        // 异步编译
        // 但在插件加载时将会在主线程中编译，以确保命令能够有效的注册
        submit(async = compileAsync) {
            runBlocking {
                if (logging) {
                    info(console().asLangText("script-compile", file.name))
                }
                val time = System.currentTimeMillis()
                val compiledFile = if (cache) cacheFile else null
                val configuration = GlaiCompilationConfiguration(runtimeProperty)
                GlaiCompiler.compile(configuration, file, compiledFile, digest) {
                    if (logging) {
                        info(console().asLangText("script-compile-success", file.name, System.currentTimeMillis() - time))
                    }
                    // 编译完成后自动运行脚本
                    it.eval(runtimeProperty).also { r -> future.complete(r) }
                }.onFailure { r -> future.complete(r) }
            }
        }

        // 报告运行结果
        if (report) {
            future.thenApply { result ->
                result.reports.forEach { rea ->
                    if (rea.severity > ScriptDiagnostic.Severity.DEBUG && !rea.message.contains("never used")) {
                        messageReceiver.sendLang("script-eval-report", file.name, rea.message + if (rea.exception == null) "" else ": ${rea.exception}")
                    }
                }
            }
        }
        return future
    }
}