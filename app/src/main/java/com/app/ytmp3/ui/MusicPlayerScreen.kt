package com.app.ytmp3.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.app.ytmp3.downloader.MP3Downloader
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

data class SongItem(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
    val durationInSeconds: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloader = remember { MP3Downloader(context) }

    var activeVideoId by remember { mutableStateOf("dQw4w9WgXcQ") }
    var youtubePlayerInstance by remember { mutableStateOf<YouTubePlayer?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedSongForDownload by remember { mutableStateOf<SongItem?>(null) }

    // Launcher izin penyimpanan untuk Android 8 dan 9
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "Izin diberikan! Silakan tekan tombol download sekali lagi.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Izin penyimpanan ditolak. Tidak dapat mengunduh file.", Toast.LENGTH_LONG).show()
        }
    }

    fun checkPermissionAndDownload(song: SongItem, bitrate: Int) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        
        scope.launch {
            downloader.downloadAudio(song.id, song.title, bitrate)
        }
    }

    val allSongs = listOf(
        SongItem("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley", "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", 212),
        SongItem("kJQP7kiw5Fk", "Despacito", "Luis Fonsi ft. Daddy Yankee", "https://img.youtube.com/vi/kJQP7kiw5Fk/hqdefault.jpg", 281),
        SongItem("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen", "https://img.youtube.com/vi/fJ9rUzIMcZQ/hqdefault.jpg", 359)
    )

    val filteredSongs = allSongs.filter { 
        it.title.contains(searchQuery, ignoreCase = true) || it.channel.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp)) {
        
        // Pemutar Video YouTube Native
        Card(modifier = Modifier.fillMaxWidth().height(210.dp), shape = RoundedCornerShape(12.dp)) {
            AndroidView(
                factory = { ctx ->
                    YouTubePlayerView(ctx).apply {
                        addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                youtubePlayerInstance = youTubePlayer
                                youTubePlayer.loadVideo(activeVideoId, 0f)
                            }
                        })
                    }
                },
                update = { youtubePlayerInstance?.cueVideo(activeVideoId, 0f) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Kotak Pencarian Judul Lagu
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari judul lagu atau artis...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF2E7D32), 
                unfocusedBorderColor = Color.DarkGray, 
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(50)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Daftar Lagu
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filteredSongs) { song ->
                SongCardItem(
                    song = song,
                    onPlayClick = {
                        activeVideoId = song.id
                        youtubePlayerInstance?.loadVideo(song.id, 0f)
                    },
                    onDownloadClick = { selectedSongForDownload = song }
                )
            }
        }
    }

    // Dialog Pilihan Bitrate & Ukuran File
    selectedSongForDownload?.let { song ->
        BitrateSelectionDialog(
            song = song,
            onDismiss = { selectedSongForDownload = null },
            onBitrateSelected = { bitrate ->
                checkPermissionAndDownload(song, bitrate)
                selectedSongForDownload = null 
            }
        )
    }
}

@Composable
fun BitrateSelectionDialog(song: SongItem, onDismiss: () -> Unit, onBitrateSelected: (Int) -> Unit) {
    val bitrates = listOf(128, 192, 256, 320)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text("Pilih Kualitas MP3", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(song.title, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                
                bitrates.forEach { kbps ->
                    val sizeInMB = (kbps * song.durationInSeconds) / 8192.0
                    val sizeFormatted = String.format("%.1f MB", sizeInMB)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBitrateSelected(kbps) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$kbps kbps", color = Color.White, fontSize = 16.sp)
                        Text(sizeFormatted, color = Color(0xFF4CAF50), fontSize = 16.sp)
                    }
                    Divider(color = Color.DarkGray, thickness = 0.5.dp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Red)
            }
        }
    )
}

@Composable
fun SongCardItem(song: SongItem, onPlayClick: () -> Unit, onDownloadClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sampul Album / Thumbnail
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = "Cover",
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // Tombol Download tepat di sebelah sampul
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier
                    .padding(start = 6.dp, end = 6.dp)
                    .background(Color(0xFF2E7D32), shape = RoundedCornerShape(50))
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download MP3", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Informasi Judul & Channel
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(song.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(song.channel, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }

            // Tombol Play Video
            IconButton(onClick = onPlayClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFFFF0000))
            }
        }
    }
}
