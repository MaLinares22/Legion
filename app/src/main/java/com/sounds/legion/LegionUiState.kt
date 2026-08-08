@file:Suppress("USELESS_ELVIS", "UNNECESSARY_SAFE_CALL")

package com.sounds.legion

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@Stable
class LegionUiState {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    private val songsRef = database.child("canciones")
    private val groupsRef = database.child("grupos")

    private var songsListener: ValueEventListener? = null
    private var groupsListener: ValueEventListener? = null
    private var favoritesListener: ValueEventListener? = null
    private var favoritesRef: DatabaseReference? = null

    var currentUserId by mutableStateOf(auth.currentUser?.uid.orEmpty())
    var pendingVerificationEmail by mutableStateOf<String?>(null)

    var isAuthenticated by mutableStateOf(false)
    var authMessage by mutableStateOf("Inicia sesión o crea una cuenta para entrar a LegionSound.")
    var selectedGenre by mutableStateOf(Genre.ALL)
    var profile by mutableStateOf(sampleProfile())
    var authForm by mutableStateOf(AuthFormState())
    var uploadForm by mutableStateOf(UploadFormState())
    var groupForm by mutableStateOf(GroupFormState())
    var playbackState by mutableStateOf(PlaybackState())
    var uploadProgress by mutableStateOf<Float?>(null)
    var profilePhotoUploadProgress by mutableStateOf<Float?>(null)

    val songs = mutableStateListOf<LegionSong>()
    val groups = mutableStateListOf<LegionGroup>()
    val favoriteSongIds = mutableStateListOf<String>()

    init {
        attachSongsListener()
        attachGroupsListener()
        val currentUser = auth.currentUser
        if (currentUser?.isEmailVerified == true) {
            isAuthenticated = true
            updateCurrentUserId(currentUser.uid)
            pendingVerificationEmail = null
            saveFcmToken(currentUser.uid)
        } else {
            isAuthenticated = false
            updateCurrentUserId("")
            pendingVerificationEmail = currentUser?.email
            if (currentUser != null) {
                authMessage = "Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada."
            }
        }
    }

    fun signIn(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            authMessage = "Debes completar correo y contraseña."
            return
        }
        auth.signInWithEmailAndPassword(normalizedEmail, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    attachSongsListener()
                    attachGroupsListener()
                    updateCurrentUserId(user.uid)
                    pendingVerificationEmail = null
                    isAuthenticated = true
                    authMessage = "Sesión iniciada como ${user.email ?: email}."
                    saveFcmToken(user.uid)
                } else {
                    updateCurrentUserId("")
                    isAuthenticated = false
                    pendingVerificationEmail = user?.email ?: email
                    authMessage = "Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada."
                }
            }
            .addOnFailureListener { exception ->
                pendingVerificationEmail = null
                updateCurrentUserId("")
                authMessage = exception.localizedMessage ?: "Correo o contraseña incorrectos."
            }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        val normalizedEmail = email.trim()
        when {
            normalizedEmail.isBlank() || password.isBlank() -> authMessage = "Completa correo y contraseña para registrarte."
            password != confirmPassword -> authMessage = "Las contraseñas no coinciden."
            password.length < 6 -> authMessage = "La contraseña debe tener al menos 6 caracteres."
            else -> {
                auth.createUserWithEmailAndPassword(normalizedEmail, password)
                    .addOnSuccessListener {
                        auth.currentUser?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                pendingVerificationEmail = auth.currentUser?.email ?: normalizedEmail
                                updateCurrentUserId("")
                                isAuthenticated = false
                                authMessage = "Cuenta creada. Revisa tu correo y verifica tu cuenta antes de iniciar sesión."
                            }
                            ?.addOnFailureListener { exception ->
                                pendingVerificationEmail = auth.currentUser?.email ?: normalizedEmail
                                updateCurrentUserId("")
                                isAuthenticated = false
                                authMessage = exception.localizedMessage ?: "No se pudo enviar el correo de verificación."
                            }
                            ?: run {
                                pendingVerificationEmail = auth.currentUser?.email ?: normalizedEmail
                                updateCurrentUserId("")
                                isAuthenticated = false
                                authMessage = "Cuenta creada. Revisa tu correo y verifica tu cuenta antes de iniciar sesión."
                            }
                    }
                    .addOnFailureListener { exception ->
                        pendingVerificationEmail = null
                        updateCurrentUserId("")
                        isAuthenticated = false
                        authMessage = exception.localizedMessage ?: "No se pudo crear la cuenta."
                    }
            }
        }
    }

    fun resetPassword(email: String) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) {
            authMessage = "Ingresa un correo para enviar el enlace de recuperación."
            return
        }

        auth.sendPasswordResetEmail(normalizedEmail)
            .addOnSuccessListener {
                authMessage = "Se envió un correo de recuperación a $normalizedEmail."
            }
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo enviar el correo de recuperación."
            }
    }

    @Suppress("unused")
    fun resendVerificationEmail() {
        val user = auth.currentUser
        if (user == null) {
            authMessage = pendingVerificationEmail?.let {
                "Vuelve a iniciar sesión para reenviar el correo de verificación."
            } ?: "No hay una cuenta pendiente de verificación."
            return
        }

        user.sendEmailVerification()
            .addOnSuccessListener {
                pendingVerificationEmail = user.email ?: pendingVerificationEmail
                authMessage = "Se envió un correo de verificación a ${user.email ?: pendingVerificationEmail ?: "tu cuenta"}."
            }
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo reenviar el correo de verificación."
            }
    }

    fun signOut() {
        auth.signOut()
        clearRealtimeListeners()
        updateCurrentUserId("")
        pendingVerificationEmail = null
        isAuthenticated = false
        authMessage = "Sesión cerrada."
        playbackState = playbackState.copy(isPlaying = false)
    }

    fun setGenre(genre: Genre) {
        selectedGenre = genre
    }

    fun toggleFavorite(song: LegionSong) {
        val userId = currentUserId
        if (userId.isBlank()) return

        val favoriteRef = database.child("favoritos").child(userId).child(song.id)
        if (favoriteSongIds.contains(song.id)) {
            favoriteRef.removeValue()
        } else {
            favoriteRef.setValue(true)
        }
    }

    fun isFavorite(songId: String): Boolean = favoriteSongIds.contains(songId)

    fun feedSongs(): List<LegionSong> = songs.filter { song ->
        val matchesGenre = selectedGenre == Genre.ALL || song.genre == selectedGenre
        matchesGenre && canSeeSong(song)
    }

    fun favoriteSongs(): List<LegionSong> = songs.filter { favoriteSongIds.contains(it.id) }

    fun canSeeSong(song: LegionSong): Boolean {
        return when (song.visibility) {
            VisibilityMode.PRIVATE -> song.uploaderId == currentUserId
            VisibilityMode.PUBLIC -> true
            VisibilityMode.GROUP -> groups.any { group -> group.id == song.groupId && group.memberIds.contains(currentUserId) }
        }
    }

    fun publishSong() {
        val upload = uploadForm
        val userId = currentUserId
        if (upload.title.isBlank() || upload.mediaUrl.isBlank() || userId.isBlank()) return
        if (!upload.mediaUrl.isRemoteStorageUrl()) {
            authMessage = "Espera a que termine la subida del archivo antes de publicar."
            return
        }

        val songRef = songsRef.push()
        val songData = mapOf<String, Any?>(
            "title" to upload.title,
            "author" to profile.name.ifBlank { "LegionSound" },
            "genre" to upload.selectedGenre.name,
            "mediaUrl" to upload.mediaUrl,
            "mediaType" to upload.mediaType.name,
            "visibility" to upload.visibility.name,
            "groupId" to upload.groupId.trim().ifBlank { null },
            "timestamp" to System.currentTimeMillis(),
            "uploaderId" to userId
        )

        songRef.setValue(songData)
            .addOnSuccessListener {
                uploadForm = UploadFormState()
            }
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo publicar la canción."
            }
    }

    /**
     * Elimina una canción, pero solo si quien la pidió es el mismo usuario que la subió.
     * Borra tanto el registro en Realtime Database como el archivo en Storage (si aplica).
     */
    fun deleteSong(song: LegionSong) {
        if (song.uploaderId.isBlank() || song.uploaderId != currentUserId) {
            authMessage = "Solo puedes eliminar canciones que tú subiste."
            return
        }

        songsRef.child(song.id).removeValue()
            .addOnSuccessListener {
                authMessage = "\"${song.title}\" fue eliminada."
                if (playbackState.currentSongId == song.id) {
                    playbackState = playbackState.copy(isPlaying = false)
                }
            }
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo eliminar la canción."
            }

        if (song.mediaUrl.isRemoteStorageUrl()) {
            runCatching {
                FirebaseStorage.getInstance().getReferenceFromUrl(song.mediaUrl).delete()
            }
        }
    }

    fun uploadMediaFile(uri: Uri, mediaType: MediaType, context: Context) {
        val userId = currentUserId
        if (userId.isBlank()) {
            authMessage = "Debes iniciar sesión para subir archivos."
            return
        }

        val originalFileName = resolveDisplayName(context, uri)
        val extension = when (mediaType) {
            MediaType.AUDIO -> ".mp3"
            MediaType.VIDEO -> ".mp4"
        }
        val generatedFileName = "${UUID.randomUUID()}$extension"
        val remotePath = "canciones_media/$userId/$generatedFileName"
        val storageRef = storage.child(remotePath)

        uploadProgress = 0f
        authMessage = "Subiendo archivo..."

        storageRef.putFile(uri)
            .addOnProgressListener { snapshot ->
                val totalBytes = snapshot.totalByteCount
                uploadProgress = if (totalBytes > 0L) {
                    snapshot.bytesTransferred.toFloat() / totalBytes.toFloat()
                } else {
                    0f
                }
            }
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        uploadForm = uploadForm.copy(
                            mediaUrl = downloadUri.toString(),
                            fileName = originalFileName ?: generatedFileName
                        )
                        authMessage = "Archivo subido correctamente."
                        uploadProgress = null
                    }
                    .addOnFailureListener { exception ->
                        authMessage = exception.localizedMessage ?: "No se pudo obtener la URL de descarga."
                        uploadProgress = null
                    }
            }
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo subir el archivo."
                uploadProgress = null
            }
    }

    /**
     * Sube una foto de perfil real a Firebase Storage (ruta perfil_fotos/{userId}/{archivo})
     * y, al terminar, actualiza profile.photoUrl con la URL de descarga real.
     */
    fun uploadProfilePhoto(uri: Uri, context: Context) {
        val userId = currentUserId
        if (userId.isBlank()) {
            authMessage = "Debes iniciar sesión para cambiar tu foto de perfil."
            return
        }

        val extension = resolveImageExtension(context, uri)
        val generatedFileName = "${UUID.randomUUID()}$extension"
        val remotePath = "perfil_fotos/$userId/$generatedFileName"
        val storageRef = storage.child(remotePath)

        profilePhotoUploadProgress = 0f
        authMessage = "Subiendo foto de perfil..."

        storageRef.putFile(uri)
            .addOnProgressListener { snapshot ->
                val totalBytes = snapshot.totalByteCount
                profilePhotoUploadProgress = if (totalBytes > 0L) {
                    snapshot.bytesTransferred.toFloat() / totalBytes.toFloat()
                } else {
                    0f
                }
            }
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        updateProfile(
                            name = profile.name,
                            description = profile.description,
                            career = profile.career,
                            photoUrl = downloadUri.toString()
                        )
                        authMessage = "Foto de perfil actualizada."
                        profilePhotoUploadProgress = null
                    }
                    .addOnFailureListener { exception ->
                        authMessage = exception.localizedMessage ?: "No se pudo obtener la URL de la foto."
                        profilePhotoUploadProgress = null
                    }
            }
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo subir la foto de perfil."
                profilePhotoUploadProgress = null
            }
    }

    fun createGroup() {
        val name = groupForm.name.trim()
        val userId = currentUserId
        if (name.isBlank() || userId.isBlank()) return

        val groupRef = groupsRef.push()
        val groupData = mapOf<String, Any?>(
            "name" to name,
            "creatorId" to userId,
            "memberIds" to mapOf(userId to true)
        )

        groupRef.setValue(groupData)
            .addOnSuccessListener {
                groupForm = GroupFormState()
            }
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo crear el grupo."
            }
    }

    fun addMemberToGroup(groupId: String, userId: String = currentUserId) {
        val memberId = userId.trim()
        if (groupId.isBlank() || memberId.isBlank()) return

        database.child("grupos")
            .child(groupId)
            .child("memberIds")
            .child(memberId)
            .setValue(true)
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo agregar el miembro al grupo."
            }
    }

    fun updateProfile(name: String, description: String, career: String, photoUrl: String) {
        val userId = currentUserId
        val updatedProfile = profile.copy(
            name = name,
            description = description,
            career = career,
            photoUrl = photoUrl
        )
        profile = updatedProfile

        if (userId.isBlank()) return

        val profileData = mapOf<String, Any?>(
            "name" to updatedProfile.name,
            "description" to updatedProfile.description,
            "career" to updatedProfile.career,
            "photoUrl" to updatedProfile.photoUrl
        )
        database.child("usuarios").child(userId).child("perfil")
            .setValue(profileData)
            .addOnFailureListener { exception ->
                authMessage = exception.localizedMessage ?: "No se pudo guardar el perfil."
            }
    }

    fun startPlayback(song: LegionSong, playlist: List<LegionSong>) {
        val songId = song.id
        playbackState = playbackState.copy(
            playlist = playlist,
            currentSongId = songId,
            isPlaying = true
        )
    }

    fun currentSong(): LegionSong? = playbackState.playlist.firstOrNull { it.id == playbackState.currentSongId }

    fun setPlaybackMode(mode: PlaybackMode) {
        playbackState = playbackState.copy(playbackMode = mode)
    }

    fun nextSong() {
        val list = playbackState.playlist
        if (list.isEmpty() || playbackState.currentSongId == null) return
        val index = list.indexOfFirst { it.id == playbackState.currentSongId }
        if (index == -1) return
        val nextIndex = when (playbackState.playbackMode) {
            PlaybackMode.ONE -> index
            PlaybackMode.LOOP -> (index + 1) % list.size
            PlaybackMode.SHUFFLE -> (list.indices).random()
            PlaybackMode.LINEAR -> if (index + 1 < list.size) index + 1 else index
        }
        val nextSongId = list[nextIndex].id
        playbackState = playbackState.copy(currentSongId = nextSongId, isPlaying = true)
    }

    fun previousSong() {
        val list = playbackState.playlist
        if (list.isEmpty() || playbackState.currentSongId == null) return
        val index = list.indexOfFirst { it.id == playbackState.currentSongId }
        if (index == -1) return
        val previousIndex = when (playbackState.playbackMode) {
            PlaybackMode.ONE -> index
            PlaybackMode.LOOP -> if (index - 1 >= 0) index - 1 else list.lastIndex
            PlaybackMode.SHUFFLE -> (list.indices).random()
            PlaybackMode.LINEAR -> if (index - 1 >= 0) index - 1 else index
        }
        val previousSongId = list[previousIndex].id
        playbackState = playbackState.copy(currentSongId = previousSongId, isPlaying = true)
    }

    fun togglePlayback() {
        playbackState = playbackState.copy(isPlaying = !playbackState.isPlaying)
    }

    private fun updateCurrentUserId(userId: String) {
        currentUserId = userId
        refreshFavoriteListener()
        refreshProfileListener()
    }

    private var profileListener: ValueEventListener? = null
    private var profileRef: DatabaseReference? = null

    /**
     * Carga el perfil real del usuario desde Realtime Database (usuarios/{userId}/perfil)
     * y mantiene `profile` sincronizado en tiempo real. Si el usuario nunca guardó un
     * perfil, se queda con un perfil vacío (no con datos de ejemplo).
     */
    private fun refreshProfileListener() {
        profileListener?.let { listener -> profileRef?.removeEventListener(listener) }
        profileListener = null
        profileRef = null

        val userId = currentUserId
        if (userId.isBlank()) {
            profile = LegionProfile(name = "", description = "", career = "", photoUrl = "")
            return
        }

        val node = database.child("usuarios").child(userId).child("perfil")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                profile = LegionProfile(
                    name = snapshot.child("name").getValue(String::class.java).orEmpty(),
                    description = snapshot.child("description").getValue(String::class.java).orEmpty(),
                    career = snapshot.child("career").getValue(String::class.java).orEmpty(),
                    photoUrl = snapshot.child("photoUrl").getValue(String::class.java).orEmpty()
                )
            }

            override fun onCancelled(error: DatabaseError) {
                authMessage = error.message ?: "No se pudo cargar el perfil."
            }
        }
        profileRef = node
        profileListener = listener
        node.addValueEventListener(listener)
    }

    private var knownSongIds: Set<String>? = null

    private fun attachSongsListener() {
        if (songsListener != null) return
        songsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedSongs = snapshot.children.mapNotNull { it.toLegionSong() }
                val updatedIds = updatedSongs.map { it.id }.toSet()

                val previousIds = knownSongIds
                if (previousIds != null) {
                    updatedSongs
                        .filter { it.id !in previousIds }
                        .forEach { notifyIfRelevantGroupSong(it) }
                }
                knownSongIds = updatedIds

                songs.clear()
                songs.addAll(updatedSongs)
            }

            override fun onCancelled(error: DatabaseError) {
                authMessage = error.message ?: "No se pudieron cargar las canciones."
            }
        }
        songsListener?.let { songsRef.addValueEventListener(it) }
    }

    /**
     * Muestra una notificación local cuando llega una canción nueva a un grupo
     * del que el usuario actual es miembro (y no fue quien la subió).
     * Funciona mientras la app está abierta o en segundo plano (proceso activo).
     * Para notificaciones cuando la app está completamente cerrada, se necesitaría
     * una Cloud Function que envíe el mensaje al tema "grupo_{groupId}".
     */
    private fun notifyIfRelevantGroupSong(song: LegionSong) {
        if (song.visibility != VisibilityMode.GROUP) return
        if (song.uploaderId == currentUserId) return
        val belongsToUser = groups.any { it.id == song.groupId && it.memberIds.contains(currentUserId) }
        if (!belongsToUser) return

        NotificationHelper.show(
            context = LegionSoundApplication.appContext,
            title = "Nuevo contenido en tu grupo",
            body = "${song.author} subió \"${song.title}\""
        )
    }

    private fun attachGroupsListener() {
        if (groupsListener != null) return
        groupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedGroups = snapshot.children.mapNotNull { it.toLegionGroup() }
                groups.clear()
                groups.addAll(updatedGroups)
                subscribeToOwnGroupTopics(updatedGroups)
            }

            override fun onCancelled(error: DatabaseError) {
                authMessage = error.message ?: "No se pudieron cargar los grupos."
            }
        }
        groupsListener?.let { groupsRef.addValueEventListener(it) }
    }

    /**
     * Suscribe al usuario actual al tema FCM de cada grupo al que pertenece
     * (formato "grupo_{groupId}"). Deja la app lista para recibir notificaciones
     * reales enviadas por tema si más adelante se agrega una Cloud Function.
     */
    /**
     * Guarda el token FCM actual del dispositivo en el perfil del usuario en Realtime Database.
     * Se llama al iniciar sesión (además de LegionMessagingService.onNewToken, que cubre
     * el caso de que el token cambie mientras la sesión ya está iniciada).
     */
    private fun saveFcmToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            database.child("usuarios").child(userId).child("fcmToken").setValue(token)
        }
    }

    private fun subscribeToOwnGroupTopics(currentGroups: List<LegionGroup>) {
        val userId = currentUserId
        if (userId.isBlank()) return
        currentGroups
            .filter { it.memberIds.contains(userId) }
            .forEach { group ->
                FirebaseMessaging.getInstance().subscribeToTopic("grupo_${group.id}")
            }
    }

    private fun refreshFavoriteListener() {
        favoritesListener?.let { listener ->
            favoritesRef?.removeEventListener(listener)
        }
        favoritesListener = null
        favoritesRef = null
        favoriteSongIds.clear()

        val userId = currentUserId
        if (userId.isBlank()) return

        val favoriteNode = database.child("favoritos").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedFavorites = snapshot.children.mapNotNull { it.key }
                favoriteSongIds.clear()
                favoriteSongIds.addAll(updatedFavorites)
            }

            override fun onCancelled(error: DatabaseError) {
                authMessage = error.message ?: "No se pudieron cargar los favoritos."
            }
        }
        favoritesRef = favoriteNode
        favoritesListener = listener
        favoriteNode.addValueEventListener(listener)
    }

    private fun clearRealtimeListeners() {
        songsListener?.let { songsRef.removeEventListener(it) }
        groupsListener?.let { groupsRef.removeEventListener(it) }
        favoritesListener?.let { favoritesRef?.removeEventListener(it) }
        profileListener?.let { profileRef?.removeEventListener(it) }
        songsListener = null
        groupsListener = null
        favoritesListener = null
        favoritesRef = null
        profileListener = null
        profileRef = null
        knownSongIds = null
        songs.clear()
        groups.clear()
        favoriteSongIds.clear()
        profile = LegionProfile(name = "", description = "", career = "", photoUrl = "")
    }
}

@Composable
fun rememberLegionUiState(): LegionUiState = remember { LegionUiState() }

private fun DataSnapshot.toLegionSong(): LegionSong? {
    val songId = key ?: return null
    val title = child("title").getValue(String::class.java).orEmpty()
    val mediaUrl = child("mediaUrl").getValue(String::class.java).orEmpty()
    val author = child("author").getValue(String::class.java).orEmpty()
    val uploaderId = child("uploaderId").getValue(String::class.java).orEmpty()
    if (title.isBlank() || mediaUrl.isBlank()) return null

    return LegionSong(
        id = songId,
        title = title,
        author = author.ifBlank { "LegionSound" },
        genre = child("genre").getValue(String::class.java).toGenreOrDefault(),
        mediaUrl = mediaUrl,
        mediaType = child("mediaType").getValue(String::class.java).toMediaTypeOrDefault(),
        visibility = child("visibility").getValue(String::class.java).toVisibilityOrDefault(),
        groupId = child("groupId").getValue(String::class.java)?.takeIf { it.isNotBlank() },
        timestamp = child("timestamp").asLongOrDefault(),
        uploaderId = uploaderId
    )
}

private fun DataSnapshot.toLegionGroup(): LegionGroup? {
    val groupId = key ?: return null
    val name = child("name").getValue(String::class.java).orEmpty()
    val creatorId = child("creatorId").getValue(String::class.java).orEmpty()
    if (name.isBlank() || creatorId.isBlank()) return null

    val memberIds = child("memberIds").children.mapNotNull { it.key }.distinct()
    return LegionGroup(
        id = groupId,
        name = name,
        creatorId = creatorId,
        memberIds = memberIds
    )
}

private fun DataSnapshot.asLongOrDefault(): Long {
    val value = value
    return when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: System.currentTimeMillis()
        else -> System.currentTimeMillis()
    }
}

private fun String?.toGenreOrDefault(default: Genre = Genre.POP): Genre {
    val normalized = this?.trim().orEmpty()
    return Genre.entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) } ?: default
}

private fun String?.toMediaTypeOrDefault(default: MediaType = MediaType.AUDIO): MediaType {
    val normalized = this?.trim().orEmpty()
    return MediaType.entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) } ?: default
}

private fun String?.toVisibilityOrDefault(default: VisibilityMode = VisibilityMode.PRIVATE): VisibilityMode {
    val normalized = this?.trim().orEmpty()
    return VisibilityMode.entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) } ?: default
}

private fun String.isRemoteStorageUrl(): Boolean = startsWith("https://") || startsWith("http://")

private fun resolveDisplayName(context: Context, uri: Uri): String? {
    val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
    return runCatching {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else {
                null
            }
        } ?: uri.lastPathSegment
    }.getOrNull()
}

private fun resolveImageExtension(context: Context, uri: Uri): String {
    val type = context.contentResolver.getType(uri)
    return when {
        type?.contains("png") == true -> ".png"
        type?.contains("webp") == true -> ".webp"
        else -> ".jpg"
    }
}