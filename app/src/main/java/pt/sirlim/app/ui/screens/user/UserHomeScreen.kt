package pt.sirlim.app.ui.screens.user

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.sirlim.app.data.model.UserRole
import pt.sirlim.app.ui.screens.admin.indications.IndicationsViewModel
import pt.sirlim.app.ui.screens.login.LoginViewModel
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

    val today = java.time.LocalDate.now().toString()
    val pendingCount = indications.count { !it.isCompleted && it.scheduledDate == today }
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
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Brush.verticalGradient(listOf(SirlimBlue, SirlimDarkBlue)))
        ) {
            // Icon decorativo em fundo
            Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-80).dp, y = 80.dp)
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

                if (isViewer) {
                    UserActionBoxPremium(
                        title = "Consultas Gerais",
                        icon = Icons.Default.Search,
                        onClick = { onNavigate("viewer_consultations") }
                    )
                } else {
                    // 1. LEITURA (VERDE CLARINHO)
                    UserActionBoxPremium(
                        title = "Leitura",
                        subtitle = "Ler QR Code do compartimento",
                        icon = Icons.Default.QrCodeScanner,
                        backgroundColor = SirlimTeal.copy(alpha = 0.5f),
                        onClick = { onNavigate("scanner") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. ESCOLHA MANUAL (LARGURA TODA)
                    UserActionBoxPremium(
                        title = "Escolha Manual Compartimento",
                        icon = Icons.Default.List,
                        onClick = { onNavigate("manual_selection") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. SEMANA E INDICAÇÕES (LADO A LADO)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        UserActionBoxPremium(
                            title = "Semana",
                            icon = Icons.Default.DateRange,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("user_weekly") }
                        )
                        UserActionBoxPremium(
                            title = "Indicações",
                            icon = Icons.Default.Assignment,
                            modifier = Modifier.weight(1f),
                            badgeCount = if (hasPending) pendingCount else null,
                            onClick = { onNavigate("user_indications") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. CONSULTAS (APENAS BORDER)
                    UserActionBoxPremium(
                        title = "Consultas",
                        icon = Icons.Default.Search,
                        isOutlined = true,
                        onClick = { onNavigate("user_consultations") }
                    )
                }
            }
        }
    }
}

@Composable
fun UserActionBoxPremium(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    isOutlined: Boolean = false,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (subtitle != null) 140.dp else 125.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(20.dp))
            .background(if (isOutlined) Color.Transparent else (backgroundColor ?: Color.White.copy(alpha = 0.12f)))
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
                tint = Color.White,
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (badgeCount != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp),
                shape = CircleShape,
                color = Color.Red,
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = badgeCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
