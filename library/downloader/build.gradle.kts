plugins {
    id("plugin.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "box.media.downloader"
}

dependencies {
    api(libs.androidx.core.ktx)

    // 序列化
    implementation(libs.kotlinx.serialization.cbor)

    // 多媒体
    api(libs.media3.common.ktx)
    implementation(libs.media3.datasource)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.transformer)
    implementation(libs.media3.muxer)
    implementation(libs.media3.exoplayer.hls)
}