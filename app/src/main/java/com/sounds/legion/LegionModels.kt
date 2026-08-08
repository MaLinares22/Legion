package com.sounds.legion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Genre(val label: String) {
    ALL("Todos"),
    POP("Pop"),
    ROCK("Rock"),
    METAL("Metal"),
    ELECTRONIC("Electrónica"),
    REGGAETON("Reggaetón"),
    JAZZ("Jazz"),
    HIP_HOP("Hip-Hop"),
    CLASSICAL("Clásica")
}

enum class VisibilityMode(val label: String) {
    PRIVATE("Privado"),
    PUBLIC("Público"),
    GROUP("Grupal")
}

enum class PlaybackMode(val label: String) {
    LINEAR("Lineal"),
    SHUFFLE("Aleatorio"),
    ONE("Solo una pista"),
    LOOP("Bucle")
}

enum class MediaType(val label: String) {
    AUDIO("MP3"),
    VIDEO("MP4")
}

data class LegionSong(
    val id: String,
    val title: String,
    val author: String,
    val genre: Genre,
    val mediaUrl: String,
    val mediaType: MediaType,
    val visibility: VisibilityMode,
    val groupId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val uploaderId: String = ""
)

data class LegionGroup(
    val id: String,
    val name: String,
    val creatorId: String,
    val memberIds: List<String>
)

data class LegionProfile(
    val name: String,
    val description: String,
    val career: String,
    val photoUrl: String = ""
)

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)

data class UploadFormState(
    val title: String = "",
    val mediaUrl: String = "",
    val fileName: String = "",
    val selectedGenre: Genre = Genre.POP,
    val mediaType: MediaType = MediaType.AUDIO,
    val visibility: VisibilityMode = VisibilityMode.PRIVATE,
    val groupId: String = ""
)

data class GroupFormState(
    val name: String = "",
    val memberEmail: String = ""
)

data class ProfileFormState(
    val name: String = "",
    val description: String = "",
    val career: String = "",
    val photoUrl: String = ""
)

data class PlaybackState(
    val playlist: List<LegionSong> = emptyList(),
    val currentSongId: String? = null,
    val playbackMode: PlaybackMode = PlaybackMode.LINEAR,
    val isPlaying: Boolean = false
)

val genreOptions = listOf(
    Genre.ALL,
    Genre.POP,
    Genre.ROCK,
    Genre.METAL,
    Genre.ELECTRONIC,
    Genre.REGGAETON,
    Genre.JAZZ,
    Genre.HIP_HOP,
    Genre.CLASSICAL
)

fun sampleSongs(): List<LegionSong> = listOf(
    LegionSong(
        id = "song-1",
        title = "Neon Pulse",
        author = "LegionSound",
        genre = Genre.ELECTRONIC,
        mediaUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        mediaType = MediaType.AUDIO,
        visibility = VisibilityMode.PRIVATE,
        uploaderId = "user-1"
    ),
    LegionSong(
        id = "song-2",
        title = "Midnight Drive",
        author = "LegionSound",
        genre = Genre.ROCK,
        mediaUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        mediaType = MediaType.AUDIO,
        visibility = VisibilityMode.GROUP,
        groupId = "group-1",
        uploaderId = "user-1"
    ),
    LegionSong(
        id = "song-3",
        title = "City Lights",
        author = "DJ Aurora",
        genre = Genre.POP,
        mediaUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
        mediaType = MediaType.AUDIO,
        visibility = VisibilityMode.GROUP,
        groupId = "group-1",
        uploaderId = "user-2"
    ),
    LegionSong(
        id = "song-4",
        title = "Bassline Stories",
        author = "LegionSound",
        genre = Genre.HIP_HOP,
        mediaUrl = "https://samplelib.com/lib/preview/mp4/sample-5s.mp4",
        mediaType = MediaType.VIDEO,
        visibility = VisibilityMode.PRIVATE,
        uploaderId = "user-1"
    )
)

fun sampleGroups(): List<LegionGroup> = listOf(
    LegionGroup(
        id = "group-1",
        name = "Neon Crew",
        creatorId = "user-1",
        memberIds = listOf("user-1", "user-2")
    ),
    LegionGroup(
        id = "group-2",
        name = "Rock Lab",
        creatorId = "user-2",
        memberIds = listOf("user-2")
    )
)

fun sampleProfile(): LegionProfile = LegionProfile(
    name = "Usuario",
    description = "Solo yo.",
    career = "Ingenieria",
    photoUrl = ""
)