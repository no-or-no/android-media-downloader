package box.media.downloader.util

import java.util.Locale

private const val HexChars = "0123456789ABCDEF"

fun ByteArray?.toHex(upperCase: Boolean = true): String {
    this ?: return ""

    val HexCharArray = if (upperCase) {
        HexChars.toCharArray()
    } else {
        HexChars.lowercase(Locale.US).toCharArray()
    }

    val chars = CharArray(size * 2)
    forEachIndexed { i, b ->
        val v = b.toInt() and 0xFF
        chars[i * 2] = HexCharArray[v ushr 4]
        chars[i * 2 + 1] = HexCharArray[v and 0x0F]
    }
    return String(chars)
}

fun String.hexToBytes(): ByteArray = ByteArray(length / 2) {
    val i = it * 2
    ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
}