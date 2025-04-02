package com.sabunmandi

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
// https://avtub.men/category/bokep-indo/
// avtub.app
class Avtub : MainAPI() {
    override var mainUrl = "https://avtub.men/"
    override var name = "Avtub"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)
    // override val hasDownloadSupport = false

    override val mainPage = mainPageOf(
        "$mainUrl/category/bokep-indo/?filter=latest" to "Latest",
        "$mainUrl/category/bokep-indo/?filter=most-viewed" to "Most Viewed",
        "$mainUrl/category/bokep-indo/?filter=random" to "Random"
    )

    // Helper extension function
    fun String?.fixUrl(): String {
        return when {
            this.isNullOrBlank() -> ""
            startsWith("http") -> this
            startsWith("//") -> "https:$this"
            startsWith("/") -> "$mainUrl$this"
            else -> "$mainUrl/$this"
        }.replace("/(?<=[^:]/)".toRegex(), "/")
    }

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        // val targetUrl = "${request.data}/page/${page}/"

        // Parse base URL and existing query parameters
        val (basePath, queryParams) = request.data.split("?", limit = 2).let {
            it[0].removeSuffix("/") to it.getOrNull(1)
        }
    
        // Build paginated URL
        val targetUrl = buildString {
            append(basePath)
            if (page > 1) append("/page/$page")
            append("/")  // Ensure trailing slash
            if (!queryParams.isNullOrEmpty()) append("?$queryParams")
        }
        
        println("DEBUG : TARGET URL :  $targetUrl : PAGE : $page")
        
        val document = app.get(targetUrl).document
        
        val items = document.select(".site-main article").mapNotNull { article ->
            // println("DEBUGXXXXXXXXX :  $article")
            // Your existing item parsing logic
            // val content = article.selectFirst(".featured-content-image") ?: return@mapNotNull null
            val href = article.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val title = article.selectFirst(".post-thumbnail img")?.attr("alt") ?: "No Title"
            val poster = article.selectFirst(".post-thumbnail img")?.attr("data-src")

            // println("CONTENTTTTTTXXXXX :  $href")

            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }

        // Pagination detection (same for all categories)
        // val hasNext = document.select("ul.pagination").let { pagination ->
        //     pagination?.last()?.select("li:nth-last-child(2) a:contains(Next)")?.firstOrNull()?.let {
        //         it.text().equals("Next", ignoreCase = true) && it.attr("href").contains("/page/")
        //     } ?: false
        // }

        val hasNext = document.select(".pagination ul").any { pagination ->
            pagination.select("a").any { link ->
                link.text().equals("Next", ignoreCase = true) &&
                !link.hasClass("disabled") &&
                link.attr("href").contains("page")
            }
        }

        println("DEBUG : HAS NEXT :  $hasNext")

        return newHomePageResponse(
            listOf(HomePageList(request.name, items)),
            hasNext = hasNext
        )
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
        // if (videoUrl.isNullOrEmpty()) {
        //     println("DEBUG - No data video URL found! : $url | $videoUrl")
        // }
        // val videoUrl = document.selectFirst("video source")?.attr("src")?.trim()
        // ?: document.selectFirst("iframe")?.attr("src")?.trim()
        // ?: throw ErrorLoadingException("No video found")
        // val videoUrl = document.selectFirst(".video-player iframe")?.attr("src")?.trim() ?: ""
        val initialIframeUrl = document.selectFirst(".video-player iframe")?.attr("src")?.fixUrl()
        ?: throw ErrorLoadingException("No video iframe found")
        val videoUrl = initialIframeUrl
        // val videoUrl = resolveNestedIframe(initialIframeUrl)

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
            println("===========================")

            // 1. Load the main document and extract the iframe URL.
            val mainDoc = app.get(data).document

            println("DEBUG : $data")
            
            // 3. Extract the packed JS snippet using the common packer pattern.
            val extractedPack = mainDoc
                .selectFirst("script:containsData(sources)")
                ?.html()
            
            println("DEBUG - Extracted packed JS: $extractedPack")
            
            // 4. Unpack the JavaScript using the CloudStream JsUnpacker utility.
            val unPacked = JsUnpacker(extractedPack).unpack() 
                ?: throw ErrorLoadingException("Unpacking failed")
            println("DEBUG - Unpacked JS: $unPacked")
            
            // 5. Extract the HLS master URL dynamically from the unpacked script.
            // This regex will match any URL starting with http or https that ends with .m3u8 and includes any query parameters.
            // val masterUrl: String = Regex("""sources:\[\{\s*file:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""")
            //     .find(unPacked)
            //     ?.groupValues?.get(1)
            //     ?: throw ErrorLoadingException("HLS URL not found in unpacked script")

            val urlCandidates = Regex("""["']([a-zA-Z0-9_]+)["']\s*:\s*["']([^"']+\.m3u8[^"']*)["']""")
                .findAll(unPacked)
                .map { it.groupValues[2] }
                .toList()

            if (urlCandidates.isEmpty()) {
                throw ErrorLoadingException("HLS URL not found in unpacked script")
            }

            val masterUrl = urlCandidates.first()


            println("DEBUG - MASTER_URL: $masterUrl")

            
            // 7. Return the extracted link via the callback.
            callback.invoke(
                ExtractorLink(
                    source = name,
                    name = "CDN Stream",
                    url = masterUrl,
                    referer = "https://cybervynx.com/",
                    quality = Qualities.Unknown.value,
                    isM3u8 = true
                )
            )
            
            return true
    
        } catch (e: Exception) {
            println("ERROR - ${e.javaClass.simpleName}")
            println("ERROR - Message: ${e.message?.take(50)}")
            println("ERROR - ${e.stackTraceToString()}")
            return false
        }
    }

    private suspend fun resolveNestedIframe(url: String, depth: Int = 3): String {
        println("Resolving iframe (depth ${4 - depth}): $url")
        if (depth <= 0) throw ErrorLoadingException("Maximum iframe depth reached")

        val doc = app.get(url, referer = mainUrl).document
        
        val newUrl = doc.selectFirst("iframe")?.attr("src")?.fixUrl()
        if(!newUrl.isNullOrEmpty()) {
            return resolveNestedIframe(newUrl, depth - 1)
        }

        return url

        // val t = doc.selectFirst("video")?.attr("src")
        // println("document debug : $t")
        
        // // Check for direct video source first
        // doc.selectFirst("video")?.attr("src")?.fixUrl()?.let {
        //     return it
        // }
        
        // // Check for nested iframe
        // val newUrl = doc.selectFirst("iframe")?.attr("src")?.fixUrl()
        //     ?: throw ErrorLoadingException("No video source found")
        
    }
    
    
    

    // override suspend fun loadLinks(
    //     data: String,
    //     isCasting: Boolean,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    //     callback: (ExtractorLink) -> Unit
    // ): Boolean {
    //     try {
    //         val document = app.get(data).document
            
    //         // 1. Find JWPlayer script
    //         val script = document.select("script:containsData(sources)").toString()
    //         println("SCRIPT_CONTENT: $script") // Check via ADB logcat

    //         // // 2. Extract JWPlayer setup configuration
    //         // val jwConfig = Regex("jwplayer\\(.*?\\)\\.setup\\(\\s*(\\{.*?\\})\\s*\\)", RegexOption.DOT_MATCHES_ALL)
    //         //     .find(script)
    //         //     ?.groupValues?.get(1)
    //         //     ?: throw ErrorLoadingException("JWPlayer config not found")

    //         // println("JW_CONFIG: $jwConfig")

    //         // // 3. Extract HLS master URL
    //         // val masterUrl = Regex("""file:\s*["'](.*?\.m3u8[^"']*)["']""")
    //         //     .find(jwConfig)
    //         //     ?.groupValues?.get(1)
    //         //     ?.replace("\\/", "/")
    //         //     ?: throw ErrorLoadingException("HLS URL not found")

    //         val iframeUrl = document.selectFirst(".video-player iframe")?.attr("src") 

    //         val iframeDoc = app.get(iframeUrl).document

    //         val extractedPack = iframeDoc
    //             .selectFirst("script:containsData(function(p,a,c,k,e,d))")
    //             ?.data()
    //             .toString()
    //             .trim()

    //         if (extractedPack.isEmpty()) {
    //             throw ErrorLoadingException("Packed JS not found")
    //         }
    //         println("DEBUG - Extracted packed JS: $extractedPack")

    //         val unPacked = JsUnpacker(extractedPack).unpack() 
    //             ?: throw ErrorLoadingException("Unpacking failed")
    //         println("DEBUG - Unpacked JS: $unPacked")

    //         val masterUrl = Regex("""sources:\[\{\s*file:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""")
    //             .find(unPacked)
    //             ?.groupValues?.get(1)
    //             ?: throw ErrorLoadingException("HLS URL not found in unpacked script")

    //         println("MASTER_URL: $masterUrl")

    //         // 5. Return the HLS stream
    //         callback.invoke(
    //             ExtractorLink(
    //                 source = name,
    //                 name = "JWPlayer Stream",
    //                 url = masterUrl,
    //                 referer = data,
    //                 quality = Qualities.Unknown.value,
    //                 isM3u8 = true
    //             )
    //         )

    //         return true

    //     } catch (e: Exception) {
    //         println("LOAD_LINKS_ERROR: ${e.stackTraceToString()}")
    //         return false
    //     }
    // }

    // override suspend fun loadLinks(
    //     data: String,
    //     isCasting: Boolean,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    //     callback: (ExtractorLink) -> Unit
    // ): Boolean {
    //     try {
    //         val document = app.get(data).document
            
    //         // 1. Extract the iframe source and video ID
    //         val iframeUrl = document.selectFirst(".video-player iframe")?.attr("src") 
    //             ?: return false
    //         val videoId = iframeUrl.substringAfterLast("/e/").substringBefore("?").trim()
            
    //         // 2. Construct the master playlist URL with all parameters
    //         // val scriptContent = document.select("script:containsData(master.m3u8)").html()
    //         // val queryParams = Regex("""master\.m3u8\?(.*?)['"]""").find(scriptContent)?.groupValues?.get(1)
            
    //         // app.postNotification("Raw params: ${queryParams ?: "NULL"}")
    //         // println("DEBUG - Raw params: ${queryParams ?: "NULL"}")

    //         // val scriptContent = document.select("script:containsData(master.m3u8)").html()
    //         // val queryParams = Regex("""master\.m3u8\?(.*?)['"]""").find(scriptContent)?.groupValues?.get(1)
    //             // ?: throw ErrorLoadingException("Missing stream parameters")

    //         // val masterUrl = "https://vuvabh8vnota.cdn-centaurus.com/hls2/01/09302/${videoId}_n/master.m3u8?$queryParams"
    //         val iframeDoc = app.get(iframeUrl).document
    //         val scriptContent = iframeDoc.select("script:containsData(sources)").html()
    //         println("DEBUG - JWPlayer Script Content: $scriptContent")

    //         val masterUrl = Regex("""file:"(https://vuvabh8vnota\.cdn-centaurus\.com/hls2/01/09302/[^"]+)""")
    //             .find(scriptContent)?.groupValues?.get(1)
    //         // app.postNotification("Extracted file URL: ${fileUrl ?: "NULL"}")
    //         // println("DEBUG - Extracted file URL: ${fileUrl ?: "NULL"}")

    //         // Log.d("Pain", masterUrl)
    //         println("DEBUG - Master URL: $masterUrl")
    //         // 3. Create the extractor link
    //         callback.invoke(
    //             ExtractorLink(
    //                 source = name,
    //                 name = "CDN-Centaurus Stream",
    //                 url = masterUrl,
    //                 referer = "https://cybervynx.com/",
    //                 quality = Qualities.Unknown.value,
    //                 isM3u8 = true
    //             )
    //         )

    //         return true
    //     } catch (e: Exception) {
    //         return false
    //     }
    // }

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
