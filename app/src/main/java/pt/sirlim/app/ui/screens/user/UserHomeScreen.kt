package pt.sirlim.app.ui.screens.user

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
            
            // Botão LEITURA - Estilo Premium Sólido (Restaurado)
            UserMainActionCard(
                title = "Leitura",
                subtitle = "Ler QR Code de Compartimento",
                icon = Icons.Default.QrCodeScanner,
                onClick = { onNavigate("scanner") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Escolha Manual Compartimento
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
                
                // Botão INDICAÇÕES - Com Glow Dinâmico que percorre o botão inteiro
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
            .clickable { onClick() }
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = SirlimTeal.copy(alpha = 0.5f)),
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
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
    
    // Animação de posição que percorre o botão inteiro horizontalmente
    val xOffsetPos by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xOffsetPos"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .shadow(
                    if (hasGlow) 12.dp else 0.dp, 
                    RoundedCornerShape(20.dp), 
                    spotColor = SirlimTeal.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .then(
                    if (hasGlow) Modifier.drawBehind {
                        // GLOW QUE ANDA DENTRO DO BOTÃO COMPLETO
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(SirlimTeal.copy(alpha = glowAlpha), Color.Transparent),
                                center = Offset(size.width * xOffsetPos, size.height / 2),
                                radius = size.width * 1.2f
                            )
                        )
                    } else Modifier
                )
                .border(
                    if (hasGlow) 1.5.dp else 1.dp, 
                    if (hasGlow) SirlimTeal.copy(alpha = glowAlpha + 0.2f) else Color.White.copy(alpha = 0.15f), 
                    RoundedCornerShape(20.dp)
                )
                .clickable { onClick() }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon, 
                    null, 
                    tint = if (hasGlow) Color.White else SirlimTeal, 
                    modifier = Modifier.size(32.dp).then(
                        if (hasGlow) Modifier.graphicsLayer(scaleX = 1.1f, scaleY = 1.1f) else Modifier
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    title, 
                    color = Color.White, 
                    fontWeight = if (hasGlow) FontWeight.Black else FontWeight.Bold, 
                    fontSize = 16.sp
                )
            }
        }

        if (badgeCount != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(28.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = Color.Red,
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(badgeCount.toString(), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
