package fr.enssat.singwithme.charbonneauGilles

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import fr.enssat.singwithme.charbonneauGilles.ui.composables.KaraokeScreen
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

