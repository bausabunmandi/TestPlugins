package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.APIHolder
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import hascheme
import okhttp3.Request
import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

@CloudstreamPlugin
class ExampleSitePlugin : Plugin() {
    override fun load() {
        // Replace "ExampleSite" with your provider's name
        registerMainAPI("Igodesu", "https://igodesu.tv", "en")
    }

    @SuppressLint("SuspiciousIndentation")
    private fun registerMainAPI(name: String, baseUrl: String, lang: String) {
        registerMainAPI(
            name = name,
            mainUrl = baseUrl,
            lang = lang,
        ) { _, page ->

            // Homepage request
            val document = app.get(baseUrl, headers = Headers.headers("Referer" to baseUrl)).document

            // Parse video list
            val videoList = document.select(".post-list .video-item").mapNotNull { item ->
                val content = item.selectFirst(".featured-content-image")
                val link = content?.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val thumbnail = content.selectFirst("img")?.attr("src")
                val title = content.selectFirst("img")?.attr("alt") 
                    ?: content.selectFirst("a")?.attr("title") 
                    ?: "Untitled"

                MovieSearchResponse(
                    name = title,
                    url = link.hasScheme(),
                    posterUrl = thumbnail?.hasScheme(),
                    quality = "HD" // You can extract quality from item if available
                )
            }

            // Return the results
            return@registerMainAPI videoList
        }

        // Register video provider
        registerVideoProvider(
            name = name,
            mainUrl = baseUrl,
            logo = "" // Add logo URL if available
        ) { url, episode ->
            
            // Video page request
            val document = app.get(url, referer = baseUrl).document
            
            // Extract video URL - ADJUST THIS SELECTOR BASED ON ACTUAL VIDEO PAGE STRUCTURE
            val videoUrl = document.selectFirst("video source")?.attr("src")
                ?: document.selectFirst("iframe")?.attr("src")
            
            if (videoUrl != null) {
                // Handle iframe content if needed
                if (videoUrl.contains("http", true)) {
                    return@registerVideoProvider VideoResult(videoUrl.hasScheme())
                }
            }
            
            throw ErrorException("No video found")
        }
    }
}