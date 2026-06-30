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
    indication: Indication? = null,
    initialDate: LocalDate? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: IndicationsViewModel = viewModel()
    val groupsViewModel: GroupsCompartmentsViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()

    val groups by groupsViewModel.groups.collectAsState()
    val compartments by groupsViewModel.compartments.collectAsState()
    val users by loginViewModel.users.collectAsState()
    val allTasks by groupsViewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.error.collectAsState()

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

    LaunchedEffect(indication) {
        indication?.id?.let { id ->
            selectedUserIds = viewModel.getIndicationUsers(id).toSet()
            selectedTaskIds = viewModel.getIndicationTasks(id).toSet()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Apagar Indicação") },
            text = { Text("Deseja apagar esta indicação? A limpeza associada também poderá ser afetada.") },
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
                title = { Text(if (indication == null) "Nova Indicação" else "Editar Indicação", color = Color.White, fontWeight = FontWeight.Bold) },
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
                            } else {
                                Toast.makeText(context, "Erro ao gravar. Tente novamente.", Toast.LENGTH_LONG).show()
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // GRUPO (FILTRAR ATIVOS OU MANTER O ATUAL SE EDITANDO)
            Text("1. Selecione o Grupo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(
                expanded = expandedGroup,
                onExpandedChange = { expandedGroup = it },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val groupName = groups.find { it.id == selectedGroupId }?.name ?: "Sem Grupo / Ver Todos"
                OutlinedTextField(
                    value = groupName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGroup) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SirlimTeal)
                )
                ExposedDropdownMenu(expanded = expandedGroup, onDismissRequest = { expandedGroup = false }) {
                    DropdownMenuItem(text = { Text("Sem Grupo / Ver Todos") }, onClick = { selectedGroupId = null; expandedGroup = false })
                    groups.filter { it.isActive || it.id == selectedGroupId }.forEach { g ->
                        DropdownMenuItem(text = { Text(g.name) }, onClick = { selectedGroupId = g.id; expandedGroup = false })
                    }
                }
            }

            // COMPARTIMENTO (FILTRAR ATIVOS)
            val filteredComps = if (selectedGroupId == null) {
                compartments.filter { it.isActive || it.id == selectedCompId }
            } else {
                compartments.filter { it.groupId == selectedGroupId && (it.isActive || it.id == selectedCompId) }
            }
            
            Text("2. Selecione o Compartimento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            ExposedDropdownMenuBox(
                expanded = expandedComp,
                onExpandedChange = { expandedComp = it },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val compName = filteredComps.find { it.id == selectedCompId }?.name ?: "Selecionar..."
                OutlinedTextField(
                    value = compName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedComp) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SirlimTeal)
                )
                ExposedDropdownMenu(expanded = expandedComp, onDismissRequest = { expandedComp = false }) {
                    filteredComps.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedCompId = c.id!!; expandedComp = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // URGENCIA
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
                        onClick = { urgency = u },
                        label = { Text(u.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor = Color.White,
                            labelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FUNCIONÁRIOS (SÓ UTILIZADORES ATIVOS)
            Text("4. Atribuir a Funcionários (Utilizadores)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    users.filter { it.role == UserRole.USER && (it.isActive || selectedUserIds.contains(it.id)) }.forEach { user ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            selectedUserIds = if (selectedUserIds.contains(user.id)) selectedUserIds - user.id!! else selectedUserIds + user.id!!
                        }.padding(4.dp)) {
                            Checkbox(checked = selectedUserIds.contains(user.id), onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = SirlimTeal))
                            Spacer(modifier = Modifier.width(8.dp))
                            // NOVO: Mostrar Username e Nome Completo
                            val displayName = if (user.fullName.isNullOrBlank()) user.username else "${user.username} (${user.fullName})"
                            Text(displayName, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAREFAS (FILTRAR ATIVAS)
            Text("5. Selecione as Tarefas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    allTasks.filter { it.isActive || selectedTaskIds.contains(it.id) }.forEach { task ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            selectedTaskIds = if (selectedTaskIds.contains(task.id)) selectedTaskIds - task.id!! else selectedTaskIds + task.id!!
                        }.padding(4.dp)) {
                            Checkbox(checked = selectedTaskIds.contains(task.id), onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = SirlimTeal))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(task.name, color = Color.White, fontSize = 14.sp)
                                task.description?.let { Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instruções Adicionais (Opcional)", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SirlimTeal)
            )

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Erro: $errorMsg", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
