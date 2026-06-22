package com.ultimateRoadRunner.game

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bridge: AndroidBridge
    private lateinit var bannerContainer: FrameLayout
    private lateinit var consentInformation: ConsentInformation

    private val TAG = "UltimateRoadRunner"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_main)

        webView         = findViewById(R.id.webView)
        bannerContainer = findViewById(R.id.bannerContainer)
        bridge          = AndroidBridge(this, webView)

        setupWebView()
        requestConsent()
    }

    /* ── UMP Consent (kept ready for AdMob) ──────────────────────────────── */

    private fun requestConsent() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(this, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                    if (formError != null) Log.e(TAG, "Consent form error: ${formError.message}")
                }
            },
            { error -> Log.e(TAG, "Consent update failed: ${error.message}") }
        )
    }

    /* ── WebView ──────────────────────────────────────────────────────────── */

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        // Required for WebGL 3D content (GLB models, Three.js renderer)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.settings.apply {
            javaScriptEnabled                = true
            domStorageEnabled                = true
            allowFileAccess                  = true
            allowContentAccess               = true
            databaseEnabled                  = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode                        = WebSettings.LOAD_DEFAULT
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)
        }

        webView.addJavascriptInterface(bridge, "AndroidBridge")
        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html")
    }

    /* ── Banner container (ready for AdMob) ──────────────────────────────── */

    fun showBanner() {
        runOnUiThread { bannerContainer.visibility = View.VISIBLE }
    }

    fun hideBanner() {
        runOnUiThread { bannerContainer.visibility = View.GONE }
    }

    /* ── Lifecycle ────────────────────────────────────────────────────────── */

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.evaluateJavascript("window.onResumeGame && window.onResumeGame()", null)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.evaluateJavascript("window.onPauseGame && window.onPauseGame()", null)
    }

    override fun onBackPressed() { /* keep player in game */ }
}
