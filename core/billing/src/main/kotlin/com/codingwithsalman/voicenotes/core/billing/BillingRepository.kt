package com.codingwithsalman.voicenotes.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class ProPricing(
    val weeklyPrice: String? = null,
    val monthlyPrice: String? = null,
    val lifetimePrice: String? = null,
    /** Free-trial length in days for each subscription, if the console configured a trial offer. */
    val weeklyTrialDays: Int? = null,
    val monthlyTrialDays: Int? = null,
)

/**
 * Play Billing wrapper: connects lazily, restores purchases, exposes localized
 * prices, launches purchase flows, acknowledges, and writes the Pro entitlement.
 * Product ids must exist in the Play Console (owner setup) before prices load —
 * the UI degrades gracefully until then.
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entitlementStore: EntitlementStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pricing = MutableStateFlow(ProPricing())
    val pricing: StateFlow<ProPricing> = _pricing.asStateFlow()

    val isPro = entitlementStore.isPro

    private var weeklyDetails: ProductDetails? = null
    private var monthlyDetails: ProductDetails? = null
    private var lifetimeDetails: ProductDetails? = null

    // Kept private so the billing SDK types never leak into consumers' classpath.
    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        // Billing 8 removed the no-arg overload; one-time products = our pro_lifetime INAPP.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Reconnected lazily on the next purchase attempt / app start.
            }
        })
    }

    private fun queryProducts() {
        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(PRODUCT_WEEKLY, PRODUCT_MONTHLY).map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            ).build()
        client.queryProductDetailsAsync(subParams) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                queryResult.productDetailsList.forEach { details ->
                    when (details.productId) {
                        PRODUCT_WEEKLY -> {
                            weeklyDetails = details
                            _pricing.value = _pricing.value.copy(
                                weeklyPrice = details.recurringPrice(),
                                weeklyTrialDays = details.trialDays(),
                            )
                        }
                        PRODUCT_MONTHLY -> {
                            monthlyDetails = details
                            _pricing.value = _pricing.value.copy(
                                monthlyPrice = details.recurringPrice(),
                                monthlyTrialDays = details.trialDays(),
                            )
                        }
                    }
                }
            }
        }

        val inappParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        client.queryProductDetailsAsync(inappParams) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                lifetimeDetails = queryResult.productDetailsList.firstOrNull()
                _pricing.value = _pricing.value.copy(
                    lifetimePrice = lifetimeDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                )
            }
        }
    }

    /**
     * Re-derives the entitlement from BOTH product types in one pass so a lapsed
     * subscription (and no lifetime) correctly REVOKES Pro, while an active
     * purchase of either kind grants it.
     */
    fun restorePurchases() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS).build()
        ) { subResult, subPurchases ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP).build()
            ) { inappResult, inappPurchases ->
                if (subResult.responseCode == BillingClient.BillingResponseCode.OK &&
                    inappResult.responseCode == BillingClient.BillingResponseCode.OK
                ) {
                    val all = subPurchases + inappPurchases
                    acknowledgeNew(all)
                    val owned = all.any { purchase ->
                        // An active free trial is reported as PURCHASED, so this grants Pro during
                        // the trial and revokes it once the (unconverted) subscription lapses.
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.any {
                                it == PRODUCT_WEEKLY || it == PRODUCT_MONTHLY || it == PRODUCT_LIFETIME
                            }
                    }
                    scope.launch { entitlementStore.setPro(owned) }
                }
            }
        }
    }

    fun launchWeekly(activity: Activity) = launchFlow(activity, weeklyDetails, isSub = true)

    fun launchMonthly(activity: Activity) = launchFlow(activity, monthlyDetails, isSub = true)

    fun launchLifetime(activity: Activity) = launchFlow(activity, lifetimeDetails, isSub = false)

    private fun launchFlow(activity: Activity, details: ProductDetails?, isSub: Boolean) {
        val product = details ?: run {
            connect() // products not loaded (likely not configured in the console yet)
            return
        }
        val paramsDetail = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .apply {
                if (isSub) {
                    // Prefer the free-trial offer so eligible users actually start the trial.
                    product.trialEligibleOffer()?.offerToken?.let(::setOfferToken)
                }
            }
            .build()
        client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(paramsDetail))
                .build(),
        )
    }

    /** Grant-only path for live onPurchasesUpdated events (revocation is restore's job). */
    private fun handlePurchases(purchases: List<Purchase>) {
        acknowledgeNew(purchases)
        val owned = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any {
                    it == PRODUCT_WEEKLY || it == PRODUCT_MONTHLY || it == PRODUCT_LIFETIME
                }
        }
        if (owned) scope.launch { entitlementStore.setPro(true) }
    }

    private fun acknowledgeNew(purchases: List<Purchase>) {
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { purchase ->
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                ) { ack -> Log.i(TAG, "ack=${ack.responseCode}") }
            }
    }

    // --- Subscription offer helpers ---------------------------------------------------------

    /** The offer that includes a free (zero-price) phase, if the console configured a trial. */
    private fun ProductDetails.trialEligibleOffer(): ProductDetails.SubscriptionOfferDetails? {
        val offers = subscriptionOfferDetails ?: return null
        return offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        } ?: offers.firstOrNull()
    }

    /**
     * The recurring (post-trial) price for the paywall — the last non-free pricing phase. Using the
     * FIRST phase would show the trial's "Free"/zero price once a trial offer exists.
     */
    private fun ProductDetails.recurringPrice(): String? {
        val phases = trialEligibleOffer()?.pricingPhases?.pricingPhaseList ?: return null
        return phases.lastOrNull { it.priceAmountMicros > 0L }?.formattedPrice
            ?: phases.lastOrNull()?.formattedPrice
    }

    /** Free-trial length in days, if the offer has a free phase (e.g. "P3D" -> 3, "P1W" -> 7). */
    private fun ProductDetails.trialDays(): Int? {
        val free = trialEligibleOffer()?.pricingPhases?.pricingPhaseList
            ?.firstOrNull { it.priceAmountMicros == 0L } ?: return null
        val m = Regex("""P(?:(\d+)W)?(?:(\d+)D)?""").matchEntire(free.billingPeriod) ?: return null
        val days = (m.groupValues[1].toIntOrNull() ?: 0) * 7 + (m.groupValues[2].toIntOrNull() ?: 0)
        return days.takeIf { it > 0 }
    }

    companion object {
        const val PRODUCT_WEEKLY = "pro_weekly"
        const val PRODUCT_MONTHLY = "pro_monthly"
        const val PRODUCT_LIFETIME = "pro_lifetime"
        private const val TAG = "VnBilling"
    }
}
