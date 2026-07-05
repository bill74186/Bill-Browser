package com.bill.browser

import android.webkit.WebView

data class Tab(
    val id: Int,
    val webView: WebView,
    var title: String = "",
    var url: String = ""
)
