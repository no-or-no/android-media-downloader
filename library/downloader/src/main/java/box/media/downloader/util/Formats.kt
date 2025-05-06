package box.media.downloader.util

import java.text.DecimalFormat
import kotlin.math.log
import kotlin.math.pow

private val bytesFormat by lazy { DecimalFormat("0.##") }

fun Long.formatedBytes(): String {
    if (this < 0) return ""
    if (this == 0L) return "0B"

    return when (log(toDouble(), 1024.0).toInt()) {
        0 -> bytesFormat.format(this) + "B"
        1 -> bytesFormat.format(this / 1024.0.pow(1)) + "KB"
        2 -> bytesFormat.format(this / 1024.0.pow(2)) + "MB"
        3 -> bytesFormat.format(this / 1024.0.pow(3)) + "GB"
        else -> bytesFormat.format(this / 1024.0.pow(4)) +"TB"
    }
}