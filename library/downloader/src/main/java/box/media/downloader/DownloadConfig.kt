package box.media.downloader

import okhttp3.OkHttpClient
import java.io.File

/**
 * @param downloadDir 下载目录
 * @param cacheDir 缓存目录
 * @param parallel 同时下载任务数
 * @param useService 是否使用 [MediaDownloadService] 下载
 * @param enableLog 是否开启日志
 * @param customLog 自定义打印日志, 默认使用 [android.util.Log]
 */
data class DownloadConfig(
    val downloadDir: File? = null,
    val cacheDir: File? = null,
    val parallel: Int = 3,
    val mode: Int = MODE_DEFAULT,
    val enableLog: Boolean = true,
    val customLog: ((level: Int, tag: String, msg: String, e: Throwable?) -> Unit)? = null,
    val userAgent: String? = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.15 Safari/537.36",
    val okhttp: OkHttpClient? = null,
) {
    companion object {
        const val MODE_DEFAULT = 0
        const val MODE_BACKGROUND_SERVICE = 1
        const val MODE_FOREGROUND_SERVICE = 2
    }
}
