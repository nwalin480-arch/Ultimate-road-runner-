package com.ultimateRoadRunner.game

import android.webkit.JavascriptInterface
import android.webkit.WebView

class AndroidBridge(
    private val activity: MainActivity,
    private val webView: WebView
) {

    // Ad methods — stubs until AdMob is wired up
    @JavascriptInterface
    fun showInterstitialAd() { /* AdMob will handle this */ }

    @JavascriptInterface
    fun showRewardedAd() { /* AdMob will handle this */ }

    @JavascriptInterface
    fun showBanner() { /* AdMob will handle this */ }

    @JavascriptInterface
    fun hideBanner() { /* AdMob will handle this */ }

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
