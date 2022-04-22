package ink.ptms.glaikit.kts

import ink.ptms.glaikit.GlaiEnv
import ink.ptms.glaikit.scripting.Import
import ink.ptms.glaikit.scripting.Include
import ink.ptms.glaikit.scripting.Script
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.mainKts.CompilerOptions
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
        baseClass(Script::class)
        defaultImports(DependsOn::class, Repository::class, Include::class, Import::class, CompilerOptions::class)
        defaultImports.append(GlaiEnv.globalImports)
        jvm {
            dependenciesFromClassContext(GlaiCompilationConfiguration::class, wholeClasspath = true)
            compilerOptions("-jvm-target", "1.8")
        }
        refineConfiguration {
            onAnnotations(
                DependsOn::class,
                Repository::class,
                Include::class,
                Import::class,
                CompilerOptions::class,
                handler = GlaiCompilationConfigurationHandler())
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
class GlaiEvaluationConfiguration(val id: String, val props: ScriptRuntimeProperty = ScriptRuntimeProperty()) : ScriptEvaluationConfiguration(
    {
        constructorArgs(id)
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

        val folder = File(getDataFolder(), "scripts")
        val importedSources = annotations.filterByAnnotationType<Include>().flatMap {
            it.annotation.paths.mapNotNull { sourceName -> FileScriptSource(folder.findInclude(sourceName).firstOrNull() ?: return@mapNotNull null) }
        }

        val importedPlugins = annotations.filterByAnnotationType<Import>().flatMap {
            it.annotation.plugins.flatMap { pluginName -> GlaiEnv.loadImportFromPlugin(pluginName) }
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
                if (importedSources.isNotEmpty()) {
                    importScripts.append(importedSources)
                }
                if (importedPlugins.isNotEmpty()) {
                    defaultImports.append(importedPlugins)
                }
                if (compileOptions.isNotEmpty()) {
                    compilerOptions.append(compileOptions)
                }
            }.asSuccess()
        }
    }
}

private fun File.findInclude(script: String): Set<File> {
    if (isDirectory) {
        return listFiles()?.flatMap { it.findScripts() }?.toSet() ?: emptySet()
    }
    if (name == script || (nameWithoutExtension == script && extension == "kts")) {
        return setOf(this)
    }
    return emptySet()
}

/**
 * 获取目录下所有有效脚本文件
 */
fun File.findScripts(): Set<File> {
    if (name.startsWith('.')) {
        return emptySet()
    }
    if (isDirectory) {
        return listFiles()?.flatMap { it.findScripts() }?.toSet() ?: emptySet()
    }
    if (extension == "kts" || extension == "kit") {
        return setOf(this)
    }
    return emptySet()
}