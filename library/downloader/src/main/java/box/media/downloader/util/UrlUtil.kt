package box.media.downloader.util

import androidx.core.net.toUri
import androidx.media3.common.util.Util

object UrlUtil {
    // https://vod3.cf.dmcdn.net/sec2(LbZwxZQsGRuTF5EPXcOvjYQOed-0cqjxGFkccSMsi2K6YtiHdDxJao4I-WV-0s00VzJubEr8WzvMTpT4Sn2-Q6zwsLXPsylnW_1v1nIiwWyuQIFFkwVSw5YbQcgZ7buRXBIm53FNZ3RQ2zbEH1WhghB4bcD6ppsJb3iypWb_fuPKCXV1RDFO4rRq6bj7XuO6)/video/281/268/572862182_mp4_h264_aac_hq_1.m3u8#cell=cf3
    fun getFileExtension(url: String): String {
        return url
            .substringBeforeLast('#') // Strip the fragment.
            .substringBeforeLast('?') // Strip the query.
            .substringAfterLast('/') // Get the last path segment.
            .substringAfterLast('.', missingDelimiterValue = "") // Get the file extension.
            .lowercase()
    }

    fun getFileName(url: String, withoutExt: Boolean = false): String {
        val filename = url
            .substringBeforeLast('#') // Strip the fragment.
            .substringBeforeLast('?') // Strip the query.
            .substringAfterLast('/') // Get the last path segment.
        return if (withoutExt) {
            filename.substringBeforeLast('.', missingDelimiterValue = "") // Get the file name.
        } else {
            filename
        }
    }

    fun getMediaMimeType(url: String): String {
        val extension = getFileExtension(url)
        val contentType = if (extension.isEmpty()) {
            Util.inferContentType(url.toUri())
        } else {
            Util.inferContentTypeForExtension(extension)
        }
        return Util.getAdaptiveMimeTypeForContentType(contentType).orEmpty()
    }

}
