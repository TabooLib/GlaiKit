package ink.ptms.glaikit.kts

import taboolib.common.io.digest
import taboolib.common.io.newFile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.console
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.util.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.host.toScriptSource

/**
 * GlaiKit
 * ink.ptms.glaikit.kts.GlaiCompiler
 *
 * @author 坏黑
 * @since 2021/12/28 2:37 AM
 */
object GlaiCompiler {

    const val SERIALIZE_VERSION = 1

    fun newGlaiCompilationConfiguration(props: ScriptRuntimeProperty): GlaiCompilationConfiguration {
        return GlaiCompilationConfiguration(props)
    }

    /**
     * 将字符串编译成脚本
     *
     * @param compilationConfiguration 编译配置
     * @param script 脚本内容
     * @param messageReceiver 日志接收者
     * @param report 是否报告运行结果
     */
    suspend fun compileToScript(
        compilationConfiguration: GlaiCompilationConfiguration,
        script: String,
        messageReceiver: ProxyCommandSender = console(),
        report: Boolean = true,
        id: String = "$",
    ): GlaiScript? {
        val compiler = GlaiEvaluator.scriptingHost.compiler(StringScriptSource(script), compilationConfiguration)
        val compiledScript = compiler.valueOrNull()
        if (compiledScript == null) {
            if (report) {
                GlaiEvaluator.reportResult(compiler.reports, messageReceiver, id)
            }
            return null
        }
        return GlaiScript(compiledScript)
    }

    suspend fun compileToScript(
        compilationConfiguration: GlaiCompilationConfiguration,
        sourceFile: File,
        compiledFile: File,
        messageReceiver: ProxyCommandSender = console(),
        report: Boolean = true,
    ): GlaiScript? {
        val compiler = GlaiEvaluator.scriptingHost.compiler(sourceFile.toScriptSource(), compilationConfiguration)
        val compiledScript = compiler.valueOrNull()
        if (compiledScript == null) {
            if (report) {
                GlaiEvaluator.reportResult(compiler.reports, messageReceiver, sourceFile.nameWithoutExtension)
            }
            return null
        }
        val digest = arrayOf(sourceFile.digest("sha-1"), compilationConfiguration.props.digest)
        writeToFile(compiledFile, digest, compilationConfiguration.props.providedProperties, compiledScript)
        return GlaiScript(compiledScript)
    }

    /**
     * 编译脚本文件
     *
     * @param compilationConfiguration 编译配置
     * @param sourceFile 源文件
     * @param compiledFile 目标文件
     * @param digest 文件签名
     * @param onSuccess 成功回调
     */
    @Suppress("SimplifiableCallChain")
    suspend fun compile(
        compilationConfiguration: GlaiCompilationConfiguration,
        sourceFile: File,
        compiledFile: File? = null,
        digest: Array<String> = arrayOf(sourceFile.digest("sha-1"), compilationConfiguration.props.digest),
        onSuccess: (GlaiScript) -> ResultWithDiagnostics<EvaluationResult>,
    ): ResultWithDiagnostics<EvaluationResult> {
        return GlaiEvaluator.scriptingHost.compiler(sourceFile.toScriptSource(), compilationConfiguration).onSuccess {
            if (compiledFile != null) {
                writeToFile(compiledFile, digest, compilationConfiguration.props.providedProperties, it)
            }
            onSuccess(GlaiScript(it))
        }
    }

    @Suppress("SimplifiableCallChain")
    fun writeToFile(compiledFile: File, digest: Array<String>, props: Properties, compiledScript: CompiledScript) {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            ObjectOutputStream(byteArrayOutputStream).use { out ->
                out.writeInt(SERIALIZE_VERSION)
                out.writeInt(4)
                out.writeObject(digest[0])
                out.writeObject(digest[1])
                out.writeObject(props.size)
                out.writeObject(props.map { (k, v) -> "$k: ${v.javaClass.name}" }.joinToString(", "))
                out.writeObject(compiledScript)
            }
            newFile(compiledFile).writeBytes(byteArrayOutputStream.toByteArray())
        }
    }
}