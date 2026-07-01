package pt.sirlim.app.ui.screens.admin.indications

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
    var isCalendarVisible by remember { mutableStateOf(true) }
    
    var searchTxt by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var showGroupFilter by remember { mutableStateOf(false) }
    
    var showDeleteConfirm by remember { mutableStateOf<Indication?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchIndications()
        groupsViewModel.fetchData()
        loginViewModel.fetchUsers()
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Apagar Indicação") },
            text = { Text("Deseja apagar esta indicação permanentemente?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteIndication(showDeleteConfirm!!.id!!) { 
                        showDeleteConfirm = null
                    }
                }) { Text("APAGAR", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("CANCELAR") } }
        )
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
            AnimatedVisibility(visible = isCalendarVisible, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    CalendarHeader(currentMonth = currentMonth, onMonthChange = { currentMonth = it })
                    IndicationCalendarGrid(currentMonth = currentMonth, selectedDate = selectedDate, indications = indications, onDateSelected = { selectedDate = it })
                }
            }

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
                            text = "Indicações em ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
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
                            Box(modifier = Modifier.weight(1.2f)) {
                                OutlinedButton(
                                    onClick = { showGroupFilter = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, SirlimTeal.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val name = groups.find { it.id == selectedGroupId }?.name ?: "Todos os Grupos"
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
                                    groups.sortedBy { it.name }.forEach { grp ->
                                        DropdownMenuItem(
                                            text = { Text(grp.name, color = Color.White) }, 
                                            onClick = { selectedGroupId = grp.id; showGroupFilter = false }
                                        )
                                    }
                                }
                            }

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

            val dayIndications = indications.filter { ind ->
                val dateMatch = ind.scheduledDate == selectedDate.toString()
                if (!dateMatch) return@filter false
                
                if (!isCalendarVisible) {
                    val comp = compartments.find { it.id == ind.compartmentId }
                    val groupMatch = selectedGroupId == null || comp?.groupId == selectedGroupId
                    val searchMatch = searchTxt.isBlank() || comp?.name?.contains(searchTxt, ignoreCase = true) == true
                    groupMatch && searchMatch
                } else true
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SirlimTeal)
                }
            } else if (dayIndications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma indicação.", color = Color.White.copy(alpha = 0.6f))
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
                            val names = users.filter { ids.contains(it.id) }.map { it.fullName ?: it.username }
                            assignedUsers.addAll(names)
                        }

                        IndicationItemPremium(
                            indication = indication,
                            compName = comp?.name ?: "Compartimento",
                            groupName = group?.name ?: "Sem Grupo",
                            users = assignedUsers.joinToString(", "),
                            onDelete = { 
                                showDeleteConfirm = indication 
                            },
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
    val borderColor = SirlimTeal.copy(alpha = 0.1f)

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("S", "T", "Q", "Q", "S", "S", "D")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
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
                            
                            val hasPending = dayIndications.isNotEmpty() && dayIndications.any { !it.isCompleted }
                            val hasCompleted = dayIndications.isNotEmpty() && dayIndications.all { it.isCompleted }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .then(if (isSelected) Modifier.border(1.5.dp, SirlimTeal, RoundedCornerShape(8.dp)) else Modifier)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
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
                                    color = Color.White,
                                    fontWeight = if (dayIndications.isNotEmpty() || isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontSize = 13.sp
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
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (indication.isCompleted) Color(0xFFE8F5E9).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val urgencyColor = when(indication.urgency) {
                Urgency.ALTA -> Color(0xFFFF5252)
                Urgency.MEDIA -> Color(0xFFFFC107)
                Urgency.BAIXA -> Color(0xFF4CAF50)
            }
            
            Box(modifier = Modifier.size(4.dp, 45.dp).background(urgencyColor, CircleShape))
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = compName, fontWeight = FontWeight.ExtraBold, color = SirlimDarkBlue, fontSize = 16.sp)
                Text(text = groupName.uppercase(), color = SirlimBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "FUNCIONÁRIOS: $users", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                
                if (indication.isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "✓ CONCLUÍDA", color = Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            
            IconButton(
                onClick = { onDelete() }, 
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete, 
                    null, 
                    tint = Color.Red.copy(alpha = 0.6f), 
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SirlimTeal)
        }
    }
}
