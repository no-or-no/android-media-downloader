package box.media.downloader.util

import android.util.Log as AndroidLog

internal object Log {
    const val TAG = "MediaBox"
    var enable = false
    var delegate: ((level: Int, tag: String, msg: String, e: Throwable?) -> Unit)? = null

    fun i(tag: String? = null, msg: String? = null, e: Throwable? = null) {
        if (!enable) return

        val t = if (tag.isNullOrEmpty()) TAG else "$TAG-$tag"

        val log = delegate
        if (log != null) {
            log(AndroidLog.INFO, t, msg.orEmpty(), e)
        } else {
            AndroidLog.i(t, msg.orEmpty(), e)
        }
    }

    fun e(tag: String? = null, msg: String? = null, e: Throwable? = null) {
        if (!enable) return

        val t = if (tag.isNullOrEmpty()) TAG else "$TAG-$tag"

        val log = delegate
        if (log != null) {
            log(AndroidLog.ERROR, t, msg.orEmpty(), e)
        } else {
            AndroidLog.e(t, msg.orEmpty(), e)
        }
    }
}