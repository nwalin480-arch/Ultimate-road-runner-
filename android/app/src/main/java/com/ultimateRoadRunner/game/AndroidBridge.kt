package com.ultimateRoadRunner.game

import android.webkit.JavascriptInterface
import android.webkit.WebView

class AndroidBridge(
    private val activity: MainActivity,
    private val webView: WebView
) {

    @JavascriptInterface
    fun showInterstitialAd() { activity.showInterstitialAd() }

    @JavascriptInterface
    fun showRewardedAd() { activity.showRewardedAd() }

    @JavascriptInterface
    fun showBanner() { activity.showAdBanner() }

    @JavascriptInterface
    fun hideBanner() { activity.hideAdBanner() }

    @JavascriptInterface
    fun onGameReady() {
        android.util.Log.d("AndroidBridge", "Game is ready")
    }

    @JavascriptInterface
    fun getLanguage(): String {
        return activity.resources.configuration.locales[0].language
    }

    fun callJS(functionName: String) {
        webView.post {
            webView.evaluateJavascript("window.$functionName && window.$functionName()", null)
        }
    }
}
