// Version: ${System.currentTimeMillis()}
package com.sabunmandi

// import com.lagradost.api.Log
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.utils.*

class FileMoonTo : Filesim() {
    override var mainUrl = "https://filemoon.to"
    override val name = "FileMoonTo"
}

class Pemersatu : ExtractorApi() {
    override val mainUrl = "https://fem.pemersatu.link"
    override val name = "Pemersatu"
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        try {

            Log.d("PemersatuExtractor", "getUrl() called with URL: $url")

            val src = app.get(url, allowRedirects = false)
            // Log.e("PemersatuExtractor", "HEADERS: ${src.headers}")
            val playbackRegex = Regex("""videoplayback\.php\?[^"']+""")
            val playbackPath = playbackRegex.find(src.text)?.value
            val playbackUrl =
                    if (playbackPath?.startsWith("http") == true) {
                        playbackPath
                    } else {
                        mainUrl + "/" + playbackPath
                    }
            val response = app.get(playbackUrl, allowRedirects = false)
            Log.e("PemersatuExtractor", "Header: ${response.headers["location"]}")

            // Log.e("PemersatuExtractor", "HTML: ${src.text}")
            // Log.e("PemersatuExtractor", "REGEX: ${playbackPath}")

            // loadExtractor(url, subtitleCallback, callback)
            callback.invoke(
                    newExtractorLink(
                            name = name,
                            source = name,
                            url = response.headers["location"] ?: url
                    ) {
                        this.referer = ""
                        this.quality = Qualities.Unknown.value
                    }
            )
        } catch (e: Exception) {
            // This will log the error if the network request fails for any reason
            Log.e("PemersatuExtractor", "Error in getUrl: ${e.message}", e)
        }
    }
}
