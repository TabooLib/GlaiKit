package ink.ptms.glaikit.scripting

import ink.ptms.glaikit.kts.GlaiScriptManager
import java.io.Closeable
import kotlin.collections.ArrayList

/**
 * GlaiKit
 * ink.ptms.glaikit.scripting.ScriptContainer
 *
 * @author 坏黑
 * @since 2022/4/17 02:35
 */
class ScriptContainer(val script: Script) {

    val baseId = script.baseId
    val resources = ArrayList<Closeable>()

    var isEnabled = true
        private set

    fun addResource(resource: () -> Unit) {
        resources += Closeable { resource() }
    }

    fun release() {
        if (isEnabled) {
            isEnabled = false
            try {
                resources.forEach { it.close() }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            GlaiScriptManager.unregisterScriptContainer(this)
        }
    }
}