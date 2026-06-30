package pt.sirlim.app.ui.screens.user.cleaning

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningTimerScreen(
    userId: String,
    compId: String,
    indicationId: String? = null,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: CleaningViewModel = viewModel()
    val compartment by viewModel.compartment.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val seconds by viewModel.secondsElapsed.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val pauseSeconds by viewModel.pauseSeconds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val customRed = Color(0xFFFF7D7D)

    var showFinishForm by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var observations by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(compId) {
        viewModel.loadCompartmentById(compId, indicationId)
        viewModel.startCleaning()
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Limpeza") },
            text = { Text("Deseja mesmo cancelar? O tempo decorrido será perdido.") },
            confirmButton = {
                TextButton(onClick = { onBack() }) { Text("SIM, CANCELAR", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("VOLTAR") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Limpeza em Curso", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Cristalino
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!groupName.isNullOrBlank()) {
                        Text(
                            text = groupName!!.uppercase(), 
                            fontSize = 11.sp, 
                            color = SirlimTeal, 
                            fontWeight = FontWeight.Bold, 
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(text = compartment?.name ?: "A carregar...", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    
                    indicationId?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .background(SirlimTeal.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("INDICAÇÃO DIÁRIA", color = SirlimTeal, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // CRONOMETRO PREMIUM REDUZIDO
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                        )
                    )
                    .border(2.dp, if (isPaused) Color.Yellow.copy(alpha = 0.4f) else SirlimTeal.copy(alpha = 0.4f), CircleShape)
                    .shadow(10.dp, CircleShape, spotColor = if (isPaused) Color.Yellow else SirlimTeal),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isPaused) "PAUSADO" else "LIMPANDO",
                        color = if (isPaused) Color.Yellow else SirlimTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = formatTimer(seconds),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    if (pauseSeconds > 0) {
                        Text(
                            text = "Pausa: ${formatTimer(pauseSeconds)}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // BOTOES PAUSA E TERMINAR PREMIUM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botão Pausa
                Button(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) SirlimTeal else Color.White.copy(alpha = 0.15f)
                    ),
                    border = if (!isPaused) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, 
                        null, 
                        tint = if (isPaused) SirlimBlue else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isPaused) "RETOMAR" else "PAUSAR", 
                        fontWeight = FontWeight.ExtraBold, 
                        color = if (isPaused) SirlimBlue else Color.White,
                        fontSize = 14.sp
                    )
                }

                // Botão Terminar
                Button(
                    onClick = {
                        viewModel.stopTimer()
                        showFinishForm = true
                    },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = customRed),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("TERMINAR", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Surface(
                color = Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Iniciada às: ${startTime?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (showFinishForm) {
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Finalizar Registo", fontSize = 26.sp, fontWeight = FontWeight.Black, color = SirlimDarkBlue)
                    Text(text = "Confirme as tarefas realizadas", fontSize = 14.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(tasks) { task ->
                            val isSelected = selectedTaskIds.contains(task.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedTaskIds = if (isSelected) selectedTaskIds - task.id!! else selectedTaskIds + task.id!!
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) SirlimTeal.copy(alpha = 0.1f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = SirlimTeal)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = task.name, color = SirlimDarkBlue, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        label = { Text("Observações (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = SirlimTeal)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { 
                                    showFinishForm = false
                                    viewModel.resumeTimer()
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("VOLTAR", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    viewModel.saveCleaning(userId, selectedTaskIds.toList(), observations) { success ->
                                        if (success) onFinish()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal)
                            ) {
                                Text("GUARDAR", color = SirlimBlue, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTimer(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    else String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
