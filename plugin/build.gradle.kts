plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(libs.android.gradle.plugin)
    implementation(plugin(libs.plugins.kotlin.android))
}

fun plugin(plugin: Provider<PluginDependency>) = plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }


gradlePlugin {
    plugins {
        create("libraryPlugin") {
            id = "plugin.library"
            implementationClass = "LibraryPlugin"
        }
    }
}