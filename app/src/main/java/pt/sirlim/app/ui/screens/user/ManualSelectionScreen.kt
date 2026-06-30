package pt.sirlim.app.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import pt.sirlim.app.data.model.Compartment
import pt.sirlim.app.ui.screens.admin.groups_compartments.GroupsCompartmentsViewModel
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSelectionScreen(
    onCompartmentSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: GroupsCompartmentsViewModel = viewModel()
    val groups by viewModel.groups.collectAsState()
    val compartments by viewModel.compartments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escolha Manual", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SirlimBlue)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SirlimTeal)
            } else {
                // FILTRAR APENAS ATIVOS
                val activeCompartments = compartments.filter { it.isActive }
                
                if (activeCompartments.isEmpty()) {
                    Text(
                        "Nenhum compartimento ativo encontrado.",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val grouped = activeCompartments.groupBy { comp ->
                            groups.find { it.id == comp.groupId }?.name ?: "Sem Grupo"
                        }

                        grouped.forEach { (groupName, compList) ->
                            item {
                                Text(
                                    text = groupName,
                                    color = SirlimTeal,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(compList) { comp ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCompartmentSelected(comp.qrCodeKey) },
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = comp.name,
                                            fontWeight = FontWeight.Bold,
                                            color = SirlimDarkBlue,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("Selecionar >", color = SirlimTeal, fontSize = 12.sp)
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
