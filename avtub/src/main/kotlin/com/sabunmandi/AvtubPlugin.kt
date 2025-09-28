package com.sabunmandi

import android.content.Context
import com.lagradost.cloudstream3.extractors.FileMoon
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AvtubPlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list
        // directly.
        registerMainAPI(Avtub())
        // Register FileMoon extractor
        // registerExtractorAPI(FileMoonTo())
        // Register FileMoon extractor
        registerExtractorAPI(FileMoonArt())
        registerExtractorAPI(FileMoon())
    }
}
