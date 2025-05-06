package box.media.downloader.util

internal inline fun String?.ifNullOrEmpty(block: () -> String): String {
    return if (this.isNullOrEmpty()) block() else this
}