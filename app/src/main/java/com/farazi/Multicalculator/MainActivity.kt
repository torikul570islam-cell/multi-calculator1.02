package com.farazi.multicalculator

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.adsbase.VideoListener
import com.startapp.sdk.ads.banner.Banner

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader
    private var isAdShowing = false
    private lateinit var startIoBanner: Banner

    companion object {
        private const val TAG = "FaraziStartIO"
        private const val START_IO_APP_ID = "209664286"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use the current Start.io initialization flow and wait for SDK readiness.
        // Test ads are enabled only in Debug builds; remove test mode from release builds.
        StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG)
        StartAppSDK.initParams(applicationContext, START_IO_APP_ID)
            .setReturnAdsEnabled(false)
            .setCallback {
                Log.i(TAG, "Start.io SDK initialization completed; debug=${BuildConfig.DEBUG}")
            }
            .init()
        StartAppAd.disableSplash()

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
        // Native Start.io banner with callbacks so load/display failures are visible in Logcat.
        startIoBanner = Banner(this, object : BannerListener {
            override fun onReceiveAd(view: View) {
                Log.i(TAG, "Banner loaded successfully")
            }

            override fun onFailedToReceiveAd(view: View) {
                Log.e(TAG, "Banner failed to load (no fill or network/SDK error)")
            }

            override fun onImpression(view: View) {
                Log.i(TAG, "Banner impression recorded")
            }

            override fun onClick(view: View) {
                Log.i(TAG, "Banner clicked")
            }
        })
        startIoBanner.visibility = View.GONE
        root.addView(startIoBanner, LinearLayout.LayoutParams(
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
            override fun shouldInterceptRequest(view: WebView, url: String) = 
                assetLoader.shouldInterceptRequest(Uri.parse(url))
            
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (!url.startsWith("https://appassets.androidplatform.net/")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // Handle exception if no browser available
                    }
                    return true
                }
                return false
            }
        }

        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    private inner class AndroidBridge {
        @JavascriptInterface
        fun setTemplateBannerVisible(visible: Boolean) {
            // Legacy callback retained for compatibility.
        }

        @JavascriptInterface
        fun showStartIoBanner(visible: Boolean) {
            runOnUiThread {
                if (::startIoBanner.isInitialized) {
                    startIoBanner.visibility = if (visible) View.VISIBLE else View.GONE
                    Log.d(TAG, "Banner visibility requested: $visible")
                }
            }
        }

        @JavascriptInterface
        fun showRewardedAd(requestedUnitId: String?) {
            runOnUiThread {
                if (isAdShowing) {
                    Log.w(TAG, "Rewarded ad request ignored: an ad is already in progress")
                    return@runOnUiThread
                }
                isAdShowing = true
                Log.i(TAG, "Requesting Start.io rewarded video; debug=${BuildConfig.DEBUG}")

                val startAppAd = StartAppAd(this@MainActivity)
                startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : com.startapp.sdk.adsbase.adlisteners.AdEventListener {
                    override fun onReceiveAd(p0: com.startapp.sdk.adsbase.Ad) {
                        Log.i(TAG, "Rewarded video loaded successfully")
                        startAppAd.setVideoListener(object : VideoListener {
                            override fun onVideoCompleted() {
                                Log.i(TAG, "Rewarded video completed; granting 7-day unlock")
                                isAdShowing = false
                                js("window.onAdRewardEarned()")
                            }
                        })
                        val shown = startAppAd.showAd()
                        if (!shown) {
                            Log.e(TAG, "Rewarded video was loaded but showAd() returned false")
                            isAdShowing = false
                            js("window.onAdRewardFailed('Ad could not be shown. Please try again.')")
                        }
                    }

                    override fun onFailedToReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                        Log.e(TAG, "Rewarded video failed to load; ad=$p0")
                        isAdShowing = false
                        js("window.onAdRewardFailed('Start.io could not load a rewarded ad. Check Logcat for FaraziStartIO details, then try again.')")
                    }
                })
            }
        }

        @JavascriptInterface
        fun openPrivacyPolicy() {
            runOnUiThread {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/privacy-policy-farazi-group/home"))
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }
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
            webView.destroy()
        }
        super.onDestroy()
    }
}
