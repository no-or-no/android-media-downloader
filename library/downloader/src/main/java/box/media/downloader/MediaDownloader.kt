@file:Suppress("UnsafeOptInUsageError")

package box.media.downloader

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.transformer.ExportResult
import box.media.downloader.internal.Cookies
import box.media.downloader.internal.DispatchQueueThread
import box.media.downloader.internal.DownloadInfoQueue
import box.media.downloader.internal.StopReason
import box.media.downloader.internal.transformToMp4
import box.media.downloader.util.Log
import box.media.downloader.util.UrlUtil
import box.media.downloader.util.md5
import box.media.downloader.util.safeResume
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.CoroutineContext

object MediaDownloader : CoroutineScope {
    internal const val TAG = "Downloader"

    /** 最大并行下载数 */
    var parallel: Int
        get() = config.parallel
        set(value) {
            require(value > 0)
            config = config.copy(parallel = value)
            launch { requireDownloadManager().maxParallelDownloads = value }
        }

    private lateinit var applicationContext: Context
    private var config: DownloadConfig = DownloadConfig()

    private var downloadManager: DownloadManager? = null

    // 数据库
    private val databaseProvider: DatabaseProvider by lazy {
        StandaloneDatabaseProvider(applicationContext)
    }
    // 下载目录
    private inline val downloadDir: File get() {
        return config.downloadDir
            ?: applicationContext.getExternalFilesDir(null)?.resolve("downloads")
            ?: applicationContext.filesDir.resolve("downloads")
    }
    // 下载缓存
    private val downloadCache: Cache by lazy {
        val cacheDir = downloadDir.resolve("cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        SimpleCache(cacheDir, NoOpCacheEvictor(), databaseProvider)
    }
    // http
    private val httpDataSourceFactory: HttpDataSource.Factory by lazy {
        val client = config.okhttp ?: OkHttpClient.Builder()
            .cookieJar(Cookies)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        OkHttpDataSource.Factory(client)
            .setUserAgent(config.userAgent)
    }

    private val downloadQueue = DownloadInfoQueue()
    private val downloadUpdater = DownloadInfoUpdater(downloadQueue)

    private val taskThread by lazy { DispatchQueueThread(TAG) }
    // 单线程 HandlerThread
    override val coroutineContext: CoroutineContext by lazy {
        val dispatcher = taskThread.dispatcher
        val exceptionHandler = CoroutineExceptionHandler { _, e -> Log.e(tag = TAG, e = e) }
        SupervisorJob() + dispatcher + exceptionHandler
    }

    /** 初始化 */
    fun init(context: Context, config: DownloadConfig = this.config) {
        this.applicationContext = context.applicationContext
        this.config = config
        Log.enable = config.enableLog
        Log.delegate = config.customLog
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        Log.e(msg = "downloadDir: $downloadDir")
    }

    fun observeTasks(scope: CoroutineScope, onUpdate: (List<DownloadInfo>) -> Unit) {
        scope.launch {
            getDownloadTasks()
        }
        downloadQueue.observeUpdate(scope) {
            onUpdate(downloadQueue.asList())
        }
    }

    suspend fun getDownloadTasks(): List<DownloadInfo> = withContext(coroutineContext) {
        try {
            requireDownloadManager().downloadIndex.getDownloads(
                Download.STATE_QUEUED, Download.STATE_STOPPED,
                Download.STATE_DOWNLOADING, Download.STATE_FAILED,
                Download.STATE_RESTARTING,
            ).use { cursor ->
                Log.e(msg = "getDownloads: ${cursor.count}")

                while (cursor.moveToNext()) {
                    val download = cursor.download
                    var info = downloadQueue[download.request.uri.toString()]
                    if (info == null) {
                        info = download.toDownloadInfo()
                        downloadQueue.add(info, false)
                    } else {
                        info.update(download)
                    }
                    Cookies[info.url] = info.cookie
                }
                downloadQueue.sort()
            }
        } catch (e: Exception) {
            Log.e(TAG, e = e)
        }
        downloadQueue.asList()
    }

    /** 开始下载/恢复下载 */
    fun download(info: DownloadInfo) {
        if (info.url.isEmpty()) return

        launch {
            downloadQueue.add(info)
            val request = info.prepareDownloadRequest() ?: return@launch
            val manager = requireDownloadManager()
            manager.maxParallelDownloads = config.parallel
            Cookies[request.uri.toString()] = info.cookie

            when (config.mode) {
                DownloadConfig.MODE_DEFAULT -> {
                    manager.resumeDownloads()
                    manager.addDownload(request)
                }
                DownloadConfig.MODE_BACKGROUND_SERVICE -> {
                    DownloadService.sendAddDownload(applicationContext, MediaDownloadService::class.java, request, false)
                }
                DownloadConfig.MODE_FOREGROUND_SERVICE -> {
                    DownloadService.sendAddDownload(applicationContext, MediaDownloadService::class.java, request, true)
                }
                else -> error("invalid mode: ${config.mode}")
            }
        }
    }

    /** 暂停下载 */
    fun pause(info: DownloadInfo, reason: StopReason = StopReason.UserRequested) = launch {
        val manager = requireDownloadManager()
        when (config.mode) {
            DownloadConfig.MODE_DEFAULT -> {
                manager.setStopReason(info.id, reason.value)
            }
            DownloadConfig.MODE_BACKGROUND_SERVICE -> {
                DownloadService.sendSetStopReason(
                    applicationContext, MediaDownloadService::class.java,
                    info.id, reason.value, false,
                )
            }
            DownloadConfig.MODE_FOREGROUND_SERVICE -> {
                DownloadService.sendSetStopReason(
                    applicationContext, MediaDownloadService::class.java,
                    info.id, reason.value, true,
                )
            }
            else -> error("invalid mode: ${config.mode}")
        }
    }

    /** 暂停全部 */
    fun pauseAll(reason: StopReason = StopReason.UserRequested) = launch {
        val manager = requireDownloadManager()
        when (config.mode) {
            DownloadConfig.MODE_DEFAULT -> {
                manager.setStopReason(null, reason.value)
            }
            DownloadConfig.MODE_BACKGROUND_SERVICE -> {
                DownloadService.sendSetStopReason(
                    applicationContext, MediaDownloadService::class.java,
                    null, reason.value, false,
                )
            }
            DownloadConfig.MODE_FOREGROUND_SERVICE -> {
                DownloadService.sendSetStopReason(
                    applicationContext, MediaDownloadService::class.java,
                    null, reason.value, true,
                )
            }
            else -> error("invalid mode: ${config.mode}")
        }
    }

    /** 删除下载 */
    fun remove(id: String) = launch {
        val manager = requireDownloadManager()
        when (config.mode) {
            DownloadConfig.MODE_DEFAULT -> {
                manager.removeDownload(id)
            }
            DownloadConfig.MODE_BACKGROUND_SERVICE -> {
                DownloadService.sendRemoveDownload(
                    applicationContext, MediaDownloadService::class.java,
                    id, false,
                )
            }
            DownloadConfig.MODE_FOREGROUND_SERVICE -> {
                DownloadService.sendRemoveDownload(
                    applicationContext, MediaDownloadService::class.java,
                    id, true,
                )
            }
            else -> error("invalid mode: ${config.mode}")
        }
    }

    internal fun requireDownloadManager(): DownloadManager {
        downloadManager?.let { return it }

        // 在 taskThread 中调用
        if (taskThread.isCurrentThread()) {
            downloadManager = createDownloadManager()
        }
        // 在其它线程调用, 需要在 taskThread 中创建 DownloadManagerΩ
        else {
            val latch = CountDownLatch(1)
            taskThread.post {
                downloadManager = createDownloadManager()
                latch.countDown()
            }
            // 阻塞当前线程, 直到 countDown()
            runCatching { latch.await() }
        }
        return downloadManager!!
    }

    private fun createDownloadManager(): DownloadManager {
        return DownloadManager(
            applicationContext,
            databaseProvider,
            downloadCache,
            httpDataSourceFactory,
            Dispatchers.IO.asExecutor(),
        ).also {
            it.addListener(downloadUpdater)
        }
    }

    private suspend fun DownloadInfo.prepareDownloadRequest(): DownloadRequest? {
        // TODO 下载 封面
        withContext(Dispatchers.IO) {

        }

        return suspendCancellableCoroutine { c ->
            val media = MediaItem.Builder()
                .setUri(url)
                .setMimeType(UrlUtil.getMediaMimeType(url))
                .build()
            val helper = DownloadHelper.forMediaItem(
                applicationContext, media, null, httpDataSourceFactory
            )
            val callback = object : DownloadHelper.Callback {
                override fun onPrepared(helper: DownloadHelper) {
                    val id = url.md5()
                    val data = toRequestData()
                    val request = helper.getDownloadRequest(id, data)
                    c.safeResume(request)
                    helper.release()
                }

                override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                    Log.e(tag = TAG, e = e)
                    c.safeResume(null)
                    helper.release()
                }
            }
            c.invokeOnCancellation { helper.release() }
            helper.prepare(callback)
        }
    }

    private class DownloadInfoUpdater(val queue: DownloadInfoQueue) : DownloadManager.Listener {
        private val scope by lazy {
            CoroutineScope(DispatchQueueThread("download-updater").dispatcher)
        }
        private var job: Job? = null
        private var updateJob: Job? = null

        override fun onInitialized(downloadManager: DownloadManager) {
            startPeriodicUpdates(downloadManager)
        }

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            MediaDownloader.launch {
                val request = download.request
                val info = queue[request.uri.toString()] ?: return@launch
                when (download.state) {
                    Download.STATE_COMPLETED -> {
                        info.state = DownloadState.Transforming
                        val mediaItem = request.toMediaItem()
                        val filename = UrlUtil.getFileName(request.uri.toString(), true)
                        val outPath = downloadDir.resolve("$filename.mp4").absolutePath
                        val result = mediaItem.transformToMp4(applicationContext, downloadCache, outPath)
                        when (result.optimizationResult) {
                            ExportResult.OPTIMIZATION_NONE,
                            ExportResult.OPTIMIZATION_SUCCEEDED -> {
                                info.filePath = outPath
                                info.state = DownloadState.Completed
                                downloadQueue.remove(info.url)
                                Log.i(msg = "transform to mp4 succeed: $filename")
                            }
                            else -> {
                                info.state = DownloadState.Failed
                                Log.e(msg = "transform to mp4 failed: ${result.optimizationResult}, $filename")
                            }
                        }
                        Log.e(msg = "onDownloadComplete: ${request.mimeType}, ${info.url}")
                    }
                    else -> {
                        info.state = download.downloadState
                    }
                }
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            MediaDownloader.launch {
                val url = download.request.uri.toString()
                Log.e(msg = "onDownloadRemoved: $url")

                Cookies.remove(url)
                downloadQueue.remove(url)
            }
        }

        fun startPeriodicUpdates(manager: DownloadManager) {
            if (job?.isActive == true) return

            job = scope.launch {
                while (isActive) {
                    if (updateJob?.isActive != true) {
                        updateJob = MediaDownloader.launch {
                            for (download in manager.currentDownloads) {
                                val info = queue[download.request.uri.toString()] ?: continue
                                info.updateProgress(download)
                            }
                        }
                    }
                    delay(1000)
                }
            }
        }

        fun stopPeriodicUpdates() {
            job?.cancel()
            job = null
            updateJob?.cancel()
            updateJob = null
        }

    }

}