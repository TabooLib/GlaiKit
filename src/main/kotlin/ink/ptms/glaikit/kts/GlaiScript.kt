package ink.ptms.glaikit.kts

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics

/**
 * GlaiKit
 * ink.ptms.glaikit.kts.GlaiScript
 *
 * @author 坏黑
 * @since 2022/4/12 01:57
 */
open class GlaiScript(val compiledScript: CompiledScript) {

    fun newGlaiEvaluationConfiguration(props: ScriptRuntimeProperty): GlaiEvaluationConfiguration {
        return GlaiEvaluationConfiguration(props)
    }

    fun eval(props: ScriptRuntimeProperty): ResultWithDiagnostics<EvaluationResult> {
        return eval(newGlaiEvaluationConfiguration(props))
    }

    fun eval(evaluationConfiguration: GlaiEvaluationConfiguration): ResultWithDiagnostics<EvaluationResult> {
        // TODO: 2022/4/12 脚本注入
        return runBlocking { GlaiEvaluator.scriptingHost.evaluator(compiledScript, evaluationConfiguration) }
    }

    /**
     * 获取脚本主类
     */
    suspend fun getClass(evaluationConfiguration: GlaiEvaluationConfiguration): ResultWithDiagnostics<KClass<*>> {
        return compiledScript.getClass(evaluationConfiguration)
    }
}