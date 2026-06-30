package pt.sirlim.app.ui.screens.admin.groups_compartments

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.data.model.Compartment
import pt.sirlim.app.data.model.Group
import pt.sirlim.app.data.model.Task
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompartmentFormScreen(
    compartment: Compartment? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GroupsCompartmentsViewModel = viewModel()
    val groups by viewModel.groups.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var name by remember(compartment) { mutableStateOf(compartment?.name ?: "") }
    var description by remember(compartment) { mutableStateOf(compartment?.description ?: "") }
    var selectedGroupId by remember(compartment) { mutableStateOf(compartment?.groupId) }
    var isActive by remember(compartment) { mutableStateOf(compartment?.isActive ?: true) }
    var selectedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    var expandedGroups by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(compartment) {
        compartment?.id?.let { id ->
            val ids = viewModel.getCompartmentTasks(id)
            selectedTaskIds = ids.toSet()
        }
    }

    val qrCodeKey = remember(name, selectedGroupId) {
        val groupName = groups.find { it.id == selectedGroupId }?.name
        viewModel.generateQrCodeKey(name, groupName)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Apagar Compartimento") },
            text = { Text("Deseja apagar permanentemente este compartimento? Só será possível se não houverem limpezas associadas.") },
            confirmButton = {
                TextButton(onClick = {
                    compartment?.let {
                        viewModel.deleteCompartment(it) { success ->
                            if (success) {
                                Toast.makeText(context, "Apagado!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, error ?: "Erro ao apagar", Toast.LENGTH_LONG).show()
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
                title = { Text(if (compartment == null) "Novo Compartimento" else "Editar Compartimento", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    if (compartment != null) {
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
                    if (name.isNotBlank()) {
                        val newComp = Compartment(
                            id = compartment?.id,
                            name = name,
                            groupId = selectedGroupId,
                            description = description,
                            qrCodeKey = qrCodeKey,
                            isActive = isActive
                        )
                        viewModel.saveCompartment(newComp, selectedTaskIds.toList()) { success ->
                            if (success) {
                                Toast.makeText(context, "Guardado com sucesso!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
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
                .padding(24.dp)
        ) {
            Text("Detalhes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Compartimento", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SirlimTeal)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expandedGroups,
                onExpandedChange = { expandedGroups = it }
            ) {
                val groupName = groups.find { it.id == selectedGroupId }?.name ?: "Sem Grupo"
                OutlinedTextField(
                    value = groupName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Grupo", color = Color.White.copy(alpha = 0.6f)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGroups) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SirlimTeal)
                )
                ExposedDropdownMenu(
                    expanded = expandedGroups,
                    onDismissRequest = { expandedGroups = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sem Grupo") },
                        onClick = {
                            selectedGroupId = null
                            expandedGroups = false
                        }
                    )
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                selectedGroupId = group.id
                                expandedGroups = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição (Opcional)", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SirlimTeal)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Compartimento Ativo", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = isActive, onCheckedChange = { isActive = it }, colors = SwitchDefaults.colors(checkedThumbColor = SirlimTeal))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Chave QR Code:", color = SirlimTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = qrCodeKey, color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Tarefas Obrigatórias", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            allTasks.forEach { task ->
                val isSelected = selectedTaskIds.contains(task.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTaskIds = if (isSelected) {
                                selectedTaskIds - task.id!!
                            } else {
                                selectedTaskIds + task.id!!
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = SirlimTeal,
                            uncheckedColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = task.name, color = Color.White, fontWeight = FontWeight.Medium)
                        task.description?.let { Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) }
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = error!!, color = Color.Red, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
