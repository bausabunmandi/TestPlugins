package com.sabunmandi

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.extractors.FileMoon
import android.content.Context

@CloudstreamPlugin
class SebokepPlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(Sebokep())
        // Register FileMoon extractor
        // registerExtractorAPI(FileMoonTo())
        // Register FileMoon extractor
        registerExtractorAPI(FileMoon())
        registerExtractorAPI(Pemersatu())
    }
}