package pt.sirlim.app.ui.screens.viewer

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.data.model.*
import pt.sirlim.app.ui.screens.admin.consultations.*
import pt.sirlim.app.ui.screens.user.consultations.CalendarHeader
import pt.sirlim.app.ui.screens.user.consultations.formatSeconds
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import pt.sirlim.app.utils.ExcelUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerConsultationsScreen(
    onBack: () -> Unit
) {
    val viewModel: AdminConsultationsViewModel = viewModel()
    val cleanings by viewModel.cleanings.collectAsState()
    val compartments by viewModel.compartments.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val androidContext = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } 
    var selectedCleaning by remember { mutableStateOf<Cleaning?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consultas Gerais", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        ExcelUtils.exportCleaningsToExcel(
                            context = androidContext,
                            cleanings = cleanings,
                            compartments = compartments,
                            groups = groups,
                            users = users,
                            fileName = "Consultas_Leitura.xlsx"
                        )
                    }) {
                        Icon(Icons.Default.Download, "Exportar", tint = Color.White)
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Calendário", color = Color.White, fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Compartimento", color = Color.White, fontSize = 12.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Utilizador", color = Color.White, fontSize = 12.sp) })
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> CalendarConsultationView(cleanings, compartments, groups, users, onItemClick = { selectedCleaning = it })
                    1 -> CompartmentConsultationView(cleanings, compartments, groups, users, onItemClick = { selectedCleaning = it })
                    2 -> UserConsultationView(cleanings, compartments, groups, users, onItemClick = { selectedCleaning = it })
                }
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SirlimTeal)
                }
            }
        }
    }

    selectedCleaning?.let { cleaning ->
        ViewerCleaningDetailDialog(
            cleaning = cleaning,
            compartment = compartments[cleaning.compartmentId],
            groupName = groups[compartments[cleaning.compartmentId]?.groupId ?: ""]?.name,
            userName = users[cleaning.userId]?.fullName ?: users[cleaning.userId]?.username ?: "Desconhecido",
            onDismiss = { selectedCleaning = null },
            getTasks = { viewModel.getPerformedTasks(cleaning.id!!) }
        )
    }
}

@Composable
fun ViewerCleaningDetailDialog(
    cleaning: Cleaning,
    compartment: Compartment?,
    groupName: String?,
    userName: String,
    onDismiss: () -> Unit,
    getTasks: suspend () -> List<Task>
) {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoadingTasks by remember { mutableStateOf(true) }

    LaunchedEffect(cleaning.id) {
        tasks = getTasks()
        isLoadingTasks = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    if (!groupName.isNullOrBlank()) Text(text = groupName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SirlimTeal)
                    Text(text = compartment?.name ?: "Detalhes", fontWeight = FontWeight.Black, fontSize = 22.sp, color = SirlimDarkBlue)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "EXECUTADO POR: $userName", fontSize = 11.sp, color = SirlimBlue, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))
                pt.sirlim.app.ui.screens.user.consultations.DetailRow("Início", pt.sirlim.app.ui.screens.user.consultations.formatTime(cleaning.startTime))
                pt.sirlim.app.ui.screens.user.consultations.DetailRow("Fim", cleaning.endTime?.let { pt.sirlim.app.ui.screens.user.consultations.formatTime(it) } ?: "--:--")
                pt.sirlim.app.ui.screens.user.consultations.DetailRow("Pausa", formatSeconds(cleaning.pauseDurationSeconds))
                Spacer(modifier = Modifier.height(20.dp))
                Text("TAREFAS REALIZADAS:", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SirlimBlue, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoadingTasks) CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = SirlimTeal)
                else if (tasks.isEmpty()) Text("Nenhuma tarefa registada.", fontSize = 12.sp, color = Color.Gray)
                else tasks.forEach { task -> Text("• ${task.name}", fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 3.dp), color = Color.DarkGray) }
                if (!cleaning.observations.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("OBSERVAÇÕES:", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SirlimBlue, letterSpacing = 1.sp)
                    Text(text = cleaning.observations, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.height(28.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = SirlimBlue), shape = RoundedCornerShape(12.dp)) { Text("FECHAR", fontWeight = FontWeight.Black) }
            }
        }
    }
}
