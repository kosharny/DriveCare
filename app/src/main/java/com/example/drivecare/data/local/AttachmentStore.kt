package com.example.drivecare.data.local

import android.content.Context
import android.net.Uri
import com.example.drivecare.core.model.newId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AttachmentStore @Inject constructor(@ApplicationContext private val context: Context) {

    val directory: File =
        File(context.filesDir, "attachments").apply {
            mkdirs()
        }

    fun file(path: String): File {
        require(path.matches(Regex("[a-zA-Z0-9-]+\\.[a-z0-9]+")))
        return File(directory, path)
    }

    suspend fun copy(uri: Uri, imageOnly: Boolean): Pair<String, String> =
        withContext(Dispatchers.IO) {
            val type = context.contentResolver.getType(uri) ?: error("unsupported")
            require(type in setOf("image/jpeg", "image/png", "image/webp", "application/pdf"))
            require(!imageOnly || type.startsWith("image/"))
            val extension =
                when (type) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "pdf"
                }
            val name = "${newId()}.$extension"
            val destination = file(name)
            try {
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 15 * 1024 * 1024)
                            output.write(buffer, 0, count)
                        }
                        require(total > 0)
                    }
                }
                name to type
            } catch (failure: Exception) {
                destination.delete()
                throw failure
            }
        }

    suspend fun cleanup(references: Set<String>, immediately: Boolean = false) =
        withContext(Dispatchers.IO) {
            directory.listFiles()?.forEach {
                if (
                    it.name !in references &&
                    (immediately || it.lastModified() < System.currentTimeMillis() - 86_400_000)
                ) {
                    it.delete()
                }
            }
        }
}
