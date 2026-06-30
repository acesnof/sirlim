package pt.sirlim.app.ui.screens.admin.indications

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
import androidx.compose.material.icons.filled.Add
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
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.data.model.Indication
import pt.sirlim.app.data.model.Urgency
import pt.sirlim.app.ui.screens.admin.groups_compartments.GroupsCompartmentsViewModel
import pt.sirlim.app.ui.screens.login.LoginViewModel
import pt.sirlim.app.ui.screens.user.consultations.CalendarHeader
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicationsScreen(
    onAddIndication: (LocalDate) -> Unit,
    onEditIndication: (Indication) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: IndicationsViewModel = viewModel()
    val groupsViewModel: GroupsCompartmentsViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()

    val indications by viewModel.indications.collectAsState()
    val compartments by groupsViewModel.compartments.collectAsState()
    val groups by groupsViewModel.groups.collectAsState()
    val users by loginViewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
        viewModel.fetchIndications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Indicações Diárias", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddIndication(selectedDate) },
                containerColor = SirlimTeal,
                contentColor = SirlimBlue
            ) {
                Icon(Icons.Default.Add, "Nova Indicação")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
        ) {
            CalendarHeader(
                currentMonth = currentMonth,
                onMonthChange = { currentMonth = it }
            )

            IndicationCalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                indications = indications,
                onDateSelected = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Indicações para ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )

            val dayIndications = indications.filter { 
                it.scheduledDate == selectedDate.toString() 
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirlimTeal)
                }
            } else if (dayIndications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma indicação para este dia.", color = Color.White.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dayIndications) { indication ->
                        val comp = compartments.find { it.id == indication.compartmentId }
                        val group = groups.find { it.id == comp?.groupId }
                        val assignedUsers = remember { mutableStateListOf<String>() }
                        
                        LaunchedEffect(indication.id) {
                            val ids = viewModel.getIndicationUsers(indication.id!!)
                            assignedUsers.clear()
                            assignedUsers.addAll(users.filter { ids.contains(it.id) }.map { it.username })
                        }

                        IndicationItemPremium(
                            indication = indication,
                            compName = comp?.name ?: "Desconhecido",
                            groupName = group?.name ?: "Sem Grupo",
                            users = assignedUsers.joinToString(", "),
                            onClick = { onEditIndication(indication) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IndicationCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    indications: List<Indication>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val offset = currentMonth.atDay(1).dayOfWeek.value - 1
    val borderColor = SirlimTeal.copy(alpha = 0.2f)

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("S", "T", "Q", "Q", "S", "S", "D")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
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
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> SirlimTeal
                                            hasPending -> Color(0xFFEF9A9A).copy(alpha = 0.6f)
                                            hasCompleted -> Color(0xFFA5D6A7).copy(alpha = 0.6f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    color = if (isSelected) SirlimDarkBlue else Color.White,
                                    fontWeight = if (dayIndications.isNotEmpty() || isSelected) FontWeight.Bold else FontWeight.Normal
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
fun IndicationItemPremium(
    indication: Indication,
    compName: String,
    groupName: String,
    users: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val urgencyColor = when(indication.urgency) {
                Urgency.ALTA -> Color(0xFFFF5252)
                Urgency.MEDIA -> Color(0xFFFFC107)
                Urgency.BAIXA -> Color(0xFF4CAF50)
            }
            
            Box(modifier = Modifier.size(4.dp, 40.dp).background(urgencyColor, CircleShape))
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = compName, fontWeight = FontWeight.ExtraBold, color = SirlimDarkBlue, fontSize = 16.sp)
                Text(text = groupName, color = SirlimBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Para: $users", color = Color.Gray, fontSize = 12.sp)
                if (indication.isCompleted) {
                    Text(text = "✓ CONCLUÍDA", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SirlimTeal)
        }
    }
}
