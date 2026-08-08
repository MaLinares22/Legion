package com.sounds.legion

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import android.media.audiofx.Visualizer
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import com.sounds.legion.ui.theme.LegionElectricBlue
import com.sounds.legion.ui.theme.LegionHotPink
import com.sounds.legion.ui.theme.LegionPurple
import com.sounds.legion.ui.theme.LegionTheme
import androidx.compose.ui.res.painterResource

private data class MainDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val mainDestinations = listOf(
    MainDestination("feed", "Feed", Icons.Filled.Home),
    MainDestination("upload", "Subir", Icons.Filled.CloudUpload),
    MainDestination("groups", "Grupos", Icons.Filled.Groups),
    MainDestination("favorites", "Favoritos", Icons.Filled.Favorite),
    MainDestination("profile", "Perfil", Icons.Filled.PersonOutline)
)

private val legionBrandGradient = Brush.horizontalGradient(
    listOf(LegionPurple, LegionElectricBlue)
)

@Composable
fun LegionSoundApp() {
    val appState = rememberLegionUiState()

    LegionTheme {
        if (appState.isAuthenticated) {
            MainShell(appState = appState)
        } else {
            AuthScreen(appState = appState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScreen(appState: LegionUiState) {
    var email by remember { mutableStateOf(appState.authForm.email) }
    var password by remember { mutableStateOf(appState.authForm.password) }
    var confirmPassword by remember { mutableStateOf(appState.authForm.confirmPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showRegisterForm by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(legionBrandGradient),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("LegionSound", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                "Tu música, tus grupos, tu ritmo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(28.dp))

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = { appState.signIn(email, password) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Entrar")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { appState.resetPassword(email) }) {
                            Text("¿Olvidaste tu contraseña?")
                        }
                        TextButton(onClick = { showRegisterForm = !showRegisterForm }) {
                            Text(if (showRegisterForm) "Cancelar" else "Registrarme")
                        }
                    }

                    AnimatedVisibility(
                        visible = showRegisterForm,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Text(
                                "Crear cuenta nueva",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirmar contraseña") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Button(
                                onClick = { appState.register(email, password, confirmPassword) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
                            ) {
                                Text("Crear cuenta")
                            }
                        }
                    }

                    if (appState.authMessage.isNotBlank()) {
                        Text(
                            appState.authMessage,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    appState.pendingVerificationEmail?.let { pendingEmail ->
                        Text(
                            text = "El correo $pendingEmail está pendiente de verificación.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(onClick = { appState.resendVerificationEmail() }) {
                            Text("Reenviar correo de verificación")
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(appState: LegionUiState) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "feed"
    val currentDestination = mainDestinations.firstOrNull { it.route == currentRoute } ?: mainDestinations.first()
    var showSignOutDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* el resultado no bloquea el uso de la app; el visualizador se activa solo si se concede audio */ }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(currentDestination.label, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Salir")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                mainDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "feed",
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            composable("feed") {
                FeedScreen(
                    appState = appState,
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("upload") {
                UploadScreen(appState = appState)
            }
            composable("groups") {
                GroupsScreen(appState = appState)
            }
            composable("favorites") {
                FavoritesScreen(
                    appState = appState,
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("profile") {
                ProfileScreen(appState = appState)
            }
            composable("player") {
                PlayerScreen(appState = appState)
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que deseas salir de tu cuenta?") },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    appState.signOut()
                }) {
                    Text("Cerrar sesión")
                }
            }
        )
    }
}

@Composable
private fun FeedScreen(
    appState: LegionUiState,
    onOpenPlayer: () -> Unit
) {
    val songs = appState.feedSongs()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            GenreFilterChips(
                selectedGenre = appState.selectedGenre,
                onGenreSelected = appState::setGenre
            )
            Spacer(Modifier.height(4.dp))
        }

        if (songs.isEmpty()) {
            item {
                PreviewFeatureCard(
                    title = "No hay contenido para este filtro",
                    body = "Prueba con otro género o sube una canción desde la pestaña Subir."
                )
            }
        } else {
            items(songs, key = { it.id }) { song ->
                SongCard(
                    song = song,
                    isFavorite = appState.isFavorite(song.id),
                    isOwner = song.uploaderId == appState.currentUserId,
                    onFavorite = { appState.toggleFavorite(song) },
                    onDelete = { appState.deleteSong(song) },
                    onPlay = {
                        appState.startPlayback(song, songs)
                        onOpenPlayer()
                    }
                )
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreFilterChips(
    selectedGenre: Genre,
    onGenreSelected: (Genre) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        genreOptions.forEach { genre ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = { onGenreSelected(genre) },
                label = { Text(genre.label) }
            )
        }
    }
}

@Composable
private fun SongCard(
    song: LegionSong,
    isFavorite: Boolean,
    isOwner: Boolean,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(legionBrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(song.author, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${song.genre.label} • ${song.visibility.label} • ${song.mediaType.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) LegionHotPink else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isOwner) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Eliminar canción",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reproducir")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar canción") },
            text = { Text("¿Seguro que quieres eliminar \"${song.title}\"? Esta acción no se puede deshacer.") },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Eliminar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadScreen(appState: LegionUiState) {
    val context = LocalContext.current
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            appState.uploadMediaFile(it, MediaType.AUDIO, context)
        }
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            appState.uploadMediaFile(it, MediaType.VIDEO, context)
        }
    }

    val upload = appState.uploadForm

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = upload.title,
            onValueChange = { appState.uploadForm = upload.copy(title = it) },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )

        UploadTypeSelector(
            mediaType = upload.mediaType,
            onAudioClick = {
                appState.uploadForm = upload.copy(mediaType = MediaType.AUDIO)
                pickAudio.launch("audio/*")
            },
            onVideoClick = {
                appState.uploadForm = upload.copy(mediaType = MediaType.VIDEO)
                pickVideo.launch("video/*")
            }
        )

        if (upload.fileName.isNotBlank()) {
            Text("Archivo: ${upload.fileName}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }

        appState.uploadProgress?.let { progress ->
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Subiendo archivo... ${(progress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text("Género musical", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        GenreFilterChips(
            selectedGenre = upload.selectedGenre,
            onGenreSelected = { appState.uploadForm = upload.copy(selectedGenre = it) }
        )

        Text("Visibilidad", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = upload.visibility == VisibilityMode.PRIVATE,
                onClick = {
                    appState.uploadForm = upload.copy(visibility = VisibilityMode.PRIVATE, groupId = "")
                },
                label = { Text(VisibilityMode.PRIVATE.label) }
            )
            FilterChip(
                selected = upload.visibility == VisibilityMode.PUBLIC,
                onClick = {
                    appState.uploadForm = upload.copy(visibility = VisibilityMode.PUBLIC, groupId = "")
                },
                label = { Text(VisibilityMode.PUBLIC.label) }
            )
            FilterChip(
                selected = upload.visibility == VisibilityMode.GROUP,
                onClick = {
                    appState.uploadForm = upload.copy(visibility = VisibilityMode.GROUP)
                },
                label = { Text(VisibilityMode.GROUP.label) }
            )
        }

        if (upload.visibility == VisibilityMode.GROUP) {
            val userGroups = appState.groups.filter { group ->
                group.memberIds.contains(appState.currentUserId)
            }

            if (userGroups.isEmpty()) {
                Text(
                    text = "No perteneces a ningún grupo todavía. Crea uno primero.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                var expanded by remember { mutableStateOf(false) }
                val selectedGroup = userGroups.firstOrNull { it.id == upload.groupId }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "Selecciona un grupo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grupo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor(
                                type = androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    androidx.compose.material3.DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        userGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    appState.uploadForm = upload.copy(groupId = group.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { appState.publishSong() },
            modifier = Modifier.fillMaxWidth(),
            enabled = appState.uploadProgress == null,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Publicar")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun UploadTypeSelector(
    mediaType: MediaType,
    onAudioClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = mediaType == MediaType.AUDIO,
            onClick = onAudioClick,
            label = { Text("MP3") }
        )
        FilterChip(
            selected = mediaType == MediaType.VIDEO,
            onClick = onVideoClick,
            label = { Text("MP4") }
        )
    }
}

@Composable
private fun GroupsScreen(appState: LegionUiState) {
    val groups = appState.groups

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = appState.groupForm.name,
                    onValueChange = { appState.groupForm = appState.groupForm.copy(name = it) },
                    label = { Text("Nuevo grupo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { appState.createGroup() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Crear grupo")
                }
            }
        }

        groups.forEach { group ->
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(legionBrandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Miembros: ${group.memberIds.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { appState.addMemberToGroup(group.id) }, shape = RoundedCornerShape(14.dp)) {
                        Text("Agregarme")
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FavoritesScreen(
    appState: LegionUiState,
    onOpenPlayer: () -> Unit
) {
    val songs = appState.favoriteSongs()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (songs.isEmpty()) {
            item {
                PreviewFeatureCard(
                    title = "Aún no tienes favoritos",
                    body = "Marca canciones en el feed para agregarlas a esta lista."
                )
            }
        } else {
            items(songs, key = { it.id }) { song ->
                SongCard(
                    song = song,
                    isFavorite = true,
                    isOwner = song.uploaderId == appState.currentUserId,
                    onFavorite = { appState.toggleFavorite(song) },
                    onDelete = { appState.deleteSong(song) },
                    onPlay = {
                        appState.startPlayback(song, songs)
                        onOpenPlayer()
                    }
                )
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun ProfileScreen(appState: LegionUiState) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(appState.profile.name) }
    var description by remember { mutableStateOf(appState.profile.description) }
    var career by remember { mutableStateOf(appState.profile.career) }
    var showSaveConfirm by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { appState.uploadProfilePhoto(it, context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (appState.profile.photoUrl.isBlank()) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(legionBrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name.takeIf { it.isNotBlank() }?.firstOrNull()?.toString() ?: "L",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    AsyncImage(
                        model = appState.profile.photoUrl,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                }
                OutlinedButton(
                    onClick = { pickPhoto.launch("image/*") },
                    enabled = appState.profilePhotoUploadProgress == null,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (appState.profilePhotoUploadProgress != null) "Subiendo..." else "Cambiar foto")
                }
                appState.profilePhotoUploadProgress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = career, onValueChange = { career = it }, label = { Text("Carrera") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { showSaveConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Guardar perfil")
                }
            }
        }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = LegionHotPink)
                Column {
                    Text("Favoritos", fontWeight = FontWeight.Bold)
                    Text("Canciones guardadas: ${appState.favoriteSongIds.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = { Text("Guardar cambios") },
            text = { Text("¿Quieres guardar los cambios en tu perfil?") },
            dismissButton = {
                TextButton(onClick = { showSaveConfirm = false }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSaveConfirm = false
                    appState.updateProfile(name, description, career, appState.profile.photoUrl)
                }) { Text("Guardar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun PlayerScreen(appState: LegionUiState) {
    val song = appState.currentSong()
    val playlist = appState.playbackState.playlist
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    // El audioSessionId real solo existe una vez que el audio empieza a sonar.
    // Por eso NO se captura una sola vez: se revisa en cada ciclo del polling de abajo.
    var visualizerSessionId by remember { mutableStateOf(player.audioSessionId) }

    val hasAudioPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    // Portada embebida del MP3 (ID3 APIC), extraída de los metadatos del archivo.
    var albumArt by remember(song?.mediaUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(song?.mediaUrl) {
        albumArt = null
        val url = song?.mediaUrl
        if (url != null && song.mediaType == MediaType.AUDIO) {
            albumArt = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                runCatching {
                    retriever.setDataSource(url, HashMap<String, String>())
                    val bytes = retriever.embeddedPicture
                    bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
                }.getOrNull().also {
                    runCatching { retriever.release() }
                }
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                appState.playbackState = appState.playbackState.copy(isPlaying = isPlaying)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(song?.id, playlist) {
        if (playlist.isNotEmpty()) {
            val mediaItems = playlist.map { MediaItem.fromUri(it.mediaUrl) }
            val currentIndex = playlist.indexOfFirst { it.id == song?.id }.coerceAtLeast(0)
            player.setMediaItems(mediaItems, currentIndex, 0L)
            player.prepare()
            if (appState.playbackState.isPlaying) {
                player.play()
            }
        }
    }

    LaunchedEffect(appState.playbackState.playbackMode) {
        player.repeatMode = when (appState.playbackState.playbackMode) {
            PlaybackMode.LINEAR -> Player.REPEAT_MODE_OFF
            PlaybackMode.SHUFFLE -> Player.REPEAT_MODE_OFF
            PlaybackMode.ONE -> Player.REPEAT_MODE_ONE
            PlaybackMode.LOOP -> Player.REPEAT_MODE_ALL
        }
        player.shuffleModeEnabled = appState.playbackState.playbackMode == PlaybackMode.SHUFFLE
    }

    LaunchedEffect(player) {
        while (isActive) {
            position = player.currentPosition.toFloat()
            duration = max(player.duration, 0L).toFloat()
            val liveSessionId = player.audioSessionId
            if (liveSessionId != 0 && liveSessionId != visualizerSessionId) {
                visualizerSessionId = liveSessionId
            }
            delay(300.milliseconds)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(song?.title ?: "Selecciona una pista", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(song?.author ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(song?.genre?.label ?: "", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)

                if (song?.mediaType == MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AndroidView(
                            factory = {
                                PlayerView(it).apply {
                                    this.player = player
                                    useController = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                PlaybackProgress(
                    position = position,
                    duration = duration,
                    onSeek = { newPosition -> player.seekTo(newPosition.toLong()) }
                )

                PlayerControls(
                    isPlaying = appState.playbackState.isPlaying,
                    onPrevious = {
                        appState.previousSong()
                        val refreshedSong = appState.currentSong()
                        if (refreshedSong != null) {
                            player.seekToDefaultPosition(playlist.indexOfFirst { it.id == refreshedSong.id }.coerceAtLeast(0))
                            player.play()
                        }
                    },
                    onPlayPause = {
                        appState.togglePlayback()
                        if (appState.playbackState.isPlaying) player.play() else player.pause()
                    },
                    onNext = {
                        appState.nextSong()
                        val refreshedSong = appState.currentSong()
                        if (refreshedSong != null) {
                            player.seekToDefaultPosition(playlist.indexOfFirst { it.id == refreshedSong.id }.coerceAtLeast(0))
                            player.play()
                        }
                    }
                )

                PlaybackModeButton(
                    mode = appState.playbackState.playbackMode,
                    onSelected = appState::setPlaybackMode
                )

                if (song?.mediaType != MediaType.VIDEO) {
                    WaveformVisualizer(
                        sessionId = visualizerSessionId,
                        hasAudioPermission = hasAudioPermission,
                        albumArt = albumArt
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackProgress(
    position: Float,
    duration: Float,
    onSeek: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Slider(
            value = position.coerceAtMost(duration.coerceAtLeast(1f)),
            onValueChange = onSeek,
            valueRange = 0f..duration.coerceAtLeast(1f),
            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMillis(position.toLong()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(formatMillis(duration.toLong()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(32.dp))
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(legionBrandGradient),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(32.dp))
        }
    }
}

// Botón único que va cambiando el modo de reproducción: lineal -> aleatorio -> bucle -> lineal...
private fun PlaybackMode.cycled(): PlaybackMode = when (this) {
    PlaybackMode.LINEAR -> PlaybackMode.SHUFFLE
    PlaybackMode.SHUFFLE -> PlaybackMode.LOOP
    PlaybackMode.LOOP -> PlaybackMode.LINEAR
    PlaybackMode.ONE -> PlaybackMode.LINEAR
}

@Composable
private fun PlaybackModeButton(
    mode: PlaybackMode,
    onSelected: (PlaybackMode) -> Unit
) {
    val icon = when (mode) {
        PlaybackMode.LINEAR -> Icons.Filled.QueueMusic
        PlaybackMode.SHUFFLE -> Icons.Filled.Shuffle
        PlaybackMode.LOOP -> Icons.Filled.Repeat
        PlaybackMode.ONE -> Icons.Filled.Repeat
    }
    OutlinedButton(
        onClick = { onSelected(mode.cycled()) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Modo: ${mode.label} · toca para cambiar")
    }
}

// Visualizador de ondas real: captura el audio en reproducción con Visualizer
// y anima las barras según la amplitud real de la señal (no es una animación falsa).
// Requiere permiso RECORD_AUDIO en tiempo de ejecución para funcionar en dispositivos modernos;
// de lo contrario la API de Android no entrega datos y las barras se quedan planas.
// Detrás de las barras se muestra la portada embebida del MP3 (si el archivo la trae en sus metadatos).
@Composable
private fun WaveformVisualizer(
    sessionId: Int?,
    hasAudioPermission: Boolean,
    albumArt: ImageBitmap?
) {
    val barCount = 24
    val amplitudes = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0.12f) } } }

    DisposableEffect(sessionId, hasAudioPermission) {
        var visualizer: Visualizer? = null
        if (hasAudioPermission && sessionId != null && sessionId != 0) {
            try {
                visualizer = Visualizer(sessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1]
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                visualizer: Visualizer,
                                waveform: ByteArray,
                                samplingRate: Int
                            ) {
                                if (waveform.isEmpty()) return
                                val step = (waveform.size / barCount).coerceAtLeast(1)
                                for (i in 0 until barCount) {
                                    val index = (i * step).coerceIn(0, waveform.size - 1)
                                    val raw = (waveform[index].toInt() and 0xFF) - 128
                                    val normalized = (abs(raw) / 128f).coerceIn(0.08f, 1f)
                                    if (i < amplitudes.size) amplitudes[i] = normalized
                                }
                            }

                            override fun onFftDataCapture(
                                visualizer: Visualizer,
                                fft: ByteArray,
                                samplingRate: Int
                            ) = Unit
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        true,
                        false
                    )
                    enabled = true
                }
            } catch (e: Exception) {
                // Sesión de audio aún no disponible (por ejemplo, antes de que empiece a sonar algo).
                // Se deja el visualizador en su estado por defecto sin romper la pantalla.
            }
        }
        onDispose {
            visualizer?.release()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (albumArt != null) {
            Image(
                bitmap = albumArt,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Capa oscura para que las barras del ecualizador resalten sobre la portada.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val gap = 6.dp.toPx()
            val barWidth = (size.width - gap * (barCount - 1)) / barCount
            amplitudes.forEachIndexed { index, amplitude ->
                val barHeight = size.height * amplitude
                val x = index * (barWidth + gap)
                drawRoundRect(
                    color = if (index % 2 == 0) primaryColor else secondaryColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }

        if (!hasAudioPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Concede el permiso de audio para ver el ecualizador",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewFeatureCard(title: String, body: String) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatMillis(value: Long): String {
    val totalSeconds = (value / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}