package pt.sirlim.app.ui.screens.user.cleaning

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
    weeklyInstructions: String? = null,
    onStart: (String, String?) -> Unit, 
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
                title = { Text("Inicio de Limpeza", color = Color.White, fontWeight = FontWeight.Bold) },
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
                    if (existingIndication != null && indicationId == null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF176).copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = SirlimDarkBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Tem uma indicação diária atribuída para este compartimento hoje.",
                                    color = SirlimDarkBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            if (!groupName.isNullOrBlank()) {
                                Text(text = groupName!!.uppercase(), color = SirlimTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            Text(text = compartment!!.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            compartment!!.description?.let {
                                if (it.isNotBlank()) {
                                    Text(text = it, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            lastCleaning?.let { info ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, null, tint = SirlimTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = info, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (existingIndication?.instructions?.isNotBlank() == true || weeklyInstructions?.isNotBlank() == true) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = SirlimTeal.copy(alpha = 0.1f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SirlimTeal.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "INSTRUÇÕES ADICIONAIS:",
                                    color = SirlimTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = (existingIndication?.instructions ?: weeklyInstructions)!!,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    Text(
                        text = if (indicationId != null || existingIndication != null) "TAREFAS DA INDICAÇÃO:" else "TAREFAS PADRÃO:",
                        color = SirlimTeal, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(tasks) { task ->
                            Card(
                                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = "• ${task.name}", modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botão INICIAR Premium com Glow
                    val infiniteTransition = rememberInfiniteTransition(label = "btnGlow")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
                        label = "alpha"
                    )

                    Button(
                        onClick = { onStart(compartment!!.id!!, indicationId ?: existingIndication?.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(12.dp * alpha, RoundedCornerShape(16.dp), spotColor = SirlimTeal),
                        colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Moving inner glow
                            val glowOffset by infiniteTransition.animateFloat(
                                initialValue = -50f,
                                targetValue = 50f,
                                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
                                label = "glowOffset"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                                            center = Offset(100f + glowOffset, 50f),
                                            radius = 300f
                                        )
                                    )
                            )
                            
                            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.PlayArrow, null, tint = SirlimBlue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("INICIAR LIMPEZA", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SirlimBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}
