package ink.ptms.glaikit

import kotlinx.coroutines.runBlocking
import taboolib.common.io.digest
import taboolib.common.io.newFile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.module.lang.asLangText
import java.io.*
import java.util.concurrent.CompletableFuture
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

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
                    evalFileAndReport(it)
                }
            }
        }
        load(newFile(getDataFolder(), "scripts", folder = true))
    }

    /**
     * 运行脚本文件，无需进行任何操作
     */
    fun evalFile(scriptFile: File, useCache: Boolean = true, notify: Boolean = true): CompletableFuture<ResultWithDiagnostics<EvaluationResult>> {
        val future = CompletableFuture<ResultWithDiagnostics<EvaluationResult>>()
        if (scriptFile.extension == "kit") {
            future.complete(evalCacheFile(scriptFile))
            return future
        }
        // 获取签名
        val digest = scriptFile.digest("sha-1")
        val cacheFile = File(getDataFolder(), "scripts/.build/${scriptFile.nameWithoutExtension}.kit")
        // 检查编译文件
        if (cacheFile.exists() && useCache) {
            val result = evalCacheFile(cacheFile, digest)
            if (result != null) {
                future.complete(result)
                return future
            }
        }
        submit(async = true) {
            runBlocking {
                if (notify) {
                    info(": ${scriptFile.name} : ${console().asLangText("script-compile")}")
                }
                val time = System.currentTimeMillis()
                scriptingHost.compiler(scriptFile.toScriptSource(), GlaiKotlinScriptConfiguration()).onSuccess {
                    if (useCache) {
                        ByteArrayOutputStream().use { byteArrayOutputStream ->
                            ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->
                                objectOutputStream.writeObject(digest)
                                objectOutputStream.writeObject(it)
                            }
                            cacheFile.writeBytes(byteArrayOutputStream.toByteArray())
                        }
                    }
                    if (notify) {
                        info(": ${scriptFile.name} : ${console().asLangText("script-compile-success", System.currentTimeMillis() - time)}")
                    }
                    scriptingHost.evaluator(it, GlaiScriptEvaluationConfiguration()).also { r -> future.complete(r) }
                }.onFailure { r -> future.complete(r) }
            }
        }
        return future
    }

    fun evalFileAndReport(scriptFile: File, sender: ProxyCommandSender = console()): CompletableFuture<ResultWithDiagnostics<EvaluationResult>> {
        return evalFile(scriptFile).also {
            it.thenApply { result ->
                result.reports.forEach { rea ->
                    if (rea.severity > ScriptDiagnostic.Severity.DEBUG && !rea.message.contains("never used")) {
                        sender.sendMessage(": ${scriptFile.name} : ${rea.message}" + if (rea.exception == null) "" else ": ${rea.exception}")
                    }
                }
            }
        }
    }

    private fun evalCacheFile(cacheFile: File, digest: String? = null): ResultWithDiagnostics<EvaluationResult>? {
        ByteArrayInputStream(cacheFile.readBytes()).use { byteArrayInputStream ->
            ObjectInputStream(byteArrayInputStream).use { objectInputStream ->
                val local = objectInputStream.readObject() as String
                if (local == digest || digest == null) {
                    val script = objectInputStream.readObject() as CompiledScript
                    return runBlocking { scriptingHost.evaluator(script, GlaiScriptEvaluationConfiguration()) }
                }
                return null
            }
        }
    }
}