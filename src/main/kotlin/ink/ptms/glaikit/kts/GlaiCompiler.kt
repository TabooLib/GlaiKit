package ink.ptms.glaikit.kts

import taboolib.common.io.digest
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.host.toScriptSource

object GlaiCompiler {

    fun newGlaiCompilationConfiguration(props: ScriptRuntimeProperty): GlaiCompilationConfiguration {
        return GlaiCompilationConfiguration(props)
    }

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
                ByteArrayOutputStream().use { byteArrayOutputStream ->
                    ObjectOutputStream(byteArrayOutputStream).use { out ->
                        out.writeInt(4)
                        out.writeObject(digest[0])
                        out.writeObject(digest[1])
                        val props = compilationConfiguration.props.providedProperties
                        out.writeInt(props.size)
                        out.writeObject(props.map { (k, v) -> "$k: ${v.javaClass.name}" }.joinToString(", "))
                        out.writeObject(it)
                    }
                    compiledFile.writeBytes(byteArrayOutputStream.toByteArray())
                }
            }
            onSuccess(GlaiScript(it))
        }
    }
}