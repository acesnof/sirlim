package pt.sirlim.app.ui.screens.admin.indications

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
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
import pt.sirlim.app.data.model.*
import pt.sirlim.app.ui.screens.admin.groups_compartments.GroupsCompartmentsViewModel
import pt.sirlim.app.ui.screens.login.LoginViewModel
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicationFormScreen(
    indicationId: String? = null,
    initialDate: LocalDate? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: IndicationsViewModel = viewModel()
    val groupsViewModel: GroupsCompartmentsViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()

    val indications by viewModel.indications.collectAsState()
    val compartments by groupsViewModel.compartments.collectAsState()
    val groups by groupsViewModel.groups.collectAsState()
    val users by loginViewModel.users.collectAsState()
    val allTasks by groupsViewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.error.collectAsState()

    // Encontrar a indicação baseada no ID
    val indication = remember(indicationId, indications) {
        indications.find { it.id == indicationId }
    }

    val isReadOnly = indication?.isCompleted == true

    var selectedGroupId by remember(indication) { 
        mutableStateOf(indication?.let { ind -> compartments.find { it.id == ind.compartmentId }?.groupId }) 
    }
    var selectedCompId by remember(indication) { mutableStateOf(indication?.compartmentId ?: "") }
    var urgency by remember(indication) { mutableStateOf(indication?.urgency ?: Urgency.MEDIA) }
    var instructions by remember(indication) { mutableStateOf(indication?.instructions ?: "") }
    var scheduledDate by remember(indication) { mutableStateOf(indication?.scheduledDate ?: initialDate?.toString() ?: LocalDate.now().toString()) }
    
    var selectedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var expandedGroup by remember { mutableStateOf(false) }
    var expandedComp by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(indicationId) {
        if (indicationId != null) {
            viewModel.fetchIndications() // Garante que a lista está carregada
            val ids = viewModel.getIndicationUsers(indicationId)
            selectedUserIds = ids.toSet()
            val tIds = viewModel.getIndicationTasks(indicationId)
            selectedTaskIds = tIds.toSet()
        } else {
            viewModel.fetchIndications()
        }
    }

    // Obter tarefas do compartimento em tempo real
    val compartmentTasks = remember { mutableStateListOf<String>() }
    LaunchedEffect(selectedCompId) {
        if (selectedCompId.isNotBlank()) {
            val ids = groupsViewModel.getCompartmentTasks(selectedCompId)
            compartmentTasks.clear()
            compartmentTasks.addAll(ids)
            // Se for nova indicação, auto-selecionar as tarefas do compartimento
            if (indication == null) {
                selectedTaskIds = ids.toSet()
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Apagar Indicação") },
            text = { Text("Deseja apagar esta indicação permanentemente?") },
            confirmButton = {
                TextButton(onClick = {
                    indication?.id?.let { id ->
                        viewModel.deleteIndication(id) { success ->
                            if (success) {
                                Toast.makeText(context, "Indicação apagada!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    }
                }) { Text("APAGAR", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("CANCELAR") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isReadOnly) "Visualizar Indicação" else if (indication == null) "Nova Indicação" else "Editar Indicação", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    if (indication != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Apagar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
            )
        },
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = {
                        if (selectedCompId.isNotBlank() && selectedUserIds.isNotEmpty()) {
                            val newInd = Indication(
                                id = indication?.id,
                                compartmentId = selectedCompId,
                                urgency = urgency,
                                instructions = instructions,
                                scheduledDate = scheduledDate,
                                isCompleted = indication?.isCompleted ?: false
                            )
                            viewModel.saveIndication(newInd, selectedUserIds.toList(), selectedTaskIds.toList()) { success ->
                                if (success) {
                                    Toast.makeText(context, "Indicação guardada com sucesso!", Toast.LENGTH_LONG).show()
                                    onBack()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Selecione o compartimento e pelo menos um funcionário.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = SirlimTeal,
                    contentColor = SirlimBlue
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SirlimBlue)
                    else Icon(Icons.Default.Save, "Guardar")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (isReadOnly) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9).copy(alpha = 0.2f))
                ) {
                    Text(
                        "ESTA INDICAÇÃO JÁ FOI CONCLUÍDA E ESTÁ EM MODO DE LEITURA.",
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // 1. GRUPO
            Text("1. Selecione o Grupo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(
                expanded = expandedGroup && !isReadOnly,
                onExpandedChange = { if (!isReadOnly) expandedGroup = it },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val groupName = groups.find { it.id == selectedGroupId }?.name ?: "Sem Grupo / Ver Todos"
                OutlinedTextField(
                    value = groupName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isReadOnly,
                    trailingIcon = { if (!isReadOnly) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGroup) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, 
                        unfocusedTextColor = Color.White, 
                        disabledTextColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = SirlimTeal
                    )
                )
                ExposedDropdownMenu(expanded = expandedGroup, onDismissRequest = { expandedGroup = false }) {
                    DropdownMenuItem(text = { Text("Sem Grupo / Ver Todos") }, onClick = { selectedGroupId = null; expandedGroup = false })
                    groups.filter { it.isActive || it.id == selectedGroupId }.forEach { g ->
                        DropdownMenuItem(text = { Text(g.name) }, onClick = { selectedGroupId = g.id; expandedGroup = false })
                    }
                }
            }

            // 2. COMPARTIMENTO
            val filteredComps = if (selectedGroupId == null) {
                compartments.filter { it.isActive || it.id == selectedCompId }
            } else {
                compartments.filter { it.groupId == selectedGroupId && (it.isActive || it.id == selectedCompId) }
            }
            
            Text("2. Selecione o Compartimento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(
                expanded = expandedComp && !isReadOnly,
                onExpandedChange = { if (!isReadOnly) expandedComp = it },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val compName = filteredComps.find { it.id == selectedCompId }?.name ?: "Selecionar..."
                OutlinedTextField(
                    value = compName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isReadOnly,
                    trailingIcon = { if (!isReadOnly) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedComp) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, 
                        unfocusedTextColor = Color.White, 
                        disabledTextColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = SirlimTeal
                    )
                )
                ExposedDropdownMenu(expanded = expandedComp, onDismissRequest = { expandedComp = false }) {
                    filteredComps.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name) }, 
                            onClick = { 
                                selectedCompId = c.id!!
                                selectedGroupId = c.groupId
                                expandedComp = false 
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. URGENCIA
            Text("3. Urgência", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Urgency.entries.forEach { u ->
                    val color = when(u) {
                        Urgency.ALTA -> Color(0xFFFF5252)
                        Urgency.MEDIA -> Color(0xFFFFC107)
                        Urgency.BAIXA -> Color(0xFF4CAF50)
                    }
                    FilterChip(
                        selected = urgency == u,
                        onClick = { if (!isReadOnly) urgency = u },
                        enabled = !isReadOnly || urgency == u,
                        label = { Text(u.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor = Color.White,
                            disabledSelectedContainerColor = color.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. FUNCIONÁRIOS
            Text("4. Funcionários Atribuídos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    val usersToShow = if (isReadOnly) {
                        users.filter { selectedUserIds.contains(it.id) }
                    } else {
                        users.filter { it.role == UserRole.USER && (it.isActive || selectedUserIds.contains(it.id)) }
                    }
                    
                    usersToShow.forEach { user ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(enabled = !isReadOnly) {
                            selectedUserIds = if (selectedUserIds.contains(user.id)) selectedUserIds - user.id!! else selectedUserIds + user.id!!
                        }.padding(4.dp)) {
                            Checkbox(
                                checked = selectedUserIds.contains(user.id), 
                                onCheckedChange = null, 
                                enabled = !isReadOnly,
                                colors = CheckboxDefaults.colors(checkedColor = SirlimTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val displayName = if (user.fullName.isNullOrBlank()) user.username else "${user.username} (${user.fullName})"
                            Text(displayName, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. TAREFAS
            Text("5. Selecione as Tarefas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    val tasksToDisplay = allTasks.filter { compartmentTasks.contains(it.id) || selectedTaskIds.contains(it.id) }
                    
                    if (selectedCompId.isBlank()) {
                        Text("Escolha um compartimento primeiro.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                    } else if (tasksToDisplay.isEmpty()) {
                        Text("Este compartimento não tem tarefas obrigatórias associadas.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                    } else {
                        tasksToDisplay.forEach { task ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(enabled = !isReadOnly) {
                                selectedTaskIds = if (selectedTaskIds.contains(task.id)) selectedTaskIds - task.id!! else selectedTaskIds + task.id!!
                            }.padding(4.dp)) {
                                Checkbox(
                                    checked = selectedTaskIds.contains(task.id), 
                                    onCheckedChange = null, 
                                    enabled = !isReadOnly,
                                    colors = CheckboxDefaults.colors(checkedColor = SirlimTeal)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(task.name, color = Color.White, fontSize = 14.sp)
                                    task.description?.let { Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { if (!isReadOnly) instructions = it },
                label = { Text("Instruções Adicionais", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = isReadOnly,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, 
                    unfocusedTextColor = Color.White, 
                    disabledTextColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = SirlimTeal
                )
            )

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Erro: $errorMsg", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
