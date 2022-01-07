package ink.ptms.glaikit.scripting

import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object ScriptIO {

    fun newFile(file: File, path: String, create: Boolean = true, folder: Boolean = false): File {
        return newFile(File(file, path), create, folder)
    }

    fun newFile(path: String, create: Boolean = true, folder: Boolean = false): File {
        return newFile(File(path), create, folder)
    }

    fun newFile(file: File, create: Boolean = true, folder: Boolean = false): File {
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (!file.exists() && create) {
            if (folder) {
                file.mkdirs()
            } else {
                file.createNewFile()
            }
        }
        return file
    }

    fun File.deepDelete() {
        if (exists()) {
            if (isDirectory) {
                listFiles()?.forEach { it.deepDelete() }
            }
            delete()
        }
    }

    fun File.deepCopyTo(target: File) {
        if (isDirectory) {
            listFiles()?.forEach { it.deepCopyTo(File(target, it.name)) }
        } else {
            copyTo(target)
        }
    }

    fun String.digest(algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        digest.update(toByteArray(StandardCharsets.UTF_8))
        return BigInteger(1, digest.digest()).toString(16)
    }

    fun File.digest(algorithm: String): String {
        return FileInputStream(this).use {
            val digest = MessageDigest.getInstance(algorithm)
            val buffer = ByteArray(1024)
            var length: Int
            while (it.read(buffer, 0, 1024).also { i -> length = i } != -1) {
                digest.update(buffer, 0, length)
            }
            BigInteger(1, digest.digest()).toString(16)
        }
    }
}