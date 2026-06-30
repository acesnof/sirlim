package pt.sirlim.app.ui.screens.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.data.model.Cleaning
import pt.sirlim.app.ui.screens.admin.consultations.AdminConsultationsViewModel
import pt.sirlim.app.ui.screens.user.consultations.CalendarHeader
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.ZoneId
import pt.sirlim.app.utils.ExcelUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val viewModel: AdminConsultationsViewModel = viewModel()
    val cleanings by viewModel.cleanings.collectAsState()
    val compartments by viewModel.compartments.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val users by viewModel.users.collectAsState()
    val androidContext = LocalContext.current

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDates by remember { mutableStateOf(setOf<LocalDate>()) }

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatórios", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Selecione os dias para o relatório:",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            CalendarHeader(currentMonth = currentMonth, onMonthChange = { currentMonth = it })

            MultiSelectCalendarGrid(
                currentMonth = currentMonth,
                selectedDates = selectedDates,
                cleanings = cleanings,
                onDateToggle = { date ->
                    selectedDates = if (selectedDates.contains(date)) selectedDates - date else selectedDates + date
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val filteredList = cleanings.filter {
                        try {
                            val date = ZonedDateTime.parse(it.startTime).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
                            selectedDates.contains(date)
                        } catch (e: Exception) { false }
                    }
                    ExcelUtils.exportCleaningsToExcel(
                        context = androidContext,
                        cleanings = filteredList,
                        compartments = compartments,
                        groups = groups,
                        users = users,
                        fileName = "Relatorio_Selecionados.xlsx"
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = selectedDates.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal)
            ) {
                Icon(Icons.Default.Download, null, tint = SirlimBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPORTAR DIAS SELECIONADOS", color = SirlimBlue, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Exportação Geral (JSON)
            OutlinedButton(
                onClick = {
                    viewModel.generateFullBackup { backup ->
                        if (backup != null) {
                            pt.sirlim.app.utils.JsonExportUtils.exportFullBackup(androidContext, backup)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Storage, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPORTAÇÃO GERAL (JSON - TODOS OS DADOS)")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MultiSelectCalendarGrid(
    currentMonth: YearMonth,
    selectedDates: Set<LocalDate>,
    cleanings: List<Cleaning>,
    onDateToggle: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val offset = currentMonth.atDay(1).dayOfWeek.value - 1
    val borderColor = SirlimTeal.copy(alpha = 0.2f)

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val totalCells = ((daysInMonth + offset + 6) / 7) * 7
        for (row in 0 until (totalCells / 7)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - offset + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).border(0.5.dp, borderColor), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNum)
                            val isSelected = selectedDates.contains(date)
                            val hasCleanings = cleanings.any {
                                try {
                                    ZonedDateTime.parse(it.startTime).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate() == date
                                } catch (e: Exception) { false }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(if (isSelected) Modifier.border(1.5.dp, SirlimTeal, RoundedCornerShape(8.dp)) else Modifier)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (hasCleanings) Color(0xFFA5D6A7).copy(alpha = 0.6f) else Color.Transparent)
                                    .clickable { onDateToggle(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = dayNum.toString(), color = Color.White, fontWeight = if (hasCleanings || isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}
