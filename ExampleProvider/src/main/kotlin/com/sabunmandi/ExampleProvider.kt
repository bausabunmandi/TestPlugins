package com.sabunmandi

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

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
        val videoUrl = document.selectFirst(".video-player iframe")?.attr("src")
            ?.replace("https://cybervynx.com/e/", "https://vuvabh8vnota.cdn-centaurus.com/hls2/01/09302/")
            ?.let { "$it/index.m3u8" }
            ?: throw ErrorLoadingException("Video URL not found")
    
        return newMovieLoadResponse(
            title, 
            url, 
            TvType.Movie
            dataUrl = videoUrl
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
            
            val iframeUrl = document.selectFirst(".video-player iframe")
                ?.attr("src")
                ?.replace("https://cybervynx.com/e/", "https://vuvabh8vnota.cdn-centaurus.com/hls2/01/09302/")
                ?.let { "$it/index.m3u8" }
                ?: return false
    
            callback.invoke(
                ExtractorLink(
                    source = name,
                    name = "Direct Stream",  // Single display name
                    url = iframeUrl,
                    referer = "https://cybervynx.com/",
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