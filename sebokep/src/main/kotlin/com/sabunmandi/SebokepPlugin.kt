package com.sabunmandi

import android.content.Context
import com.lagradost.cloudstream3.extractors.FileMoon
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class SebokepPlugin : BasePlugin() {
    override fun load() {
        // All providers should be added in this manner. Please don't edit the providers list
        // directly.
        registerMainAPI(Sebokep())
        // Register FileMoon extractor
        // registerExtractorAPI(FileMoonTo())
        // Register FileMoon extractor
        registerExtractorAPI(FileMoon())
        registerExtractorAPI(Pemersatu())
    }
}
