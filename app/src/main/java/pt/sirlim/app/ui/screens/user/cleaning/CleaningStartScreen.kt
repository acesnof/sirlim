package pt.sirlim.app.ui.screens.user.cleaning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningStartScreen(
    userId: String,
    qrKey: String? = null,
    compId: String? = null,
    indicationId: String? = null,
    onStart: (String, String?) -> Unit, // compId, indId
    onBack: () -> Unit
) {
    val viewModel: CleaningViewModel = viewModel()
    val compartment by viewModel.compartment.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val lastCleaning by viewModel.lastCleaningInfo.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val existingIndication by viewModel.existingIndication.collectAsState()

    LaunchedEffect(qrKey, compId) {
        if (qrKey != null) {
            viewModel.loadCompartmentByQr(qrKey, userId)
        } else if (compId != null) {
            viewModel.loadCompartmentById(compId, indicationId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inicio de Limpeza", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SirlimTeal)
            } else if (error != null) {
                Text(text = error!!, color = Color.Red, modifier = Modifier.align(Alignment.Center).padding(16.dp))
            } else if (compartment != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // ALERTA DE INDICAÇÃO DIÁRIA (Se detetada via QR Scan)
                    if (existingIndication != null && indicationId == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.9f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = SirlimDarkBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Este compartimento já tem uma indicação diária atribuída a si para hoje. A limpeza será associada a essa indicação.",
                                    color = SirlimDarkBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = compartment!!.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(text = groupName ?: "", color = SirlimTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            compartment!!.description?.let {
                                if (it.isNotBlank()) {
                                    Text(text = it, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            lastCleaning?.let { info ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, null, tint = SirlimTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = info,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (indicationId != null || existingIndication != null) "Tarefas da Indicação:" else "Tarefas Padrão:",
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(tasks) { task ->
                            Card(
                                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Text(text = "• ${task.name}", modifier = Modifier.padding(12.dp), color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onStart(compartment!!.id!!, indicationId ?: existingIndication?.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = SirlimBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INICIAR", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SirlimBlue)
                    }
                }
            }
        }
    }
}
