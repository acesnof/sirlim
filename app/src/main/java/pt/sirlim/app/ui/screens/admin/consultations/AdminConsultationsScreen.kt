package pt.sirlim.app.ui.screens.admin.consultations

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
import pt.sirlim.app.ui.screens.user.consultations.CalendarHeader
import pt.sirlim.app.ui.screens.user.consultations.formatSeconds
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import pt.sirlim.app.utils.ExcelUtils
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsultationsScreen(
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
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Apagar Registo") },
            text = { Text("Deseja apagar permanentemente esta limpeza?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCleaning(showDeleteConfirm!!) { 
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
                            fileName = "Relatorio_Consulta.xlsx"
                        )
                    }) {
                        Icon(Icons.Default.Download, "Exportar Excel", tint = Color.White)
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
        AdminCleaningDetailDialog(
            cleaning = cleaning,
            compartment = compartments[cleaning.compartmentId],
            groupName = groups[compartments[cleaning.compartmentId]?.groupId ?: ""]?.name,
            userName = users[cleaning.userId]?.fullName ?: users[cleaning.userId]?.username ?: "Desconhecido",
            onDismiss = { selectedCleaning = null },
            onDelete = { showDeleteConfirm = cleaning.id },
            getTasks = { viewModel.getPerformedTasks(cleaning.id!!) }
        )
    }
}

@Composable
fun CalendarConsultationView(
    cleanings: List<Cleaning>,
    compartments: Map<String, Compartment>,
    groups: Map<String, Group>,
    users: Map<String, User>,
    onItemClick: (Cleaning) -> Unit
) {
    var isCalendarVisible by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    var searchTxt by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var showGroupFilter by remember { mutableStateOf(false) }

    Column {
        AnimatedVisibility(visible = isCalendarVisible, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                CalendarHeader(currentMonth = currentMonth, onMonthChange = { currentMonth = it })
                ConsultationCalendarGrid(currentMonth = currentMonth, selectedDate = selectedDate, cleanings = cleanings, onDateSelected = { selectedDate = it })
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
            val dateMatch = try { ZonedDateTime.parse(cleaning.startTime).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate() == selectedDate } catch (e: Exception) { false }
            if (!dateMatch) return@filter false
            
            if (!isCalendarVisible) {
                val comp = compartments[cleaning.compartmentId]
                val groupMatch = selectedGroupId == null || comp?.groupId == selectedGroupId
                val searchMatch = searchTxt.isBlank() || 
                    comp?.name?.contains(searchTxt, ignoreCase = true) == true || 
                    users[cleaning.userId]?.fullName?.contains(searchTxt, ignoreCase = true) == true || 
                    users[cleaning.userId]?.username?.contains(searchTxt, ignoreCase = true) == true
                groupMatch && searchMatch
            } else true
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dayCleanings) { cleaning ->
                val comp = compartments[cleaning.compartmentId]
                val group = groups[comp?.groupId ?: ""]
                val user = users[cleaning.userId]
                AdminCleaningHistoryItem(
                    cleaning = cleaning,
                    compartmentName = comp?.name ?: "Compartimento",
                    groupName = group?.name,
                    userName = user?.fullName ?: user?.username ?: "Desconhecido",
                    onClick = { onItemClick(cleaning) }
                )
            }
        }
    }
}

@Composable
fun ConsultationCalendarGrid(currentMonth: YearMonth, selectedDate: LocalDate, cleanings: List<Cleaning>, onDateSelected: (LocalDate) -> Unit) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val offset = currentMonth.atDay(1).dayOfWeek.value - 1
    val borderColor = SirlimTeal.copy(alpha = 0.1f)

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { day ->
                Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    .size(34.dp)
                                    .then(if (isSelected) Modifier.border(1.5.dp, SirlimTeal, RoundedCornerShape(8.dp)) else Modifier)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (hasCleanings) SirlimTeal.copy(alpha = 0.25f) else Color.Transparent)
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
fun CompartmentConsultationView(
    cleanings: List<Cleaning>,
    compartments: Map<String, Compartment>,
    groups: Map<String, Group>,
    users: Map<String, User>,
    onItemClick: (Cleaning) -> Unit
) {
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedCompartment by remember { mutableStateOf<Compartment?>(null) }

    if (selectedGroupId == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Selecione o Grupo", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(groups.values.toList().sortedBy { it.name }) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedGroupId = group.id },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = SirlimTeal, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(group.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    } else if (selectedCompartment == null) {
        val groupComps = compartments.values.filter { it.groupId == selectedGroupId }.sortedBy { it.name }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedGroupId = null }, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp)) 
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(groups[selectedGroupId]?.name ?: "", color = SirlimTeal, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(groupComps) { comp ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCompartment = comp },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MeetingRoom, null, tint = SirlimTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(comp.name, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    } else {
        val filtered = cleanings.filter { it.compartmentId == selectedCompartment?.id }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedCompartment = null }, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp)) 
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(selectedCompartment?.name ?: "", color = SirlimTeal, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { cleaning ->
                    val user = users[cleaning.userId]
                    AdminCleaningHistoryItem(
                        cleaning = cleaning,
                        compartmentName = selectedCompartment?.name ?: "",
                        groupName = groups[selectedGroupId]?.name,
                        userName = user?.fullName ?: user?.username ?: "Desconhecido",
                        onClick = { onItemClick(cleaning) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserConsultationView(
    cleanings: List<Cleaning>,
    compartments: Map<String, Compartment>,
    groups: Map<String, Group>,
    users: Map<String, User>,
    onItemClick: (Cleaning) -> Unit
) {
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (selectedUser == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Selecione o Funcionário", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(users.values.filter { it.role == UserRole.USER }.sortedBy { it.fullName ?: it.username }) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedUser = user },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SirlimTeal.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = SirlimTeal)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(user.fullName ?: user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    } else {
        val userCleanings = cleanings.filter { cleaning ->
            val date = try { ZonedDateTime.parse(cleaning.startTime).withZoneSameInstant(ZoneId.systemDefault()).toLocalDate() } catch (e: Exception) { null }
            cleaning.userId == selectedUser?.id && date != null && !date.isBefore(startDate) && !date.isAfter(endDate)
        }
        
        val totalSeconds = userCleanings.sumOf { cleaning -> calculateDuration(cleaning).toLong() }.toInt()

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedUser = null }, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp)) 
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedUser?.fullName ?: selectedUser?.username ?: "", color = SirlimTeal, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("De ${startDate.format(DateTimeFormatter.ofPattern("dd/MM"))} a ${endDate.format(DateTimeFormatter.ofPattern("dd/MM"))} • Total: ${formatSeconds(totalSeconds)}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                IconButton(onClick = { showDatePicker = true }, modifier = Modifier.clip(CircleShape).background(SirlimTeal)) { 
                    Icon(Icons.Default.DateRange, "Filtrar Datas", tint = SirlimBlue, modifier = Modifier.size(20.dp)) 
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(userCleanings) { cleaning ->
                    val comp = compartments[cleaning.compartmentId]
                    val grp = groups[comp?.groupId ?: ""]
                    AdminCleaningHistoryItem(
                        cleaning = cleaning,
                        compartmentName = comp?.name ?: "",
                        groupName = grp?.name,
                        userName = selectedUser?.fullName ?: selectedUser?.username ?: "",
                        onClick = { onItemClick(cleaning) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        PremiumDateRangePickerDialog(
            currentStart = startDate,
            currentEnd = endDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                startDate = s
                endDate = e
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumDateRangePickerDialog(
    currentStart: LocalDate,
    currentEnd: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var start by remember { mutableStateOf(currentStart) }
    var end by remember { mutableStateOf(currentEnd) }
    
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar por Período", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Selecione o intervalo de datas para a pesquisa.", fontSize = 14.sp, color = Color.Gray)
                
                // Botão para Data Início
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Início: ${start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                }
                
                // Botão para Data Fim
                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Fim: ${end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!start.isAfter(end)) onConfirm(start, end)
            }) { Text("Aplicar Filtro") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { start = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    showStartPicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = end.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { end = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    showEndPicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun AdminCleaningHistoryItem(
    cleaning: Cleaning,
    compartmentName: String,
    groupName: String?,
    userName: String,
    onClick: () -> Unit
) {
    val dateStr = try { ZonedDateTime.parse(cleaning.startTime).withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) } catch (e: Exception) { "--" }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = dateStr, fontWeight = FontWeight.Black, color = SirlimBlue, fontSize = 13.sp)
                Text(text = "REGISTO", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 14.dp), color = Color.LightGray.copy(alpha = 0.5f))
            Column(modifier = Modifier.weight(1f)) {
                if (!groupName.isNullOrBlank()) Text(text = groupName.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SirlimTeal)
                Text(text = compartmentName, fontWeight = FontWeight.ExtraBold, color = SirlimDarkBlue, fontSize = 15.sp)
                Text(text = "POR: $userName", fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SirlimTeal)
        }
    }
}

@Composable
fun AdminCleaningDetailDialog(
    cleaning: Cleaning,
    compartment: Compartment?,
    groupName: String?,
    userName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!groupName.isNullOrBlank()) Text(text = groupName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SirlimTeal)
                        Text(text = compartment?.name ?: "Detalhes", fontWeight = FontWeight.Black, fontSize = 22.sp, color = SirlimDarkBlue)
                    }
                    IconButton(onClick = { onDismiss(); onDelete() }, modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))) { 
                        Icon(Icons.Default.Delete, "Apagar", tint = Color.Red, modifier = Modifier.size(20.dp)) 
                    }
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

fun calculateDuration(cleaning: Cleaning): Int {
    val start = try { ZonedDateTime.parse(cleaning.startTime).toEpochSecond() } catch (e: Exception) { 0L }
    val end = try { cleaning.endTime?.let { ZonedDateTime.parse(it).toEpochSecond() } ?: start } catch (e: Exception) { start }
    return (end - start).toInt()
}
