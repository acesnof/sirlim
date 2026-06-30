package pt.sirlim.app.ui.screens.admin.groups_compartments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import pt.sirlim.app.data.model.Compartment
import pt.sirlim.app.data.model.Group
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsCompartmentsScreen(
    initialTab: Int = 0,
    onAddCompartment: () -> Unit,
    onEditCompartment: (Compartment) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: GroupsCompartmentsViewModel = viewModel()
    val groups by viewModel.groups.collectAsState()
    val compartments by viewModel.compartments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showQrDialog by remember { mutableStateOf<Compartment?>(null) }
    var showGroupDialog by remember { mutableStateOf<Group?>(null) }
    var isAddingGroup by remember { mutableStateOf(false) }
    
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedGroupsForExport by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // FILTRO DE ATIVOS
    var showAllCompartments by remember { mutableStateOf(false) }

    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var compartmentToDelete by remember { mutableStateOf<Compartment?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Apagar Grupo") },
            text = { Text("Tem a certeza que deseja apagar o grupo '${groupToDelete?.name}'? Só será possível se o grupo estiver vazio.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(groupToDelete!!) { success ->
                        if (success) {
                            Toast.makeText(context, "Grupo apagado!", Toast.LENGTH_SHORT).show()
                            groupToDelete = null
                        } else {
                            Toast.makeText(context, error ?: "Erro ao apagar", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("APAGAR", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { groupToDelete = null }) { Text("CANCELAR") } }
        )
    }

    if (compartmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { compartmentToDelete = null },
            title = { Text("Apagar Compartimento") },
            text = { Text("Deseja apagar '${compartmentToDelete?.name}'? Só será possível se não existirem limpezas associadas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCompartment(compartmentToDelete!!) { success ->
                        if (success) {
                            Toast.makeText(context, "Compartimento apagado!", Toast.LENGTH_SHORT).show()
                            compartmentToDelete = null
                        } else {
                            Toast.makeText(context, error ?: "Erro ao apagar", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("APAGAR", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { compartmentToDelete = null }) { Text("CANCELAR") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupos e Compartimentos", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
            )
        },
        floatingActionButton = {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (selectedTab == 1) {
                    // BOTÃO MOSTRAR TODOS / SÓ ATIVOS
                    FloatingActionButton(
                        onClick = { showAllCompartments = !showAllCompartments },
                        containerColor = if (showAllCompartments) SirlimTeal else Color.White,
                        contentColor = SirlimBlue
                    ) {
                        Icon(if (showAllCompartments) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Filtrar")
                    }

                    FloatingActionButton(
                        onClick = { showExportDialog = true },
                        containerColor = Color.White,
                        contentColor = SirlimBlue
                    ) {
                        if (isGeneratingPdf) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SirlimBlue)
                        else Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDFs")
                    }
                }
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) isAddingGroup = true
                        else onAddCompartment()
                    },
                    containerColor = SirlimTeal,
                    contentColor = SirlimBlue
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SirlimTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SirlimTeal
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Grupos", color = Color.White) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Compartimentos", color = Color.White) })
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && !isGeneratingPdf) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SirlimTeal)
                } else {
                    if (selectedTab == 0) {
                        GroupsList(groups = groups, onEdit = { showGroupDialog = it }, onDelete = { groupToDelete = it })
                    } else {
                        // APLICAR FILTRO DE ATIVOS
                        val filteredList = if (showAllCompartments) compartments else compartments.filter { it.isActive }
                        
                        CompartmentsList(
                            compartments = filteredList,
                            groups = groups,
                            onEdit = onEditCompartment,
                            onDelete = { compartmentToDelete = it },
                            onShowQr = { showQrDialog = it }
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportar QR Codes") },
            text = {
                Column {
                    Text("Selecione os grupos para exportar:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            selectedGroupsForExport = if (selectedGroupsForExport.size == groups.size) emptySet() else groups.mapNotNull { it.id }.toSet()
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal.copy(alpha = 0.2f), contentColor = Color(0xFF004D40)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SirlimTeal)
                    ) {
                        Text(if (selectedGroupsForExport.size == groups.size) "Desmarcar Todos" else "Selecionar Todos os Grupos", fontWeight = FontWeight.Bold)
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(groups) { group ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                                selectedGroupsForExport = if (selectedGroupsForExport.contains(group.id)) selectedGroupsForExport - group.id!! else selectedGroupsForExport + group.id!!
                            }.padding(4.dp)) {
                                Checkbox(checked = selectedGroupsForExport.contains(group.id), onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = SirlimBlue))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(group.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showExportDialog = false
                    isGeneratingPdf = true
                    scope.launch {
                        val result = generateQrCodesPdfNative(context, compartments.filter { it.groupId in selectedGroupsForExport }, groups)
                        isGeneratingPdf = false
                        if (result.startsWith("OK")) {
                            Toast.makeText(context, "PDF enviado com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Erro: $result", Toast.LENGTH_LONG).show()
                        }
                    }
                }, enabled = selectedGroupsForExport.isNotEmpty()) {
                    Text("Gerar PDF e Enviar")
                }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("Cancelar") } }
        )
    }

    if (isAddingGroup || showGroupDialog != null) {
        GroupFormDialog(
            group = showGroupDialog,
            onDismiss = { isAddingGroup = false; showGroupDialog = null },
            onSave = { name ->
                viewModel.saveGroup(Group(id = showGroupDialog?.id, name = name)) {
                    isAddingGroup = false
                    showGroupDialog = null
                }
            }
        )
    }

    showQrDialog?.let { comp ->
        val groupName = groups.find { it.id == comp.groupId }?.name ?: "Sem Grupo"
        QrCodeDialog(
            compartment = comp,
            groupName = groupName,
            onDismiss = { showQrDialog = null },
            onShare = { shareQrCode(context, comp, groupName) }
        )
    }
}

@Composable
fun GroupsList(groups: List<Group>, onEdit: (Group) -> Unit, onDelete: (Group) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(groups) { group ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = group.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = SirlimDarkBlue)
                    IconButton(onClick = { onEdit(group) }) { Icon(Icons.Default.Edit, "Editar", tint = SirlimBlue) }
                    IconButton(onClick = { onDelete(group) }) { Icon(Icons.Default.Delete, "Apagar", tint = Color.Red.copy(alpha = 0.6f)) }
                }
            }
        }
    }
}

@Composable
fun CompartmentsList(
    compartments: List<Compartment>,
    groups: List<Group>,
    onEdit: (Compartment) -> Unit,
    onDelete: (Compartment) -> Unit,
    onShowQr: (Compartment) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(compartments) { comp ->
            val groupName = groups.find { it.id == comp.groupId }?.name ?: "Sem Grupo"
            // Design premium para itens inativos
            val cardColor = if (comp.isActive) Color.White.copy(alpha = 0.95f) else Color(0xFFEEEEEE).copy(alpha = 0.95f)
            
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onEdit(comp) }, 
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = comp.name, fontWeight = FontWeight.Bold, color = SirlimDarkBlue)
                            if (!comp.isActive) {
                                Text(
                                    text = " (INATIVO)", 
                                    color = Color(0xFFD32F2F),
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                        Text(text = groupName, fontSize = 12.sp, color = SirlimBlue.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = { onShowQr(comp) }) { 
                        Icon(Icons.Default.QrCode, "QR Code", tint = if (comp.isActive) SirlimBlue else Color.Gray) 
                    }
                    IconButton(onClick = { onDelete(comp) }) { 
                        Icon(Icons.Default.Delete, "Apagar", tint = Color.Red.copy(alpha = 0.6f)) 
                    }
                }
            }
        }
    }
}

@Composable
fun GroupFormDialog(group: Group?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (group == null) "Novo Grupo" else "Editar Grupo") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do Grupo") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun QrCodeDialog(compartment: Compartment, groupName: String, onDismiss: () -> Unit, onShare: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(groupName, fontSize = 12.sp, color = SirlimBlue, fontWeight = FontWeight.Bold)
                Text(compartment.name, fontWeight = FontWeight.Bold, color = SirlimDarkBlue, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                val bitmap = remember(compartment.qrCodeKey) { generateQrCode(compartment.qrCodeKey) }
                bitmap?.let { androidx.compose.foundation.Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(200.dp)) }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Fechar") }
                    Button(onClick = onShare, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal) ) {
                        Icon(Icons.Default.Share, null, tint = SirlimBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enviar", color = SirlimBlue)
                    }
                }
            }
        }
    }
}

fun shareQrCode(context: Context, compartment: Compartment, groupName: String) {
    val qrBitmap = generateQrCode(compartment.qrCodeKey) ?: return
    val finalBitmap = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height + 100, qrBitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 30f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    canvas.drawText("$groupName / ${compartment.name}", finalBitmap.width / 2f, 60f, paint)
    canvas.drawBitmap(qrBitmap, 0f, 100f, null)
    try {
        val cachePath = File(context.cacheDir, "images"); cachePath.mkdirs()
        val file = File(cachePath, "qr_${compartment.qrCodeKey}.png")
        val stream = FileOutputStream(file); finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); stream.close()
        val contentUri: Uri = FileProvider.getUriForFile(context, "pt.sirlim.app.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, contentUri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(intent, "Enviar QR Code"))
    } catch (e: Exception) { e.printStackTrace() }
}

suspend fun generateQrCodesPdfNative(context: Context, compartments: List<Compartment>, groups: List<Group>): String {
    return withContext(Dispatchers.IO) {
        var pdfDocument: PdfDocument? = null
        var fos: FileOutputStream? = null
        try {
            pdfDocument = PdfDocument()
            val paint = Paint().apply { isAntiAlias = true }
            val groupPaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 9f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            val compPaint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 7f; textAlign = Paint.Align.CENTER }
            
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var myPage = pdfDocument.startPage(pageInfo)
            var canvas = myPage.canvas
            
            val qrSize = 100 
            val margin = 40
            val spacing = 35
            var curX = margin
            var curY = margin + 50

            compartments.forEach { comp ->
                if (curY + qrSize + 50 > 842) { 
                    pdfDocument.finishPage(myPage)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    myPage = pdfDocument.startPage(pageInfo)
                    canvas = myPage.canvas
                    curX = margin
                    curY = margin + 50
                }

                val groupName = groups.find { it.id == comp.groupId }?.name ?: "Sem Grupo"
                val centerX = (curX + qrSize / 2).toFloat()
                canvas.drawText(groupName.uppercase(), centerX, (curY - 18).toFloat(), groupPaint)
                canvas.drawText(comp.name, centerX, (curY - 6).toFloat(), compPaint)
                
                val qrBitmap = generateQrCode(comp.qrCodeKey)
                if (qrBitmap != null) {
                    val scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true)
                    canvas.drawBitmap(scaledQr, curX.toFloat(), curY.toFloat(), paint)
                }

                curX += qrSize + spacing
                if (curX + qrSize + margin > 595) { 
                    curX = margin
                    curY += qrSize + 50
                }
            }

            pdfDocument.finishPage(myPage)

            val cachePath = File(context.cacheDir, "exports"); if (!cachePath.exists()) cachePath.mkdirs()
            val file = File(cachePath, "QR_Codes_SIRLIM.pdf")
            fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.flush()

            withContext(Dispatchers.Main) {
                val contentUri: Uri = FileProvider.getUriForFile(context, "pt.sirlim.app.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Enviar PDF"))
            }
            "OK"
        } catch (e: Exception) {
            Log.e("PDF_GEN", "Error: ${e.message}", e)
            e.message ?: "Erro desconhecido"
        } finally {
            pdfDocument?.close()
            fos?.close()
        }
    }
}

fun generateQrCode(text: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width; val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) { for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }}
        bitmap
    } catch (e: Exception) { null }
}
