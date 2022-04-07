package ink.ptms.glaikit

import ink.ptms.glaikit.scripting.ScriptBase
import org.jetbrains.kotlin.mainKts.CompilerOptions
import org.jetbrains.kotlin.mainKts.Import
import org.jetbrains.kotlin.mainKts.MainKtsConfigurator
import taboolib.common.reflect.Reflex
import taboolib.common.reflect.Reflex.Companion.getProperty
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.jvm.*

@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = GlaiKotlinScriptConfiguration::class,
    evaluationConfiguration = GlaiScriptEvaluationConfiguration::class
)
class GlaiKotlinScript(val args: Array<String>)

class GlaiKotlinScriptConfiguration : ScriptCompilationConfiguration(
    {
        baseClass(ScriptBase::class)
        defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class)
        defaultImports.append(GlaiEnv.globalImports)
        jvm {
            dependenciesFromClassContext(GlaiKotlinScriptConfiguration::class, wholeClasspath = true)
            compilerOptions("-jvm-target", "1.8")
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, Import::class, CompilerOptions::class, handler = MainKtsConfigurator())
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    }
)

class GlaiScriptEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        scriptsInstancesSharing(true)
        jvm {
            baseClassLoader(GlaiKotlinScriptConfiguration::class.java.classLoader)
            loadDependencies(false)
        }
    }
)