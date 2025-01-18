package fr.enssat.singwithme.CharbonneauGilles

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.enssat.singwithme.CharbonneauGilles.model.LyricLine
import fr.enssat.singwithme.CharbonneauGilles.model.Song
import fr.enssat.singwithme.CharbonneauGilles.model.parseMarkdown
import fr.enssat.singwithme.CharbonneauGilles.model.parseSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainViewModel : ViewModel() {

    private val _lyricLines = MutableLiveData<List<LyricLine>>()
    val lyricLines: LiveData<List<LyricLine>> get() = _lyricLines

    private val _mp3FileUri = MutableLiveData<Uri?>()
    val mp3FileUri: LiveData<Uri?> get() = _mp3FileUri

    private val _currentLyric = MutableLiveData<String>()
    val currentLyric: LiveData<String> get() = _currentLyric

    private val _progress = MutableLiveData<Float>()
    val progress: LiveData<Float> get() = _progress

    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> get() = _songs

    private val _selectedSong = MutableLiveData<Song?>()
    val selectedSong: LiveData<Song?> get() = _selectedSong

    fun downloadAndParseMarkdown(url: String) {
        viewModelScope.launch {
            try {
                val markdownContent = withContext(Dispatchers.IO) {
                    downloadFile(url)
                }
                if (markdownContent != null) {
                    val parsedLyrics = parseMarkdown(markdownContent)
                    _lyricLines.value = parsedLyrics
                    logParsedLyrics(parsedLyrics)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadMp3File(url: String, context: Context) {
        viewModelScope.launch {
            try {
                val mp3FileUri = withContext(Dispatchers.IO) {
                    downloadFileToStorage(url, context)
                }
                _mp3FileUri.value = mp3FileUri
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSongs(url: String) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    downloadFile(url)
                }
                if (json != null) {
                    val songs = parseSongs(json)
                    _songs.value = songs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectSong(song: Song, context: Context) {
        _selectedSong.value = song
        if (song.path != null) {
            val markdownUrl = "https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/${song.path}"
            val mp3Url = markdownUrl.replace(".md", ".mp3")
            downloadAndParseMarkdown(markdownUrl)
            downloadMp3File(mp3Url, context)
        }
    }

    private fun downloadFile(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            Log.d("MainViewModel", "Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val content = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    content.append(line)
                    content.append("\n")
                }
                reader.close()
                content.toString()
            } else {
                Log.e("MainViewModel", "Failed to download file. Response code: $responseCode")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainViewModel", "Exception occurred while downloading file: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun downloadFileToStorage(url: String, context: Context): Uri? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            Log.d("MainViewModel", "Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val contentResolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "creep_${System.currentTimeMillis()}.mp3")
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SingWithMe")
                }

                val contentUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (contentUri == null) {
                    Log.e("MainViewModel", "Failed to insert file into media store.")
                    return null
                }

                val outputStream = contentResolver.openOutputStream(contentUri)

                val buffer = ByteArray(4 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream?.write(buffer, 0, bytesRead)
                }

                outputStream?.flush()
                outputStream?.close()
                inputStream.close()

                MediaScannerConnection.scanFile(context, arrayOf(contentUri.toString()), null, null)

                contentUri
            } else {
                Log.e("MainViewModel", "Failed to download file. Response code: $responseCode")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainViewModel", "Exception occurred while downloading file: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun logParsedLyrics(lyrics: List<LyricLine>) {
        for (lyricLine in lyrics) {
            Log.d("ParsedLyrics", "Timecode: ${lyricLine.timecode}, Lyric: ${lyricLine.lyric}")
        }
    }

    fun updateCurrentLyric(lyric: String) {
        _currentLyric.value = lyric
    }

    fun updateProgress(currentTime: Int, duration: Int) {
        if (duration > 0) {
            val progress = (currentTime.toFloat() / duration.toFloat())
            _progress.value = progress
        } else {
            _progress.value = 0f
        }
    }
}
