package box.media.downloader.internal

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultDecoderFactory
import androidx.media3.transformer.ExoPlayerAssetLoader
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.InAppMp4Muxer
import androidx.media3.transformer.Transformer
import box.media.downloader.MediaDownloader
import box.media.downloader.util.Log
import box.media.downloader.util.safeResume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
internal suspend fun MediaItem.transformM3u8ToMp4(context: Context, cache: Cache, outPath: String): ExportResult
= withContext(MediaDownloader.coroutineContext) {
    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(
            CacheDataSource.Factory()
                .setCache(cache)
                .setCacheWriteDataSinkFactory(null)
        )
    val assetLoaderFactory = ExoPlayerAssetLoader.Factory(
        context,
        DefaultDecoderFactory.Builder(context).build(),
        Clock.DEFAULT,
        mediaSourceFactory)
    suspendCancellableCoroutine { c ->
        val transformer = Transformer.Builder(context)
//        .setVideoMimeType(MimeTypes.VIDEO_H264) // 输出H.264编码
//        .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setAssetLoaderFactory(assetLoaderFactory)
            .setMuxerFactory(InAppMp4Muxer.Factory())
            .addListener(object : Transformer.Listener {
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    Log.e(msg = "transform onError: ${exportException.message}")
                    c.safeResume(exportResult)
                }
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    Log.i(msg = "transform onCompleted: ${result.width}x${result.height}, ${result.videoMimeType}")
                    c.safeResume(result)
                }
            })
            .build()
        transformer.start(this@transformM3u8ToMp4, outPath)
    }
}