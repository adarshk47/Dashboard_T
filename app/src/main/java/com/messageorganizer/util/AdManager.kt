package com.messageorganizer.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    const val BANNER_ID    = "ca-app-pub-4083049312549641/5537795532"
    // Replace with real interstitial ID when ready, using test ID for now
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) {}
        isInitialized = true
    }

    fun loadBanner(adView: AdView) {
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.d("AdManager", "Interstitial failed: ${error.message}")
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismiss: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismiss()
                }
            }
            ad.show(activity)
        } else {
            onDismiss()
        }
    }
}
