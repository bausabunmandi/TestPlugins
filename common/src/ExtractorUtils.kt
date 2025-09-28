package com.sabunmandi.common

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.JsUnpacker
import java.net.URI
import org.jsoup.nodes.Document

object ExtractorUtils {
    suspend fun tryManualCheck(data: String): String? {
        val mainDoc: Document = app.get(data).document

        val masterUrl = mainDoc.selectFirst("video")?.attr("data-link")?.trim()
        if (!masterUrl.isNullOrEmpty()) {
            println("DEBUG: Found data-link trying manual extract link")
            println("DEBUG - MASTER_URL: $masterUrl")
            return masterUrl
        }

        println("DEBUG: data-link Not found, try extract js unpacker method")
        val extractedPack = mainDoc.selectFirst("script:containsData(sources)")?.html()
        if (!extractedPack.isNullOrEmpty()) {
            println("DEBUG - Extracted JS script block")

            val jsUnpacker = JsUnpacker(extractedPack)
            val unPacked =
                    if (jsUnpacker.detect()) {
                        println("DEBUG - Looks packed, running JsUnpacker")
                        jsUnpacker.unpack()
                    } else {
                        println("DEBUG - Script not packed, using raw script")
                        extractedPack
                    }
                            ?: extractedPack

            println("DEBUG - Final JS to parse: $unPacked")

            val urlCandidates =
                    Regex("""["']([^"']+\.m3u8[^"']*)["']""")
                            .findAll(unPacked)
                            .map { it.groupValues[1] }
                            .toList()

            println("DEBUG - urlcandidate : $urlCandidates")

            if (urlCandidates.isNotEmpty()) {
                var candidateURL = urlCandidates.first()
                println("DEBUG: URL: $candidateURL")
                if (!candidateURL.startsWith("http")) {
                    candidateURL = URI(data).resolve(candidateURL).toString()
                    println("DEBUG: URL doesnt have http, resolved to: $candidateURL")
                }
                return candidateURL
            }
        }
        return null
    }
}
