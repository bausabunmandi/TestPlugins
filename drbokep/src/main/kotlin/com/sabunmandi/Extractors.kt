package com.sabunmandi

import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.extractors.FileMoonSx
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.JWPlayer

class FileMoonTo : Filesim() {
    override var mainUrl = "https://filemoon.to"
    override val name = "FileMoonTo"
}