package com.sabunmandi

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class ExampleSitePlugin : MainAPI() {
    override var mainUrl = "https://igodesu.tv"  // Replace with your site URL
    override var name = "Example Site"  // Replace with your provider name
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)  // Change to appropriate type
    override val hasDownloadSupport = false

    // =========================== Main Page ===========================
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val allItems = ArrayList<HomePageList>()

        document.select(".post-list .video-item").mapNotNull { item ->
            // Extract common elements
            val content = item.selectFirst(".featured-content-image") ?: return@mapNotNull null
            val href = content.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val title = content.selectFirst("img")?.attr("alt") ?: "No Title"
            val poster = content.selectFirst("img")?.attr("src")
            
            MovieSearchResponse(
                name = title,
                url = href,
                apiName = this.name,
                type = TvType.Movie,  // Change to appropriate type
                posterUrl = poster
            )
        }.let { 
            allItems.add(HomePageList("Latest Videos", it))
        }

        return HomePageResponse(allItems)
    }

    // =========================== Search ===========================
    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=${query}"
        val document = app.get(searchUrl).document

        return document.select(".video-item").mapNotNull { 
            val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val title = it.selectFirst("img")?.attr("alt") ?: "No Title"
            val poster = it.selectFirst("img")?.attr("src")

            MovieSearchResponse(
                name = title,
                url = href,
                apiName = this.name,
                type = TvType.Movie,
                posterUrl = poster
            )
        }
    }

    // =========================== Load Links ===========================
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        // Extract details
        val title = document.selectFirst("h1.title")?.text() ?: "No Title"
        val poster = document.selectFirst(".featured-image img")?.attr("src")
        val description = document.selectFirst(".description")?.text()
        
        // Extract video URL (adjust selector based on actual page structure)
        val videoUrl = document.selectFirst("video source")?.attr("src") 
            ?: document.selectFirst("iframe")?.attr("src")
            ?: throw ErrorLoadingException("No video found")

        return MovieLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            type = TvType.Movie,
            dataUrl = videoUrl,
            posterUrl = poster,
            plot = description,
            year = null  // Add year extraction if available
        )
    }
}