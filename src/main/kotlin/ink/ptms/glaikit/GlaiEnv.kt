package ink.ptms.glaikit

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File
import java.nio.charset.StandardCharsets

object GlaiEnv {

    /**
     * classpath 中缺少依赖文件将会产生 Unresolved reference 错误
     */
    fun setupClasspath() {
        // 加载依赖文件
        releaseResourceFile("require/bukkit.jar")
        releaseResourceFile("require/kotlin-main-kts.jar")
        releaseResourceFile("require/kotlin-script-util.jar")
        // 加载 classpath
        val classpath = ArrayList<File>()
        classpath += File(getDataFolder(), "require").listFiles()!!.filter { it.extension == "jar" }
        classpath += File("plugins").listFiles()!!.filter { it.extension == "jar" }
        System.setProperty("kotlin.script.classpath", classpath.joinToString(":") { it.canonicalPath })
    }

    val globalImports = ArrayList<String>()

    /**
     * 加载脚本中所需的 import 列表
     */
    fun setupGlobalImports() {
        globalImports.clear()
        globalImports.addAll(loadImportsFromFile(releaseResourceFile("default.imports")))
        newFile(getDataFolder(), "out.imports").writeText(globalImports.joinToString("\n"), StandardCharsets.UTF_8)
        globalImports.addAll(loadFunctionsFromFile(releaseResourceFile("default.functions")))
    }

    fun loadImportsFromFile(file: File): List<String> {
        return FastClasspathScanner(*file.readLines().filter { it.isNotBlank() }.toTypedArray())
            .alwaysScanClasspathElementRoot(false)
            .scan()
            .namesOfAllClasses
            .map { it.substringBeforeLast(".") }
            .filter { it.isNotEmpty() }
            .toSet()
            .map { "$it.*" }
    }

    /**
     * Kotlin 中的顶层函数需要单独引入，不能使用 FastClasspathScanner 扫描
     *
     * 同时还需要保留 kotlin_module 文件
     * 该文件中记录了顶层函数的所有信息
     */
    fun loadFunctionsFromFile(file: File): List<String> {
        return file.readLines(StandardCharsets.UTF_8).filter { it.isNotBlank() }
    }
}