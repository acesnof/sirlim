package pt.sirlim.app.ui.screens.user.weekly

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.ui.screens.admin.groups_compartments.GroupsCompartmentsViewModel
import pt.sirlim.app.ui.screens.admin.scheduling.SchedulingViewModel
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserWeeklyPlanScreen(
    userId: String,
    onStartCleaning: (String, String?) -> Unit, // compId, instructions
    onBack: () -> Unit
) {
    val viewModel: SchedulingViewModel = viewModel()
    val groupsViewModel: GroupsCompartmentsViewModel = viewModel()
    
    val items by viewModel.planItems.collectAsState()
    val compartments by groupsViewModel.compartments.collectAsState()
    val groups by groupsViewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Dia atual (1=Seg, ..., 7=Dom)
    val today = LocalDate.now().dayOfWeek.value
    var selectedDay by remember { mutableIntStateOf(today) }
    val days = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    val userAssignments by viewModel.userAssignments.collectAsState()

    LaunchedEffect(userId) {
        viewModel.fetchUserAssignments()
        groupsViewModel.fetchData()
    }

    // Quando as atribuições chegam, procuramos o plano deste utilizador
    LaunchedEffect(userAssignments) {
        val myAssignment = userAssignments.find { it.userId == userId }
        myAssignment?.let {
            viewModel.fetchPlanDetails(it.planId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("A Minha Semana", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))) {
            // Seletor de Dias
            ScrollableTabRow(
                selectedTabIndex = selectedDay - 1,
                containerColor = Color.Transparent,
                contentColor = SirlimTeal,
                edgePadding = 16.dp,
                divider = {}
            ) {
                days.forEachIndexed { index, day ->
                    Tab(
                        selected = selectedDay == index + 1,
                        onClick = { selectedDay = index + 1 },
                        text = { Text(day, color = if (selectedDay == index + 1) SirlimTeal else Color.White.copy(alpha = 0.6f)) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirlimTeal)
                }
            } else {
                val dayItems = items.filter { it.dayOfWeek == selectedDay }
                val sortedDayItems = dayItems.sortedWith(compareBy(
                    { item -> 
                        val comp = compartments.find { it.id == item.compartmentId }
                        val group = groups.find { it.id == comp?.groupId }
                        group?.name ?: "ZZZ" // "ZZZ" so that items without group appear last
                    },
                    { item ->
                        val comp = compartments.find { it.id == item.compartmentId }
                        comp?.name ?: ""
                    }
                ))
                
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sortedDayItems) { item ->
                        val comp = compartments.find { it.id == item.compartmentId }
                        val group = groups.find { it.id == comp?.groupId }
                        var isCleaned by remember { mutableStateOf(false) }

                        LaunchedEffect(item.compartmentId) {
                            isCleaned = viewModel.checkIfCleanedThisWeek(item.compartmentId)
                        }

                        WeeklyTaskCard(
                            name = comp?.name ?: "Compartimento",
                            groupName = group?.name,
                            instructions = item.instructions,
                            isCompleted = isCleaned,
                            onAction = { onStartCleaning(item.compartmentId, item.instructions) }
                        )
                    }
                    
                    if (dayItems.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Sem tarefas planeadas para este dia.", color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyTaskCard(name: String, groupName: String?, instructions: String?, isCompleted: Boolean, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isCompleted) { onAction() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f)
        ),
        border = if (isCompleted) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (groupName != null) {
                    Text(text = groupName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SirlimTeal)
                } else {
                    Text(text = "SEM GRUPO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
                Text(text = name, fontWeight = FontWeight.Bold, color = if (isCompleted) Color(0xFFA5D6A7) else Color.White, fontSize = 16.sp)
                instructions?.let { 
                    if (it.isNotBlank()) {
                        Text(text = it, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }
            if (isCompleted) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
            } else {
                Icon(Icons.Default.CleaningServices, null, tint = SirlimTeal)
            }
        }
    }
}
