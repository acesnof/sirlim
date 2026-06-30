package pt.sirlim.app.ui.screens.user.consultations

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.data.model.*
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserConsultationsScreen(
    userId: String,
    onBack: () -> Unit
) {
    val viewModel: ConsultationsViewModel = viewModel()
    val cleanings by viewModel.cleanings.collectAsState()
    val compartments by viewModel.compartments.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var isCalendarVisible by remember { mutableStateOf(true) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCleaning by remember { mutableStateOf<Cleaning?>(null) }
    
    // Filters
    var searchTxt by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var showGroupFilter by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.fetchUserHistory(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("As Minhas Limpezas", color = Color.White, fontWeight = FontWeight.Bold) },
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
        ) {
            AnimatedVisibility(visible = isCalendarVisible, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    CalendarHeader(currentMonth = currentMonth, onMonthChange = { currentMonth = it })
                    CalendarGrid(currentMonth = currentMonth, selectedDate = selectedDate, cleanings = cleanings, onDateSelected = { selectedDate = it })
                }
            }

            // Header with toggle and filters
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Limpezas em ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        IconButton(onClick = { isCalendarVisible = !isCalendarVisible }) {
                            Icon(
                                if (isCalendarVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Calendário",
                                tint = SirlimTeal
                            )
                        }
                    }

                    if (!isCalendarVisible) {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Grupo Filter
                            Box(modifier = Modifier.weight(1.2f)) {
                                OutlinedButton(
                                    onClick = { showGroupFilter = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, SirlimTeal.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val name = groups[selectedGroupId]?.name ?: "Todos os Grupos"
                                    Text(name, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp), tint = SirlimTeal)
                                }
                                DropdownMenu(
                                    expanded = showGroupFilter, 
                                    onDismissRequest = { showGroupFilter = false },
                                    modifier = Modifier.background(SirlimDarkBlue).border(1.dp, SirlimTeal.copy(alpha = 0.3f))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Todos os Grupos", color = Color.White) }, 
                                        onClick = { selectedGroupId = null; showGroupFilter = false }
                                    )
                                    groups.values.toList().sortedBy { it.name }.forEach { grp ->
                                        DropdownMenuItem(
                                            text = { Text(grp.name, color = Color.White) }, 
                                            onClick = { selectedGroupId = grp.id; showGroupFilter = false }
                                        )
                                    }
                                }
                            }

                            // Search Text
                            OutlinedTextField(
                                value = searchTxt,
                                onValueChange = { searchTxt = it },
                                placeholder = { Text("Pesquisar...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SirlimTeal,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                trailingIcon = { Icon(Icons.Default.Search, null, tint = SirlimTeal.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }

            val dayCleanings = cleanings.filter { cleaning ->
                val startTime = try { ZonedDateTime.parse(cleaning.startTime).withZoneSameInstant(ZoneId.systemDefault()) } catch(e:Exception){null}
                val dateMatch = startTime?.toLocalDate() == selectedDate
                if (!dateMatch) return@filter false
                
                if (!isCalendarVisible) {
                    val comp = compartments[cleaning.compartmentId]
                    val groupMatch = selectedGroupId == null || comp?.groupId == selectedGroupId
                    val searchMatch = searchTxt.isBlank() || 
                        comp?.name?.contains(searchTxt, ignoreCase = true) == true
                    groupMatch && searchMatch
                } else true
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirlimTeal)
                }
            } else if (dayCleanings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma limpeza registada.", color = Color.White.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayCleanings) { cleaning ->
                        val comp = compartments[cleaning.compartmentId]
                        val group = groups[comp?.groupId ?: ""]
                        CleaningHistoryItem(
                            cleaning = cleaning,
                            compartment = comp,
                            groupName = group?.name,
                            onClick = { selectedCleaning = cleaning }
                        )
                    }
                }
            }
        }
    }

    selectedCleaning?.let { cleaning ->
        val comp = compartments[cleaning.compartmentId]
        val group = groups[comp?.groupId ?: ""]
        CleaningDetailDialog(
            cleaning = cleaning,
            compartment = comp,
            groupName = group?.name,
            onDismiss = { selectedCleaning = null },
            getTasks = { viewModel.getPerformedTasks(cleaning.id!!) }
        )
    }
}

@Composable
fun CleaningHistoryItem(
    cleaning: Cleaning,
    compartment: Compartment?,
    groupName: String?,
    onClick: () -> Unit
) {
    val startTime = try { ZonedDateTime.parse(cleaning.startTime).withZoneSameInstant(ZoneId.systemDefault()) } catch(e:Exception){ null }
    val timeStr = startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = timeStr, fontWeight = FontWeight.Bold, color = SirlimBlue, fontSize = 16.sp)
                Text(text = "Início", fontSize = 10.sp, color = Color.Gray)
            }
            
            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.3f))

            Column(modifier = Modifier.weight(1f)) {
                if (!groupName.isNullOrBlank()) {
                    Text(text = groupName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SirlimTeal)
                }
                Text(text = compartment?.name ?: "Compartimento", fontWeight = FontWeight.ExtraBold, color = SirlimDarkBlue, fontSize = 16.sp)
                Text(text = "Duração: ${formatSeconds(calculateDuration(cleaning))}", fontSize = 11.sp, color = Color.Gray)
            }
            
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SirlimTeal)
        }
    }
}

@Composable
fun CleaningDetailDialog(
    cleaning: Cleaning,
    compartment: Compartment?,
    groupName: String?,
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
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (!groupName.isNullOrBlank()) {
                    Text(text = groupName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SirlimTeal)
                }
                Text(text = compartment?.name ?: "Detalhes", fontWeight = FontWeight.Black, fontSize = 22.sp, color = SirlimDarkBlue)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow("Início", formatTime(cleaning.startTime))
                DetailRow("Fim", cleaning.endTime?.let { formatTime(it) } ?: "--:--")
                DetailRow("Pausa", formatSeconds(cleaning.pauseDurationSeconds))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Tarefas Realizadas:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SirlimBlue)
                if (isLoadingTasks) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                } else if (tasks.isEmpty()) {
                    Text("Nenhuma tarefa registada.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    tasks.forEach { task ->
                        Text("• ${task.name}", fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp), color = Color.DarkGray)
                    }
                }

                if (!cleaning.observations.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Observações:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SirlimBlue)
                    Text(text = cleaning.observations, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SirlimBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("FECHAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White) }
        Text(text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("pt"))} ${currentMonth.year}".uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White) }
    }
}

@Composable
fun CalendarGrid(currentMonth: YearMonth, selectedDate: LocalDate, cleanings: List<Cleaning>, onDateSelected: (LocalDate) -> Unit) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val offset = currentMonth.atDay(1).dayOfWeek.value - 1
    val borderColor = SirlimTeal.copy(alpha = 0.2f)

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            val isSelected = date == selectedDate
                            val hasCleanings = cleanings.any { try { ZonedDateTime.parse(it.startTime).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate() == date } catch(e:Exception){false} }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .then(if (isSelected) Modifier.border(1.5.dp, SirlimTeal, RoundedCornerShape(8.dp)) else Modifier)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (hasCleanings) SirlimTeal.copy(alpha = 0.35f) else Color.Transparent)
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = dayNum.toString(), color = Color.White, fontWeight = if (hasCleanings || isSelected) FontWeight.ExtraBold else FontWeight.Normal, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.Medium, color = SirlimDarkBlue, fontSize = 14.sp)
    }
}

fun calculateDuration(cleaning: Cleaning): Int {
    return try {
        val start = ZonedDateTime.parse(cleaning.startTime).toEpochSecond()
        val end = cleaning.endTime?.let { ZonedDateTime.parse(it).toEpochSecond() } ?: start
        (end - start).toInt()
    } catch (e: Exception) { 0 }
}

fun formatTime(isoStr: String): String {
    return try { ZonedDateTime.parse(isoStr).withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) } catch (e: Exception) { "--:--" }
}

fun formatSeconds(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}
