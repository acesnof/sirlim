package pt.sirlim.app.ui.screens.user

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.ui.screens.admin.indications.IndicationsViewModel
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    userId: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val indicationsViewModel: IndicationsViewModel = viewModel()
    val indications by indicationsViewModel.indications.collectAsState()
    
    LaunchedEffect(userId) {
        indicationsViewModel.fetchUserIndications(userId)
    }

    val pendingCount = indications.count { !it.isCompleted }
    val hasPending = pendingCount > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu Principal", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Sair", tint = Color.White)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            UserMainActionCard(
                title = "Leitura",
                subtitle = "Ler QR Code de Compartimento",
                icon = Icons.Default.QrCodeScanner,
                onClick = { onNavigate("scanner") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            UserSecondaryActionCard(
                title = "Escolha Manual Compartimento",
                icon = Icons.Default.List,
                onClick = { onNavigate("manual_selection") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                UserActionCard(
                    title = "Consultas",
                    icon = Icons.Default.Search,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("user_consultations") }
                )
                
                // Botão de Indicações com Glow e Contador
                UserActionCard(
                    title = "Indicações",
                    icon = Icons.Default.Assignment,
                    modifier = Modifier.weight(1f),
                    hasGlow = hasPending,
                    badgeCount = if (hasPending) pendingCount else null,
                    onClick = { onNavigate("user_indications") }
                )
            }
        }
    }
}

@Composable
fun UserMainActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SirlimTeal),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = SirlimBlue, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = SirlimBlue)
            Text(subtitle, fontSize = 12.sp, color = SirlimBlue.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun UserSecondaryActionCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, SirlimTeal.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = SirlimTeal, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun UserActionCard(
    title: String, 
    icon: ImageVector, 
    modifier: Modifier = Modifier, 
    hasGlow: Boolean = false,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = modifier) {
        if (hasGlow) {
            // Glow effect
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(elevation = 12.dp * glowAlpha, shape = RoundedCornerShape(20.dp), ambientColor = SirlimTeal, spotColor = SirlimTeal)
                    .background(SirlimTeal.copy(alpha = 0.1f * glowAlpha), RoundedCornerShape(20.dp))
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasGlow) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f)
            ),
            border = if (hasGlow) androidx.compose.foundation.BorderStroke(1.dp, SirlimTeal.copy(alpha = glowAlpha)) else null
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = if (hasGlow) Color.White else SirlimTeal, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (badgeCount != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(24.dp),
                shape = CircleShape,
                color = Color.Red,
                contentColor = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(badgeCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
