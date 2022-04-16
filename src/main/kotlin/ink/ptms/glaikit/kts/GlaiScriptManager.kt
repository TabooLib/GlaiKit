package ink.ptms.glaikit.kts

import ink.ptms.glaikit.scripting.Script
import ink.ptms.glaikit.scripting.ScriptContainer
import java.util.concurrent.ConcurrentHashMap

/**
 * GlaiKit
 * ink.ptms.glaikit.kts.GlaiScriptManager
 *
 * @author 坏黑
 * @since 2021/12/28 2:37 AM
 */
object GlaiScriptManager {

    internal val activeScriptContainers = ConcurrentHashMap<String, ScriptContainer>()

    /**
     * 注册脚本容器
     */
    fun registerScriptContainer(id: String, script: Script): ScriptContainer {
        return ScriptContainer(script).also { activeScriptContainers[id] = it }
    }

    /**
     * 注销脚本容器
     */
    fun unregisterScriptContainer(script: ScriptContainer) {
        activeScriptContainers.remove(script.baseId)
    }

    /**
     * 获取脚本容器
     */
    fun getScriptContainer(id: String): ScriptContainer? {
        return activeScriptContainers[id]
    }

    /**
     * 获取所有脚本容器
     */
    fun getScriptContainers(): List<ScriptContainer> {
        return activeScriptContainers.values.toList()
    }
}