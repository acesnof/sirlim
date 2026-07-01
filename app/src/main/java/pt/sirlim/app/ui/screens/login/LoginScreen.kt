package pt.sirlim.app.ui.screens.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import pt.sirlim.app.R
import pt.sirlim.app.data.model.User
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (User) -> Unit
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedUser by remember { mutableStateOf<User?>(null) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(SirlimBlue, SirlimDarkBlue)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Elementos decorativos de profundidade
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = 180.dp, y = (-120).dp)
                .graphicsLayer(alpha = 0.08f)
                .background(SirlimTeal, CircleShape)
        )

        AnimatedContent(
            targetState = selectedUser,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                } using SizeTransform(clip = false)
            },
            label = "LoginContent"
        ) { user ->
            if (user == null) {
                // FILTRAR UTILIZADORES ATIVOS PARA O LOGIN
                val activeUsers = users.filter { it.isActive }
                UserSelectionList(
                    users = activeUsers,
                    isLoading = isLoading,
                    error = error,
                    onRetry = { viewModel.fetchUsers() },
                    onUserSelected = { selectedUser = it }
                )
            } else {
                PinEntryScreen(
                    user = user,
                    onPinVerified = { onLoginSuccess(user) },
                    onBack = { selectedUser = null }
                )
            }
        }
    }
}

@Composable
fun UserSelectionList(
    users: List<User>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onUserSelected: (User) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        
        LogoHeader()

        Spacer(modifier = Modifier.height(30.dp))
        
        if (error != null) {
            Box(modifier = Modifier.padding(horizontal = 28.dp)) {
                ErrorCard(error = error, onRetry = onRetry)
            }
        } else {
            Text(
                text = "BEM-VINDO",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            
            Text(
                text = "Selecione o seu perfil",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isLoading) {
                CircularProgressIndicator(color = SirlimTeal, modifier = Modifier.size(40.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 40.dp)
                ) {
                    items(users) { user ->
                        UserCardPremium(user = user, onClick = { onUserSelected(user) })
                    }
                }
            }
        }
    }
}

@Composable
fun LogoHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(20.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.fillMaxSize().padding(10.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.height(18.dp))
        
        Text(
            text = "SIRLIM",
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 6.sp
        )
        
        // Retângulo com Inner Glow efeito solicitado
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .background(SirlimTeal, RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = "SISTEMA DE REGISTO DE LIMPEZAS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SirlimDarkBlue,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Versão 1.1 @ dev by SCh Tm Filipe Fonseca | 2026",
            fontSize = 9.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun UserCardPremium(user: User, onClick: () -> Unit) {
    // Efeito Glassmorphism com Inner Glow
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp), // Altura reduzida conforme pedido
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.12f), // Mais cristalino
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto / Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, SirlimTeal.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.photoUrl != null && user.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SirlimTeal,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.username.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SirlimTeal,
                        letterSpacing = 1.sp
                    )
                    
                    if (user.fullName != null && user.fullName.isNotBlank()) {
                        Text(
                            text = user.fullName,
                            fontSize = 16.sp, // Letra mais pequena
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    
                    user.description?.let {
                        Text(
                            text = it,
                            fontSize = 10.sp, // Letra mais pequena
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = SirlimTeal.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp).graphicsLayer(rotationZ = -90f)
                )
            }
        }
    }
}

@Composable
fun ErrorCard(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Erro de Ligação", color = Color.White, fontWeight = FontWeight.Bold)
            Text(error, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = SirlimTeal)) {
                Text("Tentar Novamente", color = SirlimBlue)
            }
        }
    }
}

@Composable
fun PinEntryScreen(
    user: User,
    onPinVerified: () -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(errorMsg) {
        if (errorMsg != null) {
            repeat(3) {
                shakeOffset.animateTo(15f, animationSpec = tween(50))
                shakeOffset.animateTo(-15f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.15f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Box(
            modifier = Modifier.size(90.dp).clip(CircleShape).background(SirlimTeal.copy(alpha = 0.1f)).border(1.dp, SirlimTeal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (user.photoUrl != null && user.photoUrl.isNotBlank()) {
                AsyncImage(model = user.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = user.fullName ?: user.username, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
        Text(text = "INTRODUZA O SEU PIN", fontSize = 10.sp, color = SirlimTeal, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.offset(x = shakeOffset.value.dp)) {
            repeat(4) { index ->
                val isActive = index < pin.length
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(if (isActive) SirlimTeal else Color.White.copy(alpha = 0.15f)).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        if (errorMsg != null) {
            Text(text = errorMsg!!, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("Limpar", "0"))
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    row.forEach { text ->
                        PinDigitButtonPremium(text = text) {
                            if (text == "Limpar") { pin = ""; errorMsg = null }
                            else if (pin.length < 4) {
                                pin += text
                                if (pin.length == 4) {
                                    if (pin == user.pin) onPinVerified()
                                    else { errorMsg = "PIN INCORRETO"; pin = "" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinDigitButtonPremium(text: String, onClick: () -> Unit) {
    val isAction = text == "Limpar"
    Surface(
        onClick = onClick,
        modifier = Modifier.size(if (isAction) 95.dp else 75.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        contentColor = Color.White,
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = if (isAction) 15.sp else 26.sp, fontWeight = FontWeight.Light)
        }
    }
}
