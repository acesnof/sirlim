package pt.sirlim.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import pt.sirlim.app.data.model.*
import pt.sirlim.app.ui.screens.user.consultations.formatSeconds
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ExcelUtils {
    fun exportCleaningsToExcel(
        context: Context,
        cleanings: List<Cleaning>,
        compartments: Map<String, Compartment>,
        groups: Map<String, Group>,
        users: Map<String, User>,
        fileName: String = "Relatorio_Limpezas.xlsx"
    ): Boolean {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Limpezas")

            // Header
            val headerRow = sheet.createRow(0)
            val columns = listOf("Data", "Hora Início", "Hora Fim", "Grupo", "Compartimento", "Utilizador", "Tempo Pausa", "Observações")
            columns.forEachIndexed { index, col ->
                headerRow.createCell(index).setCellValue(col)
            }

            // Data
            cleanings.forEachIndexed { index, cleaning ->
                val row = sheet.createRow(index + 1)
                val start = ZonedDateTime.parse(cleaning.startTime).withZoneSameInstant(ZoneId.systemDefault())
                val end = cleaning.endTime?.let { ZonedDateTime.parse(it).withZoneSameInstant(ZoneId.systemDefault()) }
                
                val comp = compartments[cleaning.compartmentId]
                val group = groups[comp?.groupId ?: ""]
                val user = users[cleaning.userId]

                row.createCell(0).setCellValue(start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                row.createCell(1).setCellValue(start.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                row.createCell(2).setCellValue(end?.format(DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "--")
                row.createCell(3).setCellValue(group?.name ?: "Sem Grupo")
                row.createCell(4).setCellValue(comp?.name ?: "N/A")
                row.createCell(5).setCellValue(user?.fullName ?: user?.username ?: "N/A")
                row.createCell(6).setCellValue(formatSeconds(cleaning.pauseDurationSeconds))
                row.createCell(7).setCellValue(cleaning.observations ?: "")
            }

            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, fileName)
            val fos = FileOutputStream(file)
            workbook.write(fos)
            fos.close()
            workbook.close()

            // Share
            val contentUri: Uri = FileProvider.getUriForFile(context, "pt.sirlim.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Enviar Relatório Excel"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
