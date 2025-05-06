package app.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.sample.ui.theme.AppTheme
import box.media.downloader.DownloadInfo
import box.media.downloader.MediaDownloadService
import box.media.downloader.MediaDownloader
import box.media.downloader.util.formatedBytes
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MediaDownloader.init(this)
        MediaDownloadService.createNotificationChannel(this)

        val info = DownloadInfo(
            url = "https://vod3.cf.dmcdn.net/sec2(KHCCqMnWWIskCGda_3d8dRV7VbrqjjYqjo3Fs6Xlz8pBFnjDCzzIol6TT65yZb9llJmJfie8VqmFbCxu33hplZ8Vdk2OonE8ZuJSdIjy1mbHx510jMkd6WDpQGTxKCifcK-MpTx8svhRQexctfyvfy2J29VI4kQSaHOm3jsdM-2EfHnMjlNtXma8qtrm6tLO)/video/032/341/576143230_mp4_h264_aac_fhd_2.m3u8",
            originUrl = "https://www.dailymotion.com/video/x9j0qym",
            title = "‘Should I beg now...’: Bahadur Shah Zafar’s descendant gets emotional as SC rejects Red Fort claim",
            coverUrl = "https://s1.dmcdn.net/v/YLpz-1e6Kn0TGiUpD/x360",
        )
        MediaDownloader.download(info)
        lifecycleScope.launch {
            val tasks = MediaDownloader.getDownloadTasks()
            for (it in tasks) {
                launch {
                    Log.e("---", "task: ${it.url}")
                    it.receivedBytesFlow.onEach {
                        Log.e("---", "receivedBytes: ${it.formatedBytes()}")
                    }.launchIn(this)
                    it.speedFlow.onEach {
                        Log.e("---", "speed: ${it}")
                    }.launchIn(this)
                }
            }
        }

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
