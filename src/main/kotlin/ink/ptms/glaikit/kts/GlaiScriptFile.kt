package ink.ptms.glaikit.kts

import taboolib.common5.Coerce
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import kotlin.script.experimental.api.CompiledScript

/**
 * GlaiKit
 * ink.ptms.glaikit.kts.GlaiScriptFile
 *
 * @author 坏黑
 * @since 2022/4/12 01:57
 */
class GlaiScriptFile(val sourceFile: File, compileArgs: Array<Any>, compiledScript: CompiledScript) : GlaiScript(compiledScript) {

    /**
     * [0]：文件签名
     * [1]：参数签名
     */
    val digest = arrayOf(compileArgs[0].toString(), compileArgs[1].toString())

    /**
     * 参数数量
     */
    val propsSize = Coerce.toInteger(compileArgs[2])

    /**
     * 参数描述
     */
    val propsDescription = compileArgs[3].toString()

    companion object {

        fun loadFromFile(sourceFile: File): GlaiScriptFile {
            ByteArrayInputStream(sourceFile.readBytes()).use { byteArrayInputStream ->
                ObjectInputStream(byteArrayInputStream).use { objectInputStream ->
                    when (val version = objectInputStream.readInt()) {
                        GlaiCompiler.SERIALIZE_VERSION -> {
                            val size = objectInputStream.readInt() // 4
                            val compileArgs = (1..size).map { objectInputStream.readObject() as Any }.toTypedArray()
                            val compiledScript = objectInputStream.readObject() as CompiledScript
                            return GlaiScriptFile(sourceFile, compileArgs, compiledScript)
                        } else -> {
                            error("Unsupported version $version")
                        }
                    }
                }
            }
        }
    }
}
