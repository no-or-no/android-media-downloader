package box.media.downloader.internal

import kotlinx.serialization.Serializable

@Serializable
internal data class RequestExtraData(
    val originUrl: String = "",
    val title: String = "",
    val coverUrl: String? = null,
    val cookie: String? = null,
    val createTime: Long = 0,
    val coverLocalPath: String? = null,
)