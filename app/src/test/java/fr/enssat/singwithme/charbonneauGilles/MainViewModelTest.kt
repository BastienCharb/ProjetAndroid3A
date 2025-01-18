package fr.enssat.singwithme.charbonneauGilles

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import fr.enssat.singwithme.charbonneauGilles.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import java.net.HttpURLConnection
import java.net.URL

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mainViewModel: MainViewModel
    private val testDispatcher = TestCoroutineDispatcher()
    private val testCoroutineScope = TestCoroutineScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mainViewModel = MainViewModel()
    }


    @Test
    fun testLoadSongs() = testCoroutineScope.runTest {
        val mockResponse = """
        [
            {"name": "Song1", "artist": "Artist1", "locked": false, "path": "path1"},
            {"name": "Song2", "artist": "Artist2", "locked": true, "path": "path2"}
        ]
        """

        val mockUrl = mock(URL::class.java)
        `when`(mockUrl.openConnection()).thenReturn(mock(HttpURLConnection::class.java))
        `when`(mockUrl.protocol).thenReturn("http")
        `when`(mockUrl.host).thenReturn("example.com")
        `when`(mockUrl.path).thenReturn("/path")

        val mockConnection = mock(HttpURLConnection::class.java)
        `when`(mockConnection.responseCode).thenReturn(200)
        `when`(mockConnection.inputStream).thenReturn(mockResponse.byteInputStream())

        val mockContext = mock(Context::class.java)

        val observer = Observer<List<Song>> {
            assertEquals(2, it.size)
            assertEquals("Song1", it[0].name)
            assertEquals("Song2", it[1].name)
        }
        mainViewModel.songs.observeForever(observer)
        mainViewModel.loadSongs("http://example.com/path", mockContext)
        mainViewModel.songs.removeObserver(observer)
    }

    @Test
    fun testSelectSong() = testCoroutineScope.runTest {
        val mockSong = Song("Song1", "Artist1", false, "path1")
        val mockContext = mock(Context::class.java)

        mainViewModel.selectSong(mockSong, mockContext)

        val selectedSong = mainViewModel.selectedSong.value
        assertEquals("Song1", selectedSong?.name)
    }

    @Test
    fun testUpdateCurrentLyric() {
        val mockLyric = "This is a lyric"

        mainViewModel.updateCurrentLyric(mockLyric)

        val currentLyric = mainViewModel.currentLyric.value
        assertEquals(mockLyric, currentLyric)
    }

    @Test
    fun testUpdateProgress() {
        val mockCurrentTime = 1000
        val mockDuration = 2000

        mainViewModel.updateProgress(mockCurrentTime, mockDuration)

        val progress = mainViewModel.progress.value
        assertEquals(0.5f, progress)
    }
}
