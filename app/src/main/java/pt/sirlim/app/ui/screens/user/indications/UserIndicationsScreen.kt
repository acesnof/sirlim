package pt.sirlim.app.ui.screens.user.indications

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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
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
import pt.sirlim.app.data.model.Indication
import pt.sirlim.app.data.model.Task
import pt.sirlim.app.data.model.Urgency
import pt.sirlim.app.ui.screens.admin.groups_compartments.GroupsCompartmentsViewModel
import pt.sirlim.app.ui.screens.admin.indications.IndicationsViewModel
import pt.sirlim.app.ui.screens.user.consultations.CalendarHeader
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserIndicationsScreen(
    userId: String,
    onStartCleaning: (String, String) -> Unit, // compId, indicationId
    onBack: () -> Unit
) {
    val viewModel: IndicationsViewModel = viewModel()
    val groupsViewModel: GroupsCompartmentsViewModel = viewModel()
    
    val indications by viewModel.indications.collectAsState()
    val compartments by groupsViewModel.compartments.collectAsState()
    val groups by groupsViewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(userId) {
        viewModel.fetchUserIndications(userId)
        groupsViewModel.fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("As Minhas Tarefas", color = Color.White, fontWeight = FontWeight.Bold) },
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
            CalendarHeader(currentMonth = currentMonth, onMonthChange = { currentMonth = it })

            UserIndicationCalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                indications = indications,
                onDateSelected = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tarefas para ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            val dayIndications = indications.filter { it.scheduledDate == selectedDate.toString() }
            val sortedDayIndications = dayIndications.sortedWith(compareBy(
                { ind -> 
                    val comp = compartments.find { it.id == ind.compartmentId }
                    val group = groups.find { it.id == comp?.groupId }
                    group?.name ?: "ZZZ"
                },
                { ind ->
                    val comp = compartments.find { it.id == ind.compartmentId }
                    comp?.name ?: ""
                }
            ))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirlimTeal)
                }
            } else if (dayIndications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sem tarefas atribuídas.", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sortedDayIndications) { indication ->
                        val comp = compartments.find { it.id == indication.compartmentId }
                        val group = groups.find { it.id == comp?.groupId }
                        val tasks = remember { mutableStateListOf<Task>() }
                        
                        LaunchedEffect(indication.id) {
                            val taskIds = viewModel.getIndicationTasks(indication.id!!)
                            val allTasks = groupsViewModel.tasks.value
                            tasks.clear()
                            tasks.addAll(allTasks.filter { taskIds.contains(it.id) })
                        }

                        UserIndicationItemPremium(
                            indication = indication,
                            compName = comp?.name ?: "Compartimento",
                            groupName = group?.name ?: "Sem Grupo",
                            tasks = tasks,
                            onAction = { onStartCleaning(indication.compartmentId, indication.id!!) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserIndicationCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    indications: List<Indication>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val offset = currentMonth.atDay(1).dayOfWeek.value - 1
    val borderColor = Color.White.copy(alpha = 0.1f)

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("S", "T", "Q", "Q", "S", "S", "D")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = ((daysInMonth + offset + 6) / 7) * 7
        for (row in 0 until (totalCells / 7)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - offset + 1
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(0.5.dp, borderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNum)
                            val isSelected = date == selectedDate
                            val dayIndications = indications.filter { it.scheduledDate == date.toString() }
                            
                            val hasPending = dayIndications.any { !it.isCompleted }
                            val hasCompleted = dayIndications.isNotEmpty() && dayIndications.all { it.isCompleted }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> SirlimTeal
                                            hasPending -> Color(0xFFFF5252).copy(alpha = 0.5f)
                                            hasCompleted -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    color = if (isSelected) SirlimDarkBlue else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (dayIndications.isNotEmpty() || isSelected) FontWeight.Black else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserIndicationItemPremium(
    indication: Indication,
    compName: String,
    groupName: String,
    tasks: List<Task>,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (indication.isCompleted) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (indication.isCompleted) 0.dp else 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val urgencyColor = when(indication.urgency) {
                    Urgency.ALTA -> Color(0xFFFF5252)
                    Urgency.MEDIA -> Color(0xFFFFC107)
                    Urgency.BAIXA -> Color(0xFF4CAF50)
                }
                Box(modifier = Modifier.size(6.dp, 24.dp).clip(CircleShape).background(urgencyColor))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = compName, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = if (indication.isCompleted) Color.White else SirlimDarkBlue, 
                        fontSize = 18.sp
                    )
                    Text(
                        text = groupName, 
                        color = if (indication.isCompleted) Color.White.copy(alpha = 0.6f) else SirlimBlue, 
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (indication.isCompleted) {
                    Text(text = "CONCLUÍDA", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                } else {
                    Text(text = indication.urgency.name, color = urgencyColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            
            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "TAREFAS:", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (indication.isCompleted) Color.White.copy(alpha = 0.5f) else Color.Gray
                )
                tasks.forEach { task ->
                    Text(
                        text = "• ${task.name}", 
                        fontSize = 13.sp, 
                        color = if (indication.isCompleted) Color.White.copy(alpha = 0.7f) else SirlimBlue,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            indication.instructions?.let {
                if (it.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "INSTRUÇÕES: $it", 
                        fontSize = 12.sp, 
                        color = if (indication.isCompleted) Color.White.copy(alpha = 0.5f) else Color.DarkGray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            if (!indication.isCompleted) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, null, tint = SirlimBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("REALIZAR LIMPEZA", color = SirlimBlue, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}
