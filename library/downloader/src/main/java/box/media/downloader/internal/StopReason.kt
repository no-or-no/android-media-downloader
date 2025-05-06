package box.media.downloader.internal

import android.annotation.SuppressLint
import androidx.media3.exoplayer.offline.Download
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class StopReason(val value: Int) {
    companion object {
        @SuppressLint("UnsafeOptInUsageError")
        internal val None = StopReason(Download.STOP_REASON_NONE)
        val UserRequested = StopReason(1)
    }
}