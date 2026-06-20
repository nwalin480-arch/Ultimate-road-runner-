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
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.ads.banners.BannerErrorInfo
import com.unity3d.ads.banners.BannerView
import com.unity3d.ads.banners.IUnityBannerListener
import com.unity3d.ads.banners.UnityBannerSize

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bridge: AndroidBridge
    private lateinit var bannerContainer: FrameLayout
    private lateinit var consentInformation: ConsentInformation

    private var bannerView: BannerView? = null
    private var adsInitialized = false

    private val GAME_ID                = "800076272"
    private val INTERSTITIAL_PLACEMENT = "Interstitial_Android"
    private val REWARDED_PLACEMENT     = "Rewarded_Android"
    private val BANNER_PLACEMENT       = "Banner_Android"
    private val TAG                    = "UltimateRoadRunner"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    /* ── UMP Consent ──────────────────────────────────────────────────────── */

    private fun requestConsent() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(this, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                    if (formError != null) Log.e(TAG, "Consent form error: ${formError.message}")
                    if (consentInformation.canRequestAds()) initAds()
                }
            },
            { error ->
                Log.e(TAG, "Consent update failed: ${error.message}")
                if (consentInformation.canRequestAds()) initAds()
            }
        )
        if (consentInformation.canRequestAds()) initAds()
    }

    /* ── Unity Ads Init ───────────────────────────────────────────────────── */

    private fun initAds() {
        if (adsInitialized) return
        adsInitialized = true

        UnityAds.initialize(this, GAME_ID, false, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d(TAG, "Unity Ads initialized")
                loadInterstitial()
                loadRewarded()
                loadBanner()
            }
            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String) {
                Log.e(TAG, "Unity Ads init failed: $message")
            }
        })
    }

    /* ── WebView ──────────────────────────────────────────────────────────── */

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled                = true
            domStorageEnabled                = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode                        = WebSettings.LOAD_DEFAULT
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        webView.addJavascriptInterface(bridge, "AndroidBridge")
        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html")
    }

    /* ── Banner ───────────────────────────────────────────────────────────── */

    private fun loadBanner() {
        bannerView = BannerView(this, BANNER_PLACEMENT, UnityBannerSize.getDynamicSize(this))
        bannerView!!.listener = object : IUnityBannerListener {
            override fun onBannerLoaded(view: BannerView) {
                Log.d(TAG, "Banner loaded")
            }
            override fun onBannerShown(view: BannerView) {}
            override fun onBannerClick(view: BannerView) {}
            override fun onBannerFailedToLoad(view: BannerView, errorInfo: BannerErrorInfo) {
                Log.e(TAG, "Banner failed: ${errorInfo.errorMessage}")
            }
            override fun onBannerLeftApplication(view: BannerView) {}
        }
        bannerView!!.load()
    }

    fun showBanner() {
        runOnUiThread {
            if (bannerView != null) {
                if (bannerView!!.parent == null) {
                    bannerContainer.addView(bannerView)
                }
                bannerContainer.visibility = View.VISIBLE
                bridge.callJS("onBannerAdShown")
            }
        }
    }

    fun hideBanner() {
        runOnUiThread {
            bannerContainer.visibility = View.GONE
            bridge.callJS("onBannerAdHidden")
        }
    }

    /* ── Interstitial ─────────────────────────────────────────────────────── */

    private fun loadInterstitial() {
        UnityAds.load(INTERSTITIAL_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d(TAG, "Interstitial loaded")
                bridge.callJS("onInterstitialAdLoaded")
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                Log.e(TAG, "Interstitial load failed: $message")
            }
        })
    }

    fun showInterstitial() {
        if (UnityAds.isReady(INTERSTITIAL_PLACEMENT)) {
            UnityAds.show(this, INTERSTITIAL_PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                    bridge.callJS("onInterstitialAdClosed")
                    loadInterstitial()
                }
                override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                    bridge.callJS("onInterstitialAdFailed")
                    loadInterstitial()
                }
                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}
            })
        } else {
            bridge.callJS("onInterstitialAdFailed")
            loadInterstitial()
        }
    }

    /* ── Rewarded ─────────────────────────────────────────────────────────── */

    private fun loadRewarded() {
        UnityAds.load(REWARDED_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d(TAG, "Rewarded loaded")
                bridge.callJS("onRewardedAdLoaded")
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                Log.e(TAG, "Rewarded load failed: $message")
            }
        })
    }

    fun showRewarded() {
        if (UnityAds.isReady(REWARDED_PLACEMENT)) {
            UnityAds.show(this, REWARDED_PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        bridge.callJS("onRewardedAdRewarded")
                    }
                    bridge.callJS("onRewardedAdClosed")
                    loadRewarded()
                }
                override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                    bridge.callJS("onRewardedAdFailed")
                    loadRewarded()
                }
                override fun onUnityAdsShowStart(placementId: String) {}
                override fun onUnityAdsShowClick(placementId: String) {}
            })
        } else {
            bridge.callJS("onRewardedAdFailed")
            loadRewarded()
        }
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

    override fun onDestroy() {
        bannerView?.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {}
}
