package box.media.downloader

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class DownloadState internal constructor(val value: Int) {
    companion object {
        val NotSet = DownloadState(0)
        val Pending = DownloadState(1)
        val Downloading = DownloadState(3)
        val Transforming = DownloadState(8)
        val Paused = DownloadState(9)
        val Completed = DownloadState(10)
        val Failed = DownloadState(-1)
    }

    override fun toString(): String = when (this) {
        NotSet -> "NotSet"
        Pending -> "Pending"
        Downloading -> "Downloading"
        Transforming -> "Transforming"
        Paused -> "Paused"
        Completed -> "Completed"
        Failed -> "Failed"
        else -> "Unknown($value)"
    }
}
