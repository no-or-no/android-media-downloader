@file:Suppress("NOTHING_TO_INLINE", "FunctionName", "UnsafeOptInUsageError")

package box.media.downloader

import androidx.media3.exoplayer.offline.Download
import box.media.downloader.internal.RequestExtraData
import box.media.downloader.util.Cbor
import box.media.downloader.util.Log
import box.media.downloader.util.formatedBytes
import box.media.downloader.util.md5
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.math.roundToLong

inline fun DownloadInfo(
    url: String,
    originUrl: String,
    title: String,
    coverUrl: String?,
    cookie: String? = null,
) = DownloadInfo(
    url = url,
    originUrl = originUrl,
    title = title,
    coverUrl = coverUrl,
    cookie = cookie,
    createTime = System.currentTimeMillis(),
    coverLocalPath = null,
    state = DownloadState.NotSet,
    percent = 0f,
    totalBytes = 0,
    receivedBytes = 0,
)

class DownloadInfo @PublishedApi internal constructor(
    val url: String,
    val originUrl: String,
    val title: String,
    val coverUrl: String?,
    val cookie: String?,
    val createTime: Long,
    coverLocalPath: String?,
    state: DownloadState,
    percent: Float, // [0, 100]
    totalBytes: Long,
    receivedBytes: Long,
) {
    val id: String = url.md5()
    var filePath: String? = null; internal set

    internal var download: Download? = null
        set(value) {
            field = value
            if (value != null) {
                state = value.downloadState
                val contentLength = value.contentLength
                val bytesDownloaded = value.bytesDownloaded
                val percentDownloaded = value.percentDownloaded
                percent = percentDownloaded
                if (contentLength > 0) {
                    totalBytes = contentLength
                } else if (percentDownloaded > 0) {
                    totalBytes = (bytesDownloaded * 100f / percentDownloaded).roundToLong()
                }
                receivedBytes = value.bytesDownloaded
            }
        }

    @PublishedApi
    internal val _coverLocalPath = MutableStateFlow(coverLocalPath)
    val coverLocalPathFlow = _coverLocalPath.asStateFlow()
    inline var coverLocalPath: String?
        get() = _coverLocalPath.value
        internal set(value) { _coverLocalPath.value = value }

    @PublishedApi
    internal val _state = MutableStateFlow(state)
    val stateFlow = _state.asStateFlow()
    inline var state: DownloadState
        get() = _state.value
        internal set(value) { _state.value = value }

    @PublishedApi
    internal val _percent = MutableStateFlow(percent)
    val percentFlow = _percent.asStateFlow()
    inline var percent: Float
        get() = _percent.value
        internal set(value) { _percent.value = value }

    @PublishedApi
    internal val _totalBytes = MutableStateFlow(totalBytes)
    val totalBytesFlow = _totalBytes.asStateFlow()
    inline var totalBytes: Long
        get() = _totalBytes.value
        internal set(value) { _totalBytes.value = value }

    @PublishedApi
    internal val _receivedBytes = MutableStateFlow(receivedBytes)
    val receivedBytesFlow = _receivedBytes.asStateFlow()
    inline var receivedBytes: Long
        get() = _receivedBytes.value
        internal set(value) { _receivedBytes.value = value }

    internal var lastReceivedBytes = 0L
    internal val monitorBytesPerSecond = MutableStateFlow("")
    /** 下载速度 */
    val speedFlow = monitorBytesPerSecond.asStateFlow()

    internal fun updateProgress(download: Download) {
        lastReceivedBytes = receivedBytes
        this.download = download
        Log.e(msg = "progress: ${state}, ${percent}, ${receivedBytes}/${totalBytes}, $lastReceivedBytes")

        if (state == DownloadState.Downloading) {
            val diff = receivedBytes - lastReceivedBytes
            Log.e(msg = "diff: $diff")
            monitorBytesPerSecond.value = diff.formatedBytes().let {
                if (it.isNotEmpty()) "$it/s" else ""
            }
        } else {
            monitorBytesPerSecond.value = ""
        }
    }

    internal fun update(download: Download) {
        updateProgress(download)

        val data = download.requestData
        coverLocalPath = data?.coverLocalPath
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun DownloadInfo.toRequestData(): ByteArray {
    val data = RequestExtraData(
        originUrl = originUrl,
        title = title,
        coverUrl = coverUrl,
        cookie = cookie,
        createTime = createTime,
        coverLocalPath = coverLocalPath,
    )
    return Cbor.encodeToByteArray(data)
}

@OptIn(ExperimentalSerializationApi::class)
internal fun Download.toDownloadInfo(): DownloadInfo {
    val data = requestData
    return DownloadInfo(
        url = request.uri.toString(),
        originUrl = data?.originUrl.orEmpty(),
        title = data?.title.orEmpty(),
        coverUrl = data?.coverUrl,
        cookie = data?.cookie,
        createTime = data?.createTime ?: 0,
        coverLocalPath = data?.coverLocalPath,
        state = downloadState,
        percent = percentDownloaded,
        totalBytes = contentLength,
        receivedBytes = bytesDownloaded,
    ).also {
        it.download = this
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal val Download.requestData: RequestExtraData? get() = runCatching {
    Cbor.decodeFromByteArray<RequestExtraData>(request.data)
}.getOrNull()

internal val Download.downloadState: DownloadState get() = when (state) {
    Download.STATE_QUEUED -> DownloadState.Pending
    Download.STATE_DOWNLOADING -> DownloadState.Downloading
    Download.STATE_COMPLETED -> DownloadState.Completed
    Download.STATE_FAILED -> DownloadState.Failed
    Download.STATE_STOPPED -> {
        if (stopReason == Download.STOP_REASON_NONE) {
            DownloadState.Downloading
        } else {
            DownloadState.Paused
        }
    }
    else -> {
        Log.e(msg = "downloadState: $state")
        DownloadState.NotSet
    }
}