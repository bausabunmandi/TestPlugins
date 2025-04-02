package com.sabunmandi

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType

class ExampleSite : MainAPI() {
    override var mainUrl = "https://igodesu.tv"
    override var name = "Example Site"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)
    override val hasDownloadSupport = false

    // =========================== Main Page ===========================
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val allItems = ArrayList<HomePageList>()

        val list = document.select(".post-list .video-item").mapNotNull { item ->
            val content = item.selectFirst(".featured-content-image") ?: return@mapNotNull null
            val href = content.selectFirst("a")?.attr("href")?.trim() ?: return@mapNotNull null
            val title = content.selectFirst("img")?.attr("alt")?.trim() ?: "No Title"
            val poster = content.selectFirst("img")?.attr("src")?.trim()

            newMovieSearchResponse(
                name = title,
                url = href,
                type = TvType.Movie,
            ) {
                this.posterUrl = poster
                // this.apiName = this@ExampleSitePlugin.name
                // quality = SearchQuality.HD // Add if quality info available
            }
        }

        if (list.isNotEmpty()) {
            allItems.add(HomePageList("Latest Videos", list))
        }

        return newHomePageResponse(allItems, hasNext = false)
    }

    // =========================== Search ===========================
    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=${query}"
        val document = app.get(searchUrl).document

        return document.select(".video-item").mapNotNull {
            val href = it.selectFirst("a")?.attr("href")?.trim() ?: return@mapNotNull null
            val title = it.selectFirst("img")?.attr("alt")?.trim() ?: "No Title"
            val poster = it.selectFirst("img")?.attr("src")?.trim()

            newMovieSearchResponse(
                name = title,
                url = href,
                type = TvType.Movie,
            ) {
                this.posterUrl = poster
                // this.apiName = this@ExampleSitePlugin.name
            }
        }
    }

    // =========================== Load Links ===========================
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        // Extract metadata from video-player div
        val videoPlayer = document.selectFirst(".video-player") 
            ?: throw ErrorLoadingException("Video player not found")
    
        // Get title from itemprop="name"
        val title = videoPlayer.select("meta[itemprop=name]")
            .attr("content")
            .trim()
            .ifEmpty { document.selectFirst("h1.title")?.text()?.trim() }
            ?: "No Title"
    
        // Get poster from itemprop="thumbnailUrl"
        val poster = videoPlayer.select("meta[itemprop=thumbnailUrl]")
            .attr("content")
            .trim()
            .ifEmpty { document.selectFirst(".featured-image img")?.attr("src")?.trim() }
    
        // Get description (example additional field)
        val description = document.selectFirst("meta[property='og:description']")
            ?.attr("content")
            ?.trim()
    
        // Extract video URL (from previous implementation)
        // val videoUrl = document.selectFirst(".video-player iframe")?.attr("src")?.trim()
    
        val videoUrl = document.selectFirst("video source")?.attr("src")?.trim()
        ?: document.selectFirst("iframe")?.attr("src")?.trim()
        ?: throw ErrorLoadingException("No video found")

        return newMovieLoadResponse(
            name = title,
            url = url,
            type = TvType.Movie,
            dataUrl = videoUrl,
        ) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
            val document = app.get(data).document
            
            // 1. Extract the iframe source and video ID
            val iframeUrl = document.selectFirst(".video-player iframe")?.attr("src") 
                ?: return false
            val videoId = iframeUrl.substringAfterLast("/e/").substringBefore("?").trim()
            
            // 2. Construct the master playlist URL with all parameters
            // val scriptContent = document.select("script:containsData(master.m3u8)").html()
            // val queryParams = Regex("""master\.m3u8\?(.*?)['"]""").find(scriptContent)?.groupValues?.get(1)
            
            // app.postNotification("Raw params: ${queryParams ?: "NULL"}")
            // println("DEBUG - Raw params: ${queryParams ?: "NULL"}")

            // val scriptContent = document.select("script:containsData(master.m3u8)").html()
            // val queryParams = Regex("""master\.m3u8\?(.*?)['"]""").find(scriptContent)?.groupValues?.get(1)
                // ?: throw ErrorLoadingException("Missing stream parameters")

            // val masterUrl = "https://vuvabh8vnota.cdn-centaurus.com/hls2/01/09302/${videoId}_n/master.m3u8?$queryParams"
            val iframeDoc = app.get(iframeUrl).document
            val scriptContent = iframeDoc.select("script:containsData(sources)").html()
            println("DEBUG - JWPlayer Script Content: $scriptContent")

            val masterUrl = Regex("""file:"(https://vuvabh8vnota\.cdn-centaurus\.com/hls2/01/09302/[^"]+)""")
                .find(scriptContent)?.groupValues?.get(1)
            // app.postNotification("Extracted file URL: ${fileUrl ?: "NULL"}")
            // println("DEBUG - Extracted file URL: ${fileUrl ?: "NULL"}")

            // Log.d("Pain", masterUrl)
            println("DEBUG - Master URL: $masterUrl")
            // 3. Create the extractor link
            callback.invoke(
                ExtractorLink(
                    source = name,
                    name = "CDN-Centaurus Stream",
                    url = masterUrl,
                    referer = "https://cybervynx.com/",
                    quality = Qualities.Unknown.value,
                    isM3u8 = true
                )
            )

            return true
        } catch (e: Exception) {
            return false
        }
    }

    // Add this to your plugin class
    // override suspend fun loadLinks(
    //     data: String,
    //     isCasting: Boolean,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    //     callback: (ExtractorLink) -> Unit
    // ): Boolean {
    //     try {
    //         val document = app.get(data).document
            
    //         // 1. Find StreamHG iframe
    //         val iframeSrc = document.select(".video-player iframe").attr("src")

    //         // 2. Process StreamHG URL
    //         // resolveStreamHG(iframeSrc, callback)
    //         loadExtractor(iframeSrc, subtitleCallback, callback)
    //         return true
    //     } catch (e: Exception) {
    //         return false
    //     }
    // }

    // private suspend fun resolveStreamHG(url: String, callback: (ExtractorLink) -> Unit) {
    //     val response = app.get(url, referer = mainUrl)
        
    //     // Extract encrypted source from script
    //     val scriptContent = response.document.select("script:containsData(sources)").html()
    //     val videoUrl = Regex("""sources:\s*\[\s*\{\s*file:\s*'(.*?)'""").find(scriptContent)
    //         ?.groupValues?.get(1)
    //         ?.replace("\\/", "/")
    //         ?: throw ErrorLoadingException("No StreamHG source found")

    //     // // Get quality from URL pattern
    //     // val quality = when {
    //     //     videoUrl.contains("/1080/") -> Qualities.FullHDP.value
    //     //     videoUrl.contains("/720/") -> Qualities.HD.value
    //     //     videoUrl.contains("/480/") -> Qualities.SD.value
    //     //     else -> Qualities.Unknown.value
    //     // }

    //     callback.invoke(
    //         ExtractorLink(
    //             source = name,
    //             name = name,
    //             url = videoUrl,
    //             referer = url,
    //             // quality = quality,
    //             isM3u8 = videoUrl.contains(".m3u8")
    //         )
    //     )
    // }
}