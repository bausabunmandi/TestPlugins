package com.sabunmandi

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class ExampleSitePlugin : MainAPI() {
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
        
        val title = document.selectFirst("h1.title")?.text()?.trim() ?: "No Title"
        val poster = document.selectFirst(".featured-image img")?.attr("src")?.trim()
        val description = document.selectFirst(".description")?.text()?.trim()
        
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
            // this.apiName = this@ExampleSitePlugin.name
            // this.contentRating = ContentRating.Unknown // Update if site provides rating info
            // Add other fields if available:
            // year = 2023
            // duration = 120
            // rating = 8
        }
    }
}