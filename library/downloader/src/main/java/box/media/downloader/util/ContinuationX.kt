package box.media.downloader.util

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> Continuation<T>.safeResume(value: T) {
    if (this is CancellableContinuation<T>) {
        if (isActive) {
            resume(value)
        }
    } else {
        try {
            resume(value)
        } catch (e: IllegalStateException) {
            if (e.message?.lowercase()?.contains("already resumed") != true) throw e
        } catch (e: Exception) {
            throw e
        }
    }
}