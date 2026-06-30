package pt.sirlim.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pt.sirlim.app.data.model.SirlimBackup
import java.io.File
import java.io.FileOutputStream

object JsonExportUtils {
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true 
    }

    fun exportFullBackup(context: Context, backup: SirlimBackup): Boolean {
        return try {
            val jsonString = json.encodeToString(backup)
            
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, "SIRLIM_Backup_Geral.json")
            val fos = FileOutputStream(file)
            fos.write(jsonString.toByteArray())
            fos.close()

            // Share
            val contentUri: Uri = FileProvider.getUriForFile(context, "pt.sirlim.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Enviar Backup Geral (JSON)"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
