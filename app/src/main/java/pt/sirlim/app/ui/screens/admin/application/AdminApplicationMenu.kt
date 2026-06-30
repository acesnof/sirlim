package pt.sirlim.app.ui.screens.admin.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.sirlim.app.ui.screens.admin.AdminMenuCard
import pt.sirlim.app.ui.screens.admin.AdminMenuItem
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApplicationMenu(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aplicação", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            val items = listOf(
                AdminMenuItem("Base de Dados", Icons.Default.Storage, "admin_db_settings"),
                AdminMenuItem("Contas", Icons.Default.People, "admin_accounts"),
                AdminMenuItem("Tarefas", Icons.Default.List, "admin_tasks"),
                AdminMenuItem("Grupos / Comp.", Icons.Default.HomeWork, "admin_groups_compartments")
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    AdminMenuCard(item = item) {
                        onNavigate(item.route)
                    }
                }
            }
        }
    }
}
