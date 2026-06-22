package com.ultimateRoadRunner.game

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bridge: AndroidBridge
    private lateinit var bannerContainer: FrameLayout
    private lateinit var consentInformation: ConsentInformation

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var bannerAdView: AdView? = null

    private val TAG = "UltimateRoadRunner"

    companion object {
        private const val INTERSTITIAL_ID    = "ca-app-pub-1078340192803579/6497437223"
        private const val BANNER_ID          = "ca-app-pub-1078340192803579/8852137824"
        private const val REWARDED_ID        = "ca-app-pub-1078340192803579/1245110540"
        const val PRIVACY_POLICY_URL         = "https://ultimate-road-runner-policy.sharoncmons03.replit.app"
    }

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

    private fun requestConsent() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(this, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                    if (formError != null) Log.e(TAG, "Consent error: ${formError.message}")
                    initAdMob()
                }
            },
            { error ->
                Log.e(TAG, "Consent update failed: ${error.message}")
                initAdMob()
            }
        )
    }

    private fun initAdMob() {
        MobileAds.initialize(this) {
            Log.d(TAG, "AdMob initialized")
            loadInterstitial()
            loadRewarded()
            loadBanner()
        }
    }

    private fun loadInterstitial() {
        InterstitialAd.load(this, INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial()
                            callJS("onInterstitialAdClosed")
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            loadInterstitial()
                            callJS("onInterstitialAdFailed")
                        }
                    }
                    callJS("onInterstitialAdLoaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial load failed: ${error.message}")
                    interstitialAd = null
                    callJS("onInterstitialAdFailed")
                }
            }
        )
    }

    fun showInterstitialAd() {
        runOnUiThread {
            val ad = interstitialAd
            if (ad != null) {
                ad.show(this)
            } else {
                callJS("onInterstitialAdFailed")
                loadInterstitial()
            }
        }
    }

    private fun loadRewarded() {
        RewardedAd.load(this, REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            loadRewarded()
                            callJS("onRewardedAdClosed")
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            rewardedAd = null
                            loadRewarded()
                            callJS("onRewardedAdFailed")
                        }
                    }
                    callJS("onRewardedAdLoaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded load failed: ${error.message}")
                    rewardedAd = null
                    callJS("onRewardedAdFailed")
                }
            }
        )
    }

    fun showRewardedAd() {
        runOnUiThread {
            val ad = rewardedAd
            if (ad != null) {
                ad.show(this) { callJS("onRewardedAdRewarded") }
            } else {
                callJS("onRewardedAdFailed")
                loadRewarded()
            }
        }
    }

    private fun loadBanner() {
        runOnUiThread {
            val adView = AdView(this)
            adView.adUnitId = BANNER_ID
            adView.setAdSize(AdSize.BANNER)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() { callJS("onBannerAdShown") }
            }
            bannerContainer.removeAllViews()
            bannerContainer.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
            bannerAdView = adView
        }
    }

    fun showAdBanner() {
        runOnUiThread {
            bannerContainer.visibility = View.VISIBLE
            callJS("onBannerAdShown")
        }
    }

    fun hideAdBanner() {
        runOnUiThread {
            bannerContainer.visibility = View.GONE
            callJS("onBannerAdHidden")
        }
    }

    fun openPrivacyPolicy() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
            startActivity(intent)
        }
    }

    fun callJS(functionName: String) {
        webView.post {
            webView.evaluateJavascript("window.$functionName && window.$functionName()", null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

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

    override fun onResume() {
        super.onResume()
        webView.onResume()
        bannerAdView?.resume()
        webView.evaluateJavascript("window.onResumeGame && window.onResumeGame()", null)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        bannerAdView?.pause()
        webView.evaluateJavascript("window.onPauseGame && window.onPauseGame()", null)
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {}
}
