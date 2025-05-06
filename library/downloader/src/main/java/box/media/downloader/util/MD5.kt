@file:Suppress("NOTHING_TO_INLINE")

package box.media.downloader.util

import java.security.MessageDigest

internal fun ByteArray.md5(offset: Int = 0, length: Int = this.size, upperCase: Boolean = false): String {
    try {
        val md = MessageDigest.getInstance("MD5")
        md.update(this, offset, length)
        return md.digest().toHex(upperCase)
    } catch (e: Exception) {
        Log.e(e = e)
    }
    return ""
}

internal inline fun String.md5(upperCase: Boolean = false): String {
    return toByteArray().md5(upperCase = upperCase)
}
