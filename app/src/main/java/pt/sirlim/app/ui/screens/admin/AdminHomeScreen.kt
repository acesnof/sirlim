package pt.sirlim.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Administração", fontWeight = FontWeight.Bold, color = Color.White) 
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SirlimBlue
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
        ) {
            // Icon decorativo em fundo (muito transparente)
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .graphicsLayer(alpha = 0.05f),
                tint = Color.White
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top // Encostado ao topo
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. PRIMEIRA LINHA: APLICAÇÃO E CONSULTAS
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AdminActionBox(
                        title = "Aplicação",
                        icon = Icons.Default.Settings,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("admin_application") }
                    )
                    AdminActionBox(
                        title = "Consultas",
                        icon = Icons.Default.Search,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("admin_consultations") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. SEGUNDA LINHA: PROGRAMAÇÃO E INDICAÇÕES
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AdminActionBox(
                        title = "Programação",
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("admin_scheduling") }
                    )
                    AdminActionBox(
                        title = "Indicações",
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("admin_indications") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. TERCEIRA LINHA: RELATÓRIOS (LARGURA TODA, APENAS LINHA)
                AdminActionBox(
                    title = "Relatórios",
                    icon = Icons.Default.Assessment,
                    isOutlined = true,
                    onClick = { onNavigate("admin_reports") }
                )
            }
        }
    }
}

@Composable
fun AdminActionBox(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(20.dp))
            .background(if (isOutlined) Color.Transparent else Color.White.copy(alpha = 0.12f))
            .then(
                if (isOutlined) Modifier.border(2.dp, SirlimTeal, RoundedCornerShape(20.dp))
                else Modifier.border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            )
            .clickable { onClick() }
    ) {
        if (!isOutlined) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SirlimTeal,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Mantendo para compatibilidade com AdminApplicationMenu
data class AdminMenuItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun AdminMenuCard(item: AdminMenuItem, onClick: () -> Unit) {
    AdminActionBox(
        title = item.title,
        icon = item.icon,
        onClick = onClick
    )
}
