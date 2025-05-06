@file:Suppress("DEPRECATION")
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginContainer
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_OPTIONS_DSL_NAME

internal inline val Project.isRoot: Boolean get() {
    return this == rootProject
}

internal inline fun <reified T> Project.createExtension(name: String): T {
    return extensions.create(name, T::class.java)
}

internal inline fun <reified T> Project.extension(apply: T.() -> Unit = {}): T {
    return extensions.getByType(T::class.java).apply(apply)
}

internal inline fun <reified T: Task> Project.createTask(name: String): T {
    return tasks.create(name, T::class.java)
}

internal inline val Project.enableCompose: Boolean get() {
    return plugins.hasPlugin("org.jetbrains.kotlin.plugin.compose")
}

// 参考 org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPlugin
internal inline fun LibraryExtension.kotlinOptions(crossinline block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.getByName<KotlinJvmOptions>(KOTLIN_OPTIONS_DSL_NAME).apply(block)
}

// 参考 org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPlugin
internal inline fun ApplicationExtension.kotlinOptions(crossinline block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.getByName<KotlinJvmOptions>(KOTLIN_OPTIONS_DSL_NAME).apply(block)
}

internal fun Project.plugins(block: PluginDependenciesSpecScope.() -> Unit) {
    PluginDependenciesSpecScope(plugins).apply(block)
}

internal class PluginDependenciesSpecScope(private val plugins: PluginContainer) {
    fun id(id: String) {
        plugins.apply(id)
    }
}

internal fun String.uppercaseFirstLetter(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.uppercaseChar() else it
    }
}

internal fun String.lowercaseFirstLetter(): String {
    return replaceFirstChar {
        if (it.isUpperCase()) it.lowercaseChar() else it
    }
}

internal inline fun String?.ifNullOrEmpty(block: () -> String): String {
    return if (isNullOrEmpty()) block() else this
}