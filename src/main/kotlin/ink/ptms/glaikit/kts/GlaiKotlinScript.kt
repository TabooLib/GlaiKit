package ink.ptms.glaikit.kts

import ink.ptms.glaikit.GlaiEnv
import ink.ptms.glaikit.scripting.ScriptBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.mainKts.CompilerOptions
import org.jetbrains.kotlin.mainKts.Import
import org.jetbrains.kotlin.mainKts.impl.IvyResolver
import taboolib.common.platform.function.getDataFolder
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.util.filterByAnnotationType

@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = GlaiCompilationConfiguration::class,
    evaluationConfiguration = GlaiEvaluationConfiguration::class
)
class GlaiKotlinScript(val args: Array<String>)

/**
 * 编译配置
 */
@Suppress("SimplifiableCallChain")
class GlaiCompilationConfiguration(val props: ScriptRuntimeProperty = ScriptRuntimeProperty()) : ScriptCompilationConfiguration(
    {
        baseClass(ScriptBase::class)
        defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class)
        defaultImports.append(GlaiEnv.globalImports)
        jvm {
            dependenciesFromClassContext(GlaiCompilationConfiguration::class, wholeClasspath = true)
            compilerOptions("-jvm-target", "1.8")
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, Import::class, CompilerOptions::class, handler = GlaiCompilationConfigurationHandler())
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        val map: MutableList<Pair<String, KClass<*>>> = props.providedProperties.map { it.key.toString() to it.value.javaClass.kotlin }.toMutableList()
        map += "runArgs" to Properties::class
        providedProperties(*map.toTypedArray())
    }
)

/**
 * 运行配置
 */
class GlaiEvaluationConfiguration(val props: ScriptRuntimeProperty = ScriptRuntimeProperty()) : ScriptEvaluationConfiguration(
    {
        scriptsInstancesSharing(true)
        jvm {
            baseClassLoader(GlaiCompilationConfiguration::class.java.classLoader)
            loadDependencies(false)
        }
        val map: MutableList<Pair<String, *>> = props.providedProperties.map { it.key.toString() to it.value }.toMutableList()
        map += "runArgs" to props.runArgs
        providedProperties(*map.toTypedArray())
    }
)

/**
 * 编译配置处理器
 */
class GlaiCompilationConfigurationHandler : RefineScriptCompilationConfigurationHandler {

    private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), IvyResolver())

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        return processAnnotations(context)
    }

    fun processAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        fun report(severity: ScriptDependenciesResolver.ReportSeverity, message: String, position: ScriptContents.Position?) {
            diagnostics.add(
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    message,
                    mapLegacyDiagnosticSeverity(severity),
                    context.script.locationId,
                    mapLegacyScriptPosition(position)
                )
            )
        }

        val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()

        val importedSources = annotations.filterByAnnotationType<Import>().flatMap {
            it.annotation.paths.mapNotNull { sourceName ->
                FileScriptSource(findImportFile(sourceName) ?: return@mapNotNull null)
            }
        }
        val compileOptions = annotations.filterByAnnotationType<CompilerOptions>().flatMap {
            it.annotation.options.toList()
        }

        val resolveResult = try {
            runBlocking {
                resolver.resolveFromScriptSourceAnnotations(annotations.filter { it.annotation is DependsOn || it.annotation is Repository })
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(*diagnostics.toTypedArray(), e.asDiagnostics(path = context.script.locationId))
        }

        return resolveResult.onSuccess { resolvedClassPath ->
            ScriptCompilationConfiguration(context.compilationConfiguration) {
                updateClasspath(resolvedClassPath)
                if (importedSources.isNotEmpty()) importScripts.append(importedSources)
                if (compileOptions.isNotEmpty()) compilerOptions.append(compileOptions)
            }.asSuccess()
        }
    }
}

private fun findImportFile(sourceName: String): File? {
    File(getDataFolder(), "scripts/.lazy/$sourceName").takeIf { it.exists() }?.let { return it }
    File(getDataFolder(), "scripts/$sourceName").takeIf { it.exists() }?.let { return it }
    return null
}