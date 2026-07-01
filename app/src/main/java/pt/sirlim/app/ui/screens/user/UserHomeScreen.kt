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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.ui.screens.login.LoginViewModel
import pt.sirlim.app.data.model.UserRole
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
    val loginViewModel: LoginViewModel = viewModel()
    val indications by indicationsViewModel.indications.collectAsState()
    val users by loginViewModel.users.collectAsState()
    
    val currentUser = users.find { it.id == userId }
    val isViewer = currentUser?.role == UserRole.VIEWER

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
            if (isViewer) {
                Spacer(modifier = Modifier.height(24.dp))
                // Perfil Leitura: Apenas Consultas (Geral/Admin style)
                UserActionCard(
                    title = "Consultas Gerais",
                    icon = Icons.Default.Search,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("viewer_consultations") }
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                
                UserActionCard(
                    title = "Leitura",
                    icon = Icons.Default.QrCodeScanner,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SirlimTeal,
                    hasInnerGlow = true,
                    onClick = { onNavigate("scanner") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                UserActionCard(
                    title = "Escolha Manual Compartimento",
                    icon = Icons.Default.List,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("manual_selection") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    UserActionCard(
                        title = "Consultas",
                        icon = Icons.Default.Search,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("user_consultations") }
                    )
                    
                    UserActionCard(
                        title = "Indicações",
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f),
                        hasGlowOnLine = hasPending,
                        badgeCount = if (hasPending) pendingCount else null,
                        onClick = { onNavigate("user_indications") }
                    )
                }
            }
        }
    }
}

@Composable
fun UserActionCard(
    title: String, 
    icon: ImageVector, 
    modifier: Modifier = Modifier, 
    backgroundColor: Color? = null,
    hasInnerGlow: Boolean = false,
    hasGlowOnLine: Boolean = false,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lineGlow")
    val lineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineAlpha"
    )

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor ?: Color.White.copy(alpha = 0.15f)
            ),
            border = if (hasGlowOnLine) 
                androidx.compose.foundation.BorderStroke(2.dp, SirlimTeal.copy(alpha = lineAlpha))
            else 
                androidx.compose.foundation.BorderStroke(1.dp, SirlimTeal.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Inner Glow Cristalino (Apenas para Leitura)
                if (hasInnerGlow) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                                    radius = 450f
                                ),
                                RoundedCornerShape(18.dp)
                            )
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon, 
                        null, 
                        tint = if (backgroundColor != null) SirlimBlue else SirlimTeal, 
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        title, 
                        color = if (backgroundColor != null) SirlimBlue else Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Círculo com o número de indicações
        if (badgeCount != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(26.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = Color.Red,
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(badgeCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
