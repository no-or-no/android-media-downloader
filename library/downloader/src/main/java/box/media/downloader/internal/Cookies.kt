package box.media.downloader.internal

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.ConcurrentHashMap

internal object Cookies : CookieJar {
    private val map = ConcurrentHashMap<HttpUrl, List<Cookie>>()

    operator fun set(url: String, cookie: String?) {
        if (cookie.isNullOrEmpty()) return
        val url = url.toHttpUrlOrNull() ?: return
        val cookie = Cookie.parse(url, cookie) ?: return
        val list = map[url] ?: emptyList()
        if (cookie in list) return

        if (list is MutableList) {
            list += cookie
        } else if (list.isNotEmpty()) {
            map[url] = list + cookie
        } else {
            map[url] = arrayListOf(cookie)
        }
    }

    fun remove(url: String) {
        val url = url.toHttpUrlOrNull() ?: return
        map.remove(url)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return map[url] ?: emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        map[url] = cookies
    }
}