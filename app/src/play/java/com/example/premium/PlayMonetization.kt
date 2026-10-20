package com.example.premium

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.BuildConfig
import com.example.widget.triggerWidgetUpdate
import com.example.workers.PremiumExpiryWorker
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
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/** The Google Play build: Play Billing for "Premium forever", AdMob for the ads and videos, UMP for consent. */
fun createMonetization(app: Application): Monetization = PlayMonetization(app)

private class PlayMonetization(private val app: Application) : Monetization {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = PremiumStore(app, scope, onNextChange = { PremiumExpiryWorker.schedule(app, it) })
    private fun now() = System.currentTimeMillis()

    override val hasStore = true
    override val state: StateFlow<PremiumState> = store.state
    override val access: StateFlow<Access> = store.access

    private val _price = MutableStateFlow<String?>(null)
    override val price: StateFlow<String?> = _price.asStateFlow()

    private val _video = MutableStateFlow(VideoStatus.IDLE)
    override val video: StateFlow<VideoStatus> = _video.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** Consent is settled and the ads SDK is initialised: ads may load. */
    private val adsReady = MutableStateFlow(false)
    private val adsStarting = AtomicBoolean(false)

    private var started = false
    private var conversions = 0
    private var interstitialShownThisSession = false

    override fun start(activity: Activity) {
        if (!started) {
            started = true
            store.countSession()
            restorePurchases()
            loadProduct()
        }
        // Someone who bought Premium never sees ads, so there is nothing to ask consent for.
        if (store.access.value.adFree) return
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { afterConsent(consent) } },
            { afterConsent(consent) }
        )
        // A choice from an earlier session lets ads start right away.
        afterConsent(consent)
    }

    private fun afterConsent(consent: ConsentInformation) {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!consent.canRequestAds() || !adsStarting.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(app) { adsReady.value = true }
        }
    }

    override fun onResume() {
        store.refresh()
        // A pending purchase may have gone through, or a refund come back, while the app was away.
        if (started) restorePurchases()
    }

    // --- Premium forever (Play Billing) ---

    private val billing: BillingClient = BillingClient.newBuilder(app)
        .setListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> handlePurchases(purchases.orEmpty(), complete = false)
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
                else -> Unit
            }
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()
    private var product: ProductDetails? = null
    private val waitingForBilling = mutableListOf<() -> Unit>()
    private var connecting = false

    /** Runs [block] once Play Billing is connected (connecting first if needed). */
    private fun withBilling(block: () -> Unit) {
        if (billing.isReady) return block()
        waitingForBilling += block
        if (connecting) return
        connecting = true
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                scope.launch {
                    connecting = false
                    val ready = result.responseCode == BillingClient.BillingResponseCode.OK
                    val blocks = waitingForBilling.toList()
                    waitingForBilling.clear()
                    if (ready) blocks.forEach { it() }
                }
            }

            override fun onBillingServiceDisconnected() {
                scope.launch { connecting = false }
            }
        })
    }

    private fun loadProduct(then: (ProductDetails) -> Unit = {}) = withBilling {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        billing.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val found = details.productDetailsList.firstOrNull { it.productId == PRODUCT_ID } ?: return@queryProductDetailsAsync
            scope.launch {
                product = found
                _price.value = found.oneTimePurchaseOfferDetails?.formattedPrice
                then(found)
            }
        }
    }

    override fun buy(activity: Activity) {
        val launch = { details: ProductDetails ->
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
                )
                .build()
            withBilling { billing.launchBillingFlow(activity, params) }
            Unit
        }
        product?.let(launch) ?: loadProduct(launch)
    }

    override fun restorePurchases() = withBilling {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handlePurchases(purchases, complete = true)
        }
    }

    /**
     * [complete] = the full list from Google Play: no purchase in it means none (refunded, or another account).
     * Purchases are acknowledged, or Google Play refunds them after three days.
     */
    private fun handlePurchases(purchases: List<Purchase>, complete: Boolean) {
        val ours = purchases.filter { PRODUCT_ID in it.products }
        val bought = ours.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = ours.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        if (bought != null && !bought.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(bought.purchaseToken).build()
            billing.acknowledgePurchase(params) { }
        }
        scope.launch {
            store.update { state ->
                when {
                    bought != null -> state.copy(purchased = true, purchasePending = false)
                    complete -> state.copy(purchased = false, purchasePending = pending)
                    else -> state.copy(purchasePending = pending || state.purchasePending)
                }
            }
        }
    }

    // --- Rewarded videos: VIDEOS_PER_UNLOCK of them unlock Premium for REWARD_HOURS ---

    private var rewarded: RewardedAd? = null
    private var rewardedAt = 0L
    private var videoLoading = false
    /** The screen waiting to show a video that is still loading. */
    private var pendingWatch: WeakReference<Activity>? = null
    private var videoTimeout: Job? = null

    override fun preloadVideo(activity: Activity) {
        if (freshRewarded() == null) loadVideo(activity)
    }

    override fun watchVideo(activity: Activity) {
        val ad = freshRewarded()
        if (ad != null) return showVideo(activity, ad)
        pendingWatch = WeakReference(activity)
        _video.value = VideoStatus.LOADING
        videoTimeout?.cancel()
        videoTimeout = scope.launch {
            delay(VIDEO_WAIT_MS)
            if (_video.value == VideoStatus.LOADING) {
                pendingWatch = null
                _video.value = VideoStatus.UNAVAILABLE
            }
        }
        loadVideo(activity)
    }

    private fun freshRewarded(): RewardedAd? = rewarded?.takeIf { now() - rewardedAt < AD_FRESH_MS }

    private fun loadVideo(activity: Activity) {
        if (videoLoading || !adsReady.value) return
        videoLoading = true
        RewardedAd.load(activity, BuildConfig.AD_UNIT_REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                videoLoading = false
                rewarded = ad
                rewardedAt = now()
                val waiting = pendingWatch?.get()
                if (waiting != null && !waiting.isFinishing && _video.value == VideoStatus.LOADING) showVideo(waiting, ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                videoLoading = false
                if (_video.value == VideoStatus.LOADING) {
                    pendingWatch = null
                    videoTimeout?.cancel()
                    _video.value = VideoStatus.UNAVAILABLE
                }
            }
        })
    }

    private fun showVideo(activity: Activity, ad: RewardedAd) {
        rewarded = null
        pendingWatch = null
        videoTimeout?.cancel()
        _video.value = VideoStatus.SHOWING
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                _video.value = VideoStatus.IDLE
                // Halfway to an unlock: have the second video ready.
                if (!store.access.value.unlocked || store.state.value.videosNeeded(now()) < PremiumRules.VIDEOS_PER_UNLOCK) {
                    loadVideo(activity)
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                _video.value = VideoStatus.UNAVAILABLE
            }
        }
        ad.show(activity) { store.update { PremiumRules.afterVideo(it, now()) } }
    }

    // --- Full-screen ads at natural breaks, and the one-time review request ---

    private var interstitial: InterstitialAd? = null
    private var interstitialAt = 0L
    private var interstitialLoading = false

    override fun noteConversion() {
        conversions++
        if (conversions == 1) preloadInterstitial()
    }

    private fun interstitialAllowedSoon(): Boolean {
        val access = store.access.value
        return !access.interstitialsPaused && now() - store.installedAt >= InterstitialPolicy.GRACE_AFTER_INSTALL_MS &&
            !interstitialShownThisSession && store.interstitialsOn(now()) < InterstitialPolicy.MAX_PER_DAY
    }

    private fun preloadInterstitial() {
        if (!adsReady.value || interstitialLoading || !interstitialAllowedSoon()) return
        if (interstitial != null && now() - interstitialAt < AD_FRESH_MS) return
        interstitialLoading = true
        InterstitialAd.load(app, BuildConfig.AD_UNIT_INTERSTITIAL, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialLoading = false
                interstitial = ad
                interstitialAt = now()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialLoading = false
            }
        })
    }

    override fun onBreak(activity: Activity) {
        val now = now()
        val ad = interstitial?.takeIf { now - interstitialAt < AD_FRESH_MS }
        val mayShow = InterstitialPolicy.mayShow(
            access = store.access.value,
            now = now,
            installedAt = store.installedAt,
            lastShownAt = store.lastInterstitialAt,
            shownToday = store.interstitialsOn(now),
            conversionsThisSession = conversions,
            shownThisSession = interstitialShownThisSession,
        )
        if (ad != null && mayShow) {
            interstitial = null
            interstitialShownThisSession = true
            store.markInterstitialShown(now)
            ad.show(activity)
            return
        }
        if (ReviewPolicy.mayAsk(store.reviewAsked, now, store.installedAt, store.sessions, conversions, interstitialShownThisSession)) {
            store.markReviewAsked()
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful && !activity.isFinishing) manager.launchReviewFlow(activity, task.result)
            }
        }
    }

    override fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    // --- Banner ---

    @Composable
    override fun Banner(placeholder: @Composable () -> Unit, modifier: Modifier) {
        val access by access.collectAsState()
        if (access.adFree) return
        val ready by adsReady.collectAsState()
        BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val context = LocalContext.current
            val widthDp = maxWidth.value.toInt()
            val size = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
            var loaded by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth().height(size.height.dp), contentAlignment = Alignment.Center) {
                if (ready) {
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    val adView = remember {
                        AdView(context).apply {
                            setAdSize(size)
                            adUnitId = BuildConfig.AD_UNIT_BANNER
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    loaded = true
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                    DisposableEffect(lifecycle, adView) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE -> adView.pause()
                                Lifecycle.Event.ON_RESUME -> adView.resume()
                                else -> Unit
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose {
                            lifecycle.removeObserver(observer)
                            adView.destroy()
                        }
                    }
                    AndroidView(factory = { adView }, modifier = Modifier.width(size.width.dp).height(size.height.dp))
                }
                // On top until an ad arrives, so a tap there opens Premium rather than an empty ad view.
                if (!loaded) placeholder()
            }
        }
    }

    // After every property above is set.
    init {
        // Locking or unlocking changes the widgets (locked screen, colour theme).
        scope.launch {
            store.access.map { it.unlocked }.distinctUntilChanged().drop(1).collect { triggerWidgetUpdate(app, settle = true) }
        }
        scope.launch {
            adsReady.collect { ready ->
                if (!ready) return@collect
                pendingWatch?.get()?.let { loadVideo(it) }
            }
        }
    }

    private companion object {
        const val PRODUCT_ID = "premium_forever"
        /** Loaded ads expire after an hour. */
        const val AD_FRESH_MS = 50 * 60 * 1000L
        const val VIDEO_WAIT_MS = 15_000L
    }
}
