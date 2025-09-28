package com.sabunmandi

import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.JWPlayer
import com.lagradost.cloudstream3.utils.*

class FileMoonTo : Filesim() {
    override var mainUrl = "https://filemoon.to"
    override val name = "FileMoonTo"
}

class Davioad : JWPlayer() {
    override var mainUrl = "https://davioad.com"
    override val name = "davioad"
}
