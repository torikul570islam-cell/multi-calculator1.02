package com.farazi.multicalculator

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader
    private var isAdShowing = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Start.io SDK with App ID: 209664286
        StartAppSDK.init(this, "209664286", false)
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
            // Main banner visibility handled by frontend/hybrid rotation if needed
        }

        @JavascriptInterface
        fun showRewardedAd(requestedUnitId: String?) {
            runOnUiThread {
                if (isAdShowing) return@runOnUiThread
                isAdShowing = true

                val startAppAd = StartAppAd(this@MainActivity)
                startAppAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : com.startapp.sdk.adsbase.adlisteners.AdEventListener {
                    override fun onReceiveAd(p0: com.startapp.sdk.adsbase.Ad) {
                        isAdShowing = false
                        startAppAd.showAd(object : AdDisplayListener {
                            override fun adHidden(p0: com.startapp.sdk.adsbase.Ad) {
                                js("window.onAdRewardEarned()")
                            }
                            override fun adDisplayed(p0: com.startapp.sdk.adsbase.Ad) {}
                            override fun adClicked(p0: com.startapp.sdk.adsbase.Ad) {}
                            override fun notDisplayed(p0: com.startapp.sdk.adsbase.Ad) {
                                js("window.onAdRewardFailed('Ad could not be shown. Please try again.')")
                            }
                        })
                    }

                    override fun onFailedToReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                        isAdShowing = false
                        js("window.onAdRewardFailed('Ad could not load. Check internet and try again.')")
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
