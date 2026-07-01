package pt.sirlim.app.ui.screens.admin.scheduling

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import pt.sirlim.app.data.model.User
import pt.sirlim.app.data.model.WeeklyPlan
import pt.sirlim.app.data.model.WeeklyPlanCompartment
import pt.sirlim.app.ui.screens.admin.groups_compartments.GroupsCompartmentsViewModel
import pt.sirlim.app.ui.screens.login.LoginViewModel
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(onBack: () -> Unit) {
    val viewModel: SchedulingViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()
    val plans by viewModel.plans.collectAsState()
    val userAssignments by viewModel.userAssignments.collectAsState()
    val allUsers by loginViewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var planToEdit by remember { mutableStateOf<WeeklyPlan?>(null) }
    var selectedPlanForDetails by remember { mutableStateOf<WeeklyPlan?>(null) }
    var showAddPlanDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<WeeklyPlan?>(null) }

    LaunchedEffect(Unit) { 
        viewModel.fetchPlans() 
        loginViewModel.fetchUsers()
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Apagar Plano") },
            text = { Text("Deseja apagar o plano '${showDeleteConfirm!!.name}' e todas as suas configurações?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(showDeleteConfirm!!.id!!) { success ->
                        showDeleteConfirm = null
                    }
                }) { Text("APAGAR", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("CANCELAR") } }
        )
    }

    if (selectedPlanForDetails != null) {
        PlanDetailsScreen(plan = selectedPlanForDetails!!, onBack = { selectedPlanForDetails = null })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Programação Semanal", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { planToEdit = null; showAddPlanDialog = true }, containerColor = SirlimTeal) {
                    Icon(Icons.Default.Add, null, tint = SirlimBlue)
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue))).padding(16.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = SirlimTeal)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(plans) { plan ->
                            val assignedUsers = userAssignments.filter { it.planId == plan.id }.mapNotNull { assoc ->
                                allUsers.find { it.id == assoc.userId }?.fullName ?: allUsers.find { it.id == assoc.userId }?.username
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // ÁREA DE TEXTO CLICÁVEL (Para ver detalhes)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedPlanForDetails = plan }
                                    ) {
                                        Text(plan.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        if (assignedUsers.isNotEmpty()) {
                                            Text("Atribuído a: ${assignedUsers.joinToString(", ")}", color = SirlimTeal, fontSize = 11.sp)
                                        } else {
                                            Text("Sem utilizadores atribuídos", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                        }
                                        Text("Toque para configurar os dias", color = SirlimTeal.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    // BOTÕES DE ACÇÃO (Sempre visíveis e clicáveis)
                                    Row {
                                        IconButton(
                                            onClick = { planToEdit = plan; showAddPlanDialog = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, "Editar", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { showDeleteConfirm = plan },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, "Apagar", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPlanDialog) {
        PlanFormDialog(
            plan = planToEdit,
            // FILTRAR APENAS UTILIZADORES ATIVOS COM ROLE 'USER'
            allUsers = allUsers.filter { it.isActive && it.role == pt.sirlim.app.data.model.UserRole.USER },
            currentAssignments = userAssignments,
            onDismiss = { showAddPlanDialog = false },
            onConfirm = { name, uids ->
                viewModel.savePlan(id = planToEdit?.id, name = name, selectedUserIds = uids) { success ->
                    if (success) showAddPlanDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanFormDialog(
    plan: WeeklyPlan?,
    allUsers: List<User>,
    currentAssignments: List<pt.sirlim.app.data.model.UserWeeklyPlan>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(plan?.name ?: "") }
    val selectedUserIds = remember { 
        mutableStateListOf<String>().apply {
            addAll(currentAssignments.filter { it.planId == plan?.id }.map { it.userId })
        }
    }

    // Filtrar utilizadores que não têm plano, ou que já estão neste plano
    val availableUsers = allUsers.filter { user ->
        val existingAssignment = currentAssignments.find { it.userId == user.id }
        existingAssignment == null || (plan != null && existingAssignment.planId == plan.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (plan == null) "Novo Plano" else "Editar Plano") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Nome do Plano") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Atribuir a Funcionários:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    availableUsers.forEach { user ->
                        val isSelected = selectedUserIds.contains(user.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedUserIds.remove(user.id)
                                else selectedUserIds.add(user.id!!)
                            },
                            label = { Text(user.fullName ?: user.username, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SirlimTeal,
                                selectedLabelColor = SirlimBlue
                            )
                        )
                    }
                }
                if (availableUsers.isEmpty()) {
                    Text("Todos os utilizadores já têm planos atribuídos.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, selectedUserIds.toList()) }) {
                Text(if (plan == null) "Criar" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailsScreen(plan: WeeklyPlan, onBack: () -> Unit) {
    val viewModel: SchedulingViewModel = viewModel()
    val groupsViewModel: GroupsCompartmentsViewModel = viewModel()
    val items by viewModel.planItems.collectAsState()
    val compartments by groupsViewModel.compartments.collectAsState()
    val groups by groupsViewModel.groups.collectAsState()
    
    var selectedDay by remember { mutableIntStateOf(1) } // 1=Seg
    val days = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    LaunchedEffect(plan.id) { 
        viewModel.fetchPlanDetails(plan.id!!) 
        groupsViewModel.fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan.name, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))) {
            // Dias da Semana (Tabs)
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
                        text = { Text(day, color = Color.White, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Lista de Compartimentos para o Dia
            val dayItems = items.filter { it.dayOfWeek == selectedDay }
            
            Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dayItems) { item ->
                        val comp = compartments.find { it.id == item.compartmentId }
                        val group = groups.find { it.id == comp?.groupId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (group != null) {
                                        Text(group.name.uppercase(), color = SirlimTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("SEM GRUPO", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(comp?.name ?: "Desconhecido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    item.instructions?.let { 
                                        if (it.isNotBlank()) {
                                            Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                        }
                                    }
                                }
                                IconButton(onClick = { viewModel.removeItem(item.id!!, plan.id!!) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                    
                    item {
                        // Botão Adicionar Compartimento
                        var showAddDialog by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SirlimTeal.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Add, null, tint = SirlimTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adicionar Compartimento", color = SirlimTeal)
                        }

                        if (showAddDialog) {
                            AddCompartmentToPlanDialog(
                                compartments = compartments.filter { it.isActive },
                                groups = groups,
                                alreadyAddedCompIds = dayItems.map { it.compartmentId },
                                onDismiss = { showAddDialog = false },
                                onConfirm = { compId, instr ->
                                    viewModel.addItem(WeeklyPlanCompartment(
                                        plan_id = plan.id!!,
                                        compartmentId = compId,
                                        dayOfWeek = selectedDay,
                                        instructions = instr
                                    ))
                                    showAddDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCompartmentToPlanDialog(
    compartments: List<pt.sirlim.app.data.model.Compartment>,
    groups: List<pt.sirlim.app.data.model.Group>,
    alreadyAddedCompIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedCompId by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    
    var groupsExpanded by remember { mutableStateOf(false) }
    var compsExpanded by remember { mutableStateOf(false) }

    // Ordenação dos grupos: Alfabética, com "Sem Grupo" no fim
    val sortedGroups = groups.sortedBy { it.name }
    
    // Filtrar compartimentos pelo grupo selecionado E remover os já adicionados no dia
    val filteredCompartments = if (selectedGroupId == null) {
        compartments.filter { (it.groupId == null || it.groupId!!.isBlank()) && !alreadyAddedCompIds.contains(it.id) }
    } else {
        compartments.filter { it.groupId == selectedGroupId && !alreadyAddedCompIds.contains(it.id) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar ao Plano", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Seleção do Grupo
                Column {
                    Text("1. Selecione o Grupo", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { groupsExpanded = true }
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                    ) {
                        val groupName = if (selectedGroupId == null) "Sem Grupo / Outros" 
                                       else sortedGroups.find { it.id == selectedGroupId }?.name ?: "Selecione o Grupo"
                        Text(groupName)
                    }
                    DropdownMenu(expanded = groupsExpanded, onDismissRequest = { groupsExpanded = false }, modifier = Modifier.fillMaxWidth(0.8f)) {
                        sortedGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) }, 
                                onClick = { 
                                    selectedGroupId = group.id
                                    selectedCompId = "" // Reset comp ao mudar grupo
                                    groupsExpanded = false 
                                }
                            )
                        }
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Sem Grupo / Outros") }, 
                            onClick = { 
                                selectedGroupId = null
                                selectedCompId = ""
                                groupsExpanded = false 
                            }
                        )
                    }
                }

                // 2. Seleção do Compartimento
                Column {
                    Text("2. Selecione o Compartimento", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { compsExpanded = true }
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                    ) {
                        val compName = filteredCompartments.find { it.id == selectedCompId }?.name ?: "Escolha o Compartimento"
                        Text(compName)
                    }
                    DropdownMenu(expanded = compsExpanded, onDismissRequest = { compsExpanded = false }, modifier = Modifier.fillMaxWidth(0.8f)) {
                        if (filteredCompartments.isEmpty()) {
                            DropdownMenuItem(text = { Text("Nenhum compartimento encontrado", color = Color.Gray) }, onClick = {})
                        } else {
                            filteredCompartments.sortedBy { it.name }.forEach { comp ->
                                DropdownMenuItem(
                                    text = { Text(comp.name) }, 
                                    onClick = { 
                                        selectedCompId = comp.id!!
                                        compsExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Instruções
                OutlinedTextField(
                    value = instructions, 
                    onValueChange = { instructions = it }, 
                    label = { Text("Instruções de limpeza") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedCompId.isNotBlank()) onConfirm(selectedCompId, instructions) },
                enabled = selectedCompId.isNotBlank()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
