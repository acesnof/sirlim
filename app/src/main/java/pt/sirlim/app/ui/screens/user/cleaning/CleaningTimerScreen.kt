package pt.sirlim.app.ui.screens.user.cleaning

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val tasks by viewModel.tasks.collectAsState()
    val seconds by viewModel.secondsElapsed.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val pauseSeconds by viewModel.pauseSeconds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val softYellow = Color(0xFFFFEB3B).copy(alpha = 0.8f)
    val customRed = Color(0xFFFF7D7D) // ALTERADO: Cor exata pedida #ff7d7d
    val softTeal = SirlimTeal.copy(alpha = 0.8f)

    var showFinishForm by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var observations by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(compId) {
        viewModel.loadCompartmentById(compId, indicationId)
        viewModel.startCleaning()
    }

    LaunchedEffect(tasks) {
        if (tasks.isNotEmpty() && selectedTaskIds.isEmpty()) {
            selectedTaskIds = tasks.mapNotNull { it.id }.toSet()
        }
    }

    val timeFormatted = formatSeconds(seconds)
    val pauseFormatted = formatSeconds(pauseSeconds)

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Limpeza") },
            text = { Text("Tem a certeza que deseja APAGAR esta limpeza? Esta ação é irreversível.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopTimer()
                    onBack()
                }) {
                    Text("SIM, APAGAR", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("NÃO")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(compartment?.name ?: "Limpeza em Curso", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancelar", tint = Color.White)
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
            if (!showFinishForm) {
                // Timer View
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Início: ${startTime?.withZoneSameInstant(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "--:--:--"}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = timeFormatted,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isPaused) softYellow else Color.White,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )

                    if (isPaused) {
                        Text(text = "EM PAUSA", color = softYellow, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(60.dp))

                    Button(
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) softTeal else softYellow
                        )
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pausar",
                            tint = SirlimDarkBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { 
                            viewModel.stopTimer()
                            showFinishForm = true 
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(60.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = customRed, // ALTERADO: #ff7d7d
                            contentColor = Color.White // ALTERADO: Letra branca
                        )
                    ) {
                        Icon(Icons.Default.Stop, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TERMINAR LIMPEZA", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Summary/Finish Form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text("Resumo da Limpeza", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Tempo Total", color = SirlimTeal, fontSize = 13.sp)
                                Text(timeFormatted, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.2f)))
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Tempo Pausa", color = softYellow, fontSize = 13.sp)
                                Text(pauseFormatted, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Tarefas Realizadas:", color = Color.White, fontWeight = FontWeight.Bold)
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(tasks) { task ->
                            val isChecked = selectedTaskIds.contains(task.id)
                            Surface(
                                onClick = {
                                    selectedTaskIds = if (isChecked) selectedTaskIds - task.id!! else selectedTaskIds + task.id!!
                                },
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = SirlimTeal)
                                    )
                                    Text(text = task.name, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        label = { Text("Observações", color = Color.White.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SirlimTeal
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.saveCleaning(userId, selectedTaskIds.toList(), observations) { success ->
                                if (success) {
                                    Toast.makeText(context, "Limpeza guardada com sucesso!", Toast.LENGTH_LONG).show()
                                    onFinish()
                                } else {
                                    Toast.makeText(context, "Erro ao guardar: ${viewModel.error.value}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = SirlimBlue)
                        else {
                            Icon(Icons.Default.Check, null, tint = SirlimBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GUARDAR REGISTO", fontWeight = FontWeight.Black, color = SirlimBlue)
                        }
                    }

                    TextButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
                    ) {
                        Text("CANCELAR E APAGAR", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

fun formatSeconds(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}
