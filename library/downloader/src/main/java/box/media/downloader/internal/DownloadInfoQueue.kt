@file:Suppress("NOTHING_TO_INLINE")

package box.media.downloader.internal

import androidx.collection.MutableScatterMap
import box.media.downloader.DownloadInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * 下载任务队列
 */
internal class DownloadInfoQueue {

    private val deque = ArrayDeque<DownloadInfo>()
    private val map = MutableScatterMap<String, DownloadInfo>()
    private val signal = MutableStateFlow(0)

    val size: Int get() = deque.size
    val indices: IntRange get() = deque.indices

    inline fun isEmpty(): Boolean = size == 0

    fun add(info: DownloadInfo, sort: Boolean = true) {
        if (map[info.url] != null) return
        map[info.url] = info
        deque.addLast(info)
        if (sort) {
            sort()
        }
        notifyUpdate()
    }

    operator fun get(index: Int): DownloadInfo = deque[index]

    operator fun get(url: String): DownloadInfo? {
        return map[url]
    }

    fun sort() {
        deque.sortBy { it.createTime }
    }

    fun remove(url: String) {
        map.remove(url)
        deque.removeIf { it.url == url }
        notifyUpdate()
    }

    fun clear() {
        map.clear()
        deque.clear()
        notifyUpdate()
    }

    fun notifyUpdate() {
        signal.update { it + 1 }
    }

    fun observeUpdate(scope: CoroutineScope, action: suspend () -> Unit): Job {
        return signal.onEach { action() }.launchIn(scope)
    }

    fun asList(): List<DownloadInfo> {
        return deque
    }

}