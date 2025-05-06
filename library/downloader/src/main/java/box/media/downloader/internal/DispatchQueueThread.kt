@file:Suppress("Unused")

package box.media.downloader.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.android.asCoroutineDispatcher
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class DispatchQueueThread(name: String) {
    val dispatcher by lazy { handler.asCoroutineDispatcher(name) }

    private val thread = DispatcherThread(name)
    private inline val name get() = thread.name
    private inline val handler get() = thread.handler

    fun isCurrentThread(): Boolean {
        return Thread.currentThread() === thread
    }

    @JvmSynthetic
    fun post(delay: Duration = Duration.ZERO, run: () -> Unit): Runnable {
        val runnable = Runnable(run)
        post(runnable, delay.inWholeMilliseconds)
        return runnable
    }

    @JvmOverloads
    fun post(run: Runnable, delayMillis: Long = 0) {
        try {
            handler.postDelayed(run, delayMillis)
        } catch (e: Throwable) {
            if (delayMillis <= 0) {
                Log.e(name, "post", e)
            } else {
                Log.e(name, "post delay ${delayMillis.milliseconds}", e)
            }
        }
    }

    fun remove(run: Runnable) {
        try {
            handler.removeCallbacks(run)
        } catch (e: Throwable) {
            Log.e(name, "cancel", e)
        }
    }

    @JvmOverloads
    fun sendMessage(msg: Message, delayMillis: Long = 0) {
        try {
            handler.sendMessageDelayed(msg, delayMillis)
        } catch (e: Throwable) {
            if (delayMillis <= 0) {
                Log.e(name, "sendMessage", e)
            } else {
                Log.e(name, "sendMessage delay $delayMillis", e)
            }
        }
    }

    fun clear() {
        try {
            handler.removeCallbacksAndMessages(null)
        } catch (e: Throwable) {
            Log.e(name, "clean", e)
        }
    }

    fun recycle() {
        try {
            handler.looper.quit()
        } catch (e: Throwable) {
            Log.e(name, "recycle", e)
        }
    }

    fun interrupt() {
        try {
            thread.interrupt()
        } catch (e: Throwable) {
            Log.e(name, "interrupt", e)
        }
    }

    private class DispatcherThread(name: String): Thread(name) {
        private val lock = CountDownLatch(1)
        @Volatile
        private var _handler: Handler? = null

        val handler: Handler get() {
            lock.await()
            return _handler!!
        }

        init { start() }

        override fun run() {
            Looper.prepare()
            _handler = Handler(Looper.myLooper()!!)
            lock.countDown()
            Looper.loop()
        }
    }

}
