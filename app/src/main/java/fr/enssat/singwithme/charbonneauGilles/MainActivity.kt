package fr.enssat.singwithme.charbonneauGilles

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import fr.enssat.singwithme.charbonneauGilles.ui.theme.SingWithMeTheme

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var isPlaying = false
    private var isPaused = false
    private lateinit var mainViewModel: MainViewModel
    private var currentLyricIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            SingWithMeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    KaraokeScreen(
                        viewModel = mainViewModel,
                        onPlayClick = {
                            startKaraoke()
                        },
                        onPauseClick = {
                            pauseKaraoke()
                        },
                        onResetClick = {
                            resetKaraoke()
                        },
                        isPlaying = isPlaying
                    )
                }
            }
        }

        val songsUrl = "https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/playlist.json"
        mainViewModel.loadSongs(songsUrl, this)
    }

    private fun startKaraoke() {
        mainViewModel.mp3FileUri.observe(this) { mp3FileUri ->
            if (mp3FileUri != null) {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(this, mp3FileUri)
                }
                if (isPaused) {
                    mediaPlayer?.seekTo(mainViewModel.currentProgress.value?.toInt() ?: 0)
                    isPaused = false
                }
                mediaPlayer?.start()

                if (handler == null) {
                    handler = Handler(Looper.getMainLooper())
                }
                if (runnable == null) {
                    runnable = object : Runnable {
                        override fun run() {
                            val currentTime = mediaPlayer?.currentPosition ?: 0
                            val duration = mediaPlayer?.duration ?: 0
                            val lyricLines = mainViewModel.lyricLines.value
                            if (lyricLines != null) {
                                for (i in lyricLines.indices) {
                                    if (lyricLines[i].timecode.toTimeMillis() <= currentTime) {
                                        if (i != currentLyricIndex) {
                                            currentLyricIndex = i
                                            mainViewModel.updateCurrentLyric(lyricLines[i].lyric)
                                            Log.d("Karaoke", "Current Time: $currentTime, Lyric: ${lyricLines[i].lyric}, Lyric time : ${lyricLines[i].timecode.toTimeMillis()}")
                                        }
                                    } else {
                                        break
                                    }
                                }
                            }
                            mainViewModel.updateProgress(currentTime, duration)
                            handler?.postDelayed(this, 100)
                        }
                    }
                }
                handler?.post(runnable!!)
                isPlaying = true
            }
        }
    }

    private fun pauseKaraoke() {
        mediaPlayer?.pause()
        isPaused = true
        runnable?.let { handler?.removeCallbacks(it) }
        isPlaying = false
    }

    private fun resetKaraoke() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        runnable?.let { handler?.removeCallbacks(it) }
        handler = null
        runnable = null
        isPlaying = false
        isPaused = false
        currentLyricIndex = -1
        mainViewModel.updateCurrentLyric("")
        mainViewModel.updateProgress(0, 0)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        runnable?.let { handler?.removeCallbacks(it) }
        isPaused = true
    }

    override fun onResume() {
        super.onResume()
        if (isPlaying) {
            mediaPlayer?.start()
            if (handler == null) {
                handler = Handler(Looper.getMainLooper())
            }
            if (runnable == null) {
                runnable = object : Runnable {
                    override fun run() {
                        val currentTime = mediaPlayer?.currentPosition ?: 0
                        val duration = mediaPlayer?.duration ?: 0
                        val lyricLines = mainViewModel.lyricLines.value
                        if (lyricLines != null) {
                            for (i in lyricLines.indices) {
                                if (lyricLines[i].timecode.toTimeMillis() <= currentTime) {
                                    if (i != currentLyricIndex) {
                                        currentLyricIndex = i
                                        mainViewModel.updateCurrentLyric(lyricLines[i].lyric)
                                        Log.d("Karaoke", "Current Time: $currentTime, Lyric: ${lyricLines[i].lyric}, Lyric time : ${lyricLines[i].timecode.toTimeMillis()}")
                                    }
                                } else {
                                    break
                                }
                            }
                        }
                        mainViewModel.updateProgress(currentTime, duration)
                        handler?.postDelayed(this, 100)
                    }
                }
            }
            handler?.post(runnable!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        runnable?.let { handler?.removeCallbacks(it) }
        handler = null
        runnable = null
    }

    fun String.toTimeMillis(): Int {
        val trimmed = this.trim()
        val parts = trimmed.split(":")
        val minutes = parts[0].toInt()
        val seconds = parts[1].toInt()
        return minutes * 60 * 1000 + seconds * 1000
    }
}

@Composable
fun KaraokeScreen(
    viewModel: MainViewModel,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResetClick: () -> Unit,
    isPlaying: Boolean
) {
    val currentLyric by viewModel.currentLyric.observeAsState("")
    val progress by viewModel.progress.observeAsState(0f)
    val songs by viewModel.songs.observeAsState(emptyList())
    val selectedSong by viewModel.selectedSong.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()

    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
        ) {
            Button(onClick = { expanded = !expanded }) {
                Text(selectedSong?.name ?: "Select a Song")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                songs.forEach { song ->
                    DropdownMenuItem(
                        text = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                elevation = CardDefaults.cardElevation()
                            ) {
                                Column {
                                    if (song.locked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    Text(
                                        text = song.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        },
                        onClick = {
                            viewModel.selectSong(song, context)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        Text(
            text = currentLyric,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(bottom = 24.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pause), // Custom play icon
                    contentDescription = "Play",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onResetClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White
                )
            }
        }
    }
}
