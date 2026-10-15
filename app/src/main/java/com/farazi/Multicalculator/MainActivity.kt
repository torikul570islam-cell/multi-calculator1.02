package com.farazi.multicalculator

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader
    private lateinit var bannerAdView: AdView
    private var bannerLoaded = false
    private var rewardedAd: RewardedAd? = null
    private var adLoading = false
    private var adShowing = false
    private var rewardEarned = false
    private var sdkReady = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 17, 28)
        window.navigationBarColor = Color.rgb(7, 17, 28)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7, 17, 28))
        }
        webView = WebView(this)
        root.addView(webView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        // Main_Bottom_Banner: native AdMob banner, visible only on the Templates page.
        bannerAdView = AdView(this).apply {
            adUnitId = BuildConfig.BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)
            visibility = View.GONE
        }
        root.addView(bannerAdView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        setContentView(root)
        assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.setSupportMultipleWindows(false)
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, url: String) = assetLoader.shouldInterceptRequest(android.net.Uri.parse(url))
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !url.startsWith("https://appassets.androidplatform.net/")
        }
        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        Thread {
            MobileAds.initialize(this) {
                runOnUiThread {
                    sdkReady = true
                    if (::bannerAdView.isInitialized && bannerAdView.visibility == View.VISIBLE && !bannerLoaded) {
                        bannerLoaded = true
                        bannerAdView.loadAd(AdRequest.Builder().build())
                    }
                    // Do not request an ad at launch. Ads are loaded only after a user taps the reward button.
                }
            }
        }.start()
    }

    private inner class AndroidBridge {
        @JavascriptInterface
        fun setTemplateBannerVisible(visible: Boolean) {
            runOnUiThread {
                bannerAdView.visibility = if (visible) View.VISIBLE else View.GONE
                if (visible && sdkReady && !bannerLoaded) {
                    bannerLoaded = true
                    bannerAdView.loadAd(AdRequest.Builder().build())
                }
            }
        }

        @JavascriptInterface
        fun showRewardedAd(requestedUnitId: String?) {
            runOnUiThread {
                if (!sdkReady) {
                    js("window.onAdRewardFailed('Ad service is still starting. Please try again in a moment.')")
                    return@runOnUiThread
                }
                if (adLoading || adShowing) {
                    js("window.onAdRewardFailed('An ad request is already in progress. Please wait.')")
                    return@runOnUiThread
                }
                loadAndShowRewardedAd()
            }
        }
    }

    private fun loadAndShowRewardedAd() {
        adLoading = true
        rewardEarned = false
        rewardedAd = null
        val request = AdRequest.Builder().build()
        RewardedAd.load(this, BuildConfig.REWARDED_AD_UNIT_ID, request,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoading = false
                    rewardedAd = null
                    js("window.onAdRewardFailed('Ad could not load. Check internet and try again.')")
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    adLoading = false
                    rewardedAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() { adShowing = true }
                        override fun onAdDismissedFullScreenContent() {
                            adShowing = false
                            rewardedAd = null
                            if (!rewardEarned) js("window.onAdRewardFailed('Ad was closed before the reward was earned. Templates remain locked.')")
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            adShowing = false
                            rewardedAd = null
                            js("window.onAdRewardFailed('Ad could not be shown. Please try again.')")
                        }
                    }
                    ad.show(this@MainActivity) {
                        rewardEarned = true
                        js("window.onAdRewardEarned()")
                    }
                }
            })
    }

    private fun js(script: String) {
        runOnUiThread { if (::webView.isInitialized) webView.evaluateJavascript(script, null) }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized) {
            webView.evaluateJavascript("(function(){var g=document.getElementById('adGateBackdrop');if(g&&g.classList.contains('show')){document.getElementById('cancelRewardAd').click();return 'handled'}if(!document.getElementById('historyPage').hidden){document.getElementById('historyHome').click();return 'handled'}if(!document.getElementById('toolPage').hidden){document.getElementById('backTemplates').click();return 'handled'}if(!document.getElementById('templatesPage').hidden){document.getElementById('templatesHome').click();return 'handled'}return 'home'})()") { result ->
                if (result == "\"home\"") super.onBackPressed()
            }
        } else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("Android")
            if (::bannerAdView.isInitialized) bannerAdView.destroy()
            webView.destroy()
        }
        super.onDestroy()
    }
}
