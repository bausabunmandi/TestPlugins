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
import com.lagradost.cloudstream3.extractors.LuluStream

class FileMoonTo : Filesim() {
    override var mainUrl = "https://filemoon.to"
    override val name = "FileMoonTo"
}

class Pemersatu : JWPlayer() {
    override var mainUrl = "https://fem.pemersatu.link"
    override val name = "Pemersatu"
}

class Lulust : LuluStream() {
    override var mainUrl = "https://lulu.st"
    override val name = "Lulustream"
}


