package ink.ptms.glaikit

import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Plugin

@RuntimeDependencies(
    RuntimeDependency("org.jetbrains.intellij.deps:trove4j:1.0.20181211"),
    RuntimeDependency("org.jetbrains.kotlin:kotlin-reflect:1.5.10"),
    RuntimeDependency("org.jetbrains.kotlin:kotlin-main-kts:1.5.10"),
    RuntimeDependency("org.jetbrains.kotlin:kotlin-scripting-common:1.5.10"),
    RuntimeDependency("org.jetbrains.kotlin:kotlin-scripting-jvm:1.5.10"),
    RuntimeDependency("org.jetbrains.kotlin:kotlin-scripting-jvm-host:1.5.10"),
    RuntimeDependency("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:1.5.10"),
    RuntimeDependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8"),
)
object GlaiKit : Plugin() {

    override fun onEnable() {
        GlaiEnv.setupClasspath()
        GlaiEnv.setupGlobalImports()
        try {
            GlaiEvaluator.setupScriptFiles()
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }
}