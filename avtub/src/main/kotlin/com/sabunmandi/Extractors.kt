package com.sabunmandi

import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.extractors.FileMoonSx

class FileMoonTo : FileMoonSx() {
    override var mainUrl = "https://filemoon.to"
}
