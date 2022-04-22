package ink.ptms.glaikit

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import taboolib.common.io.newFile
import taboolib.common.io.taboolibId
import taboolib.common.platform.function.*
import taboolib.common.reflect.Reflex.Companion.getProperty
import taboolib.common.util.join
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

object GlaiEnv {

    val pluginImports = ConcurrentHashMap<String, List<String>>()

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
        val separator = if (System.getProperty("os.name").lowercase().contains("windows")) ";" else ":"
        System.setProperty("kotlin.script.classpath", classpath.joinToString(separator) { it.path })
    }

    val globalImports = ArrayList<String>()

    /**
     * 加载脚本中所需的 import 列表
     */
    fun setupGlobalImports() {
        pluginImports.clear()
        globalImports.clear()
        globalImports.addAll(loadImportsFromFile(releaseResourceFile("default.imports")))
        newFile(getDataFolder(), "out/default.imports").writeText(globalImports.joinToString("\n"), StandardCharsets.UTF_8)
        globalImports.addAll(loadFunctionsFromFile(releaseResourceFile("default.functions")))
    }

    fun loadImportsFromFile(file: File, classLoader: ClassLoader? = null): List<String> {
        return loadImportsFromString(file.readLines(StandardCharsets.UTF_8), classLoader)
    }

    fun loadImportsFromString(str: List<String>, classLoader: ClassLoader? = null): List<String> {
        val scanner = FastClasspathScanner(*str.filter { it.isNotBlank() }.toTypedArray())
        if (classLoader != null) {
            scanner.addClassLoader(classLoader)
        }
        val classes = scanner.alwaysScanClasspathElementRoot(false).scan().namesOfAllClasses
        return classes.map { it.substringBeforeLast(".") }.filter { it.isNotEmpty() }.toSet().map { "$it.*" }
    }

    /**
     * 从插件中加载
     * 可传入插件名称或特定包名
     */
    fun loadImportFromPlugin(name: String): List<String> {
        if (pluginImports.contains(name)) {
            return pluginImports[name]!!
        }
        val args = name.split(":")
        val plugin = Bukkit.getPluginManager().getPlugin(args[0]) ?: return emptyList()
        // 默认添加插件主类所在的包
        // 如果是 TabooLib 则进行特殊兼容
        val javaName = plugin.javaClass.name
        val main = if (javaName.endsWith("$taboolibId.platform.BukkitPlugin") || javaName.endsWith("$taboolibId.platform.BungeePlugin")) {
            javaName.substringBefore(".$taboolibId")
        } else {
            javaName.substringBeforeLast('.')
        }
        // 插入用户片段
        val extra = if (args.size > 1) args[1].split(",").toTypedArray() else emptyArray()
        val imports = loadImportsFromString(listOf("!!", main, *extra, "-$main.$taboolibId"), plugin.javaClass.classLoader)
        if (imports.isNotEmpty()) {
            newFile(getDataFolder(), "out/$name.imports").writeText(imports.joinToString("\n"), StandardCharsets.UTF_8)
            pluginImports[name] = imports
        }
        return imports
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