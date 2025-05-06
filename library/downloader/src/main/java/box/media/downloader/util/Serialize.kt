package box.media.downloader.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

@OptIn(ExperimentalSerializationApi::class)
internal val Cbor = Cbor {
    encodeDefaults = true
    ignoreUnknownKeys = true
}