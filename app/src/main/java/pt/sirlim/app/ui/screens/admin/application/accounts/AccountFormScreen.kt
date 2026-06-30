package pt.sirlim.app.ui.screens.admin.application.accounts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import pt.sirlim.app.data.model.User
import pt.sirlim.app.data.model.UserRole
import pt.sirlim.app.ui.theme.SirlimBlue
import pt.sirlim.app.ui.theme.SirlimDarkBlue
import pt.sirlim.app.ui.theme.SirlimTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    user: User? = null,
    onBack: () -> Unit
) {
    val viewModel: AccountsViewModel = viewModel()
    
    var username by remember(user) { mutableStateOf(user?.username ?: "") }
    var pin by remember(user) { mutableStateOf(user?.pin ?: "") }
    var fullName by remember(user) { mutableStateOf(user?.fullName ?: "") }
    var description by remember(user) { mutableStateOf(user?.description ?: "") }
    var role by remember(user) { mutableStateOf(user?.role ?: UserRole.USER) }
    var isActive by remember(user) { mutableStateOf(user?.isActive ?: true) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val isSaving by viewModel.isSaving.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val errorMsg by viewModel.error.collectAsState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Apagar Conta") },
            text = { Text("Tem a certeza que deseja apagar permanentemente este utilizador?") },
            confirmButton = {
                TextButton(onClick = {
                    user?.let {
                        viewModel.deleteUser(it) { success ->
                            if (success) {
                                showDeleteDialog = false
                                onBack()
                            }
                        }
                    }
                }) {
                    Text("APAGAR", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (user == null) "Nova Conta" else "Editar Conta", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    if (user != null) {
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
                    val newUser = User(
                        id = user?.id,
                        username = username,
                        pin = pin,
                        fullName = fullName,
                        description = description,
                        role = role,
                        isActive = isActive,
                        photoUrl = user?.photoUrl
                    )
                    viewModel.saveUser(newUser, selectedImageUri, context.contentResolver) { success ->
                        if (success) onBack()
                    }
                },
                containerColor = SirlimTeal,
                contentColor = SirlimBlue
            ) {
                if (isSaving || isDeleting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SirlimBlue)
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo Selection - REDUCED SIZE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp) // Reduced from 120dp
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { 
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (user?.photoUrl != null && user.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.CameraAlt, "Foto", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Escolher Foto", color = SirlimTeal, fontSize = 14.sp)
            }

            // Form Fields - Compacted spacing
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nome de Utilizador", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SirlimTeal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it },
                label = { Text("PIN (4 dígitos)", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SirlimTeal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nome Completo", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SirlimTeal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição", color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SirlimTeal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Role Selection
            Text("Privilégios", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                UserRole.entries.forEach { roleOption ->
                    val roleNamePt = when(roleOption) {
                        UserRole.ADMIN -> "Admin"
                        UserRole.USER -> "Utilizador"
                        UserRole.VIEWER -> "Leitura"
                    }
                    FilterChip(
                        selected = role == roleOption,
                        onClick = { role = roleOption },
                        label = { Text(roleNamePt, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = Color.White,
                            selectedContainerColor = SirlimTeal,
                            selectedLabelColor = SirlimBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Utilizador Ativo", color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = SirlimTeal, checkedTrackColor = SirlimTeal.copy(alpha = 0.5f))
                )
            }

            if (errorMsg != null) {
                Text(text = errorMsg!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
