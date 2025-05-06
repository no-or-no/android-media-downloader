package box.media.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler

@OptIn(UnstableApi::class)
class MediaDownloadService : DownloadService(
    DOWNLOAD_FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.m_download_notification_channel_name,
    0,
) {
    companion object {
        private const val JOB_ID = 1

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return

            val managerCompat = NotificationManagerCompat.from(context)
            if (managerCompat.getNotificationChannel(DOWNLOAD_NOTIFICATION_CHANNEL_ID) == null) {
                // 创建通知渠道
                val channel = NotificationChannel(DOWNLOAD_NOTIFICATION_CHANNEL_ID, DOWNLOAD_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).also {
                    it.enableLights(false) //闪光灯
                    it.enableVibration(false) //震动
                    it.setSound(null, null) //设置静音
                    it.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC // 锁屏界面不展示 VISIBILITY_SECRET
                }
                // 会弹出系统权限
                managerCompat.createNotificationChannel(channel)
            }
        }
    }

    private val notificationHelper by lazy {
        DownloadNotificationHelper(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager {
        return MediaDownloader.requireDownloadManager()
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, JOB_ID)
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return notificationHelper.buildProgressNotification(
            this,
            R.drawable.m_ic_download,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }

}