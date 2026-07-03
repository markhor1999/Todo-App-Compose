package com.codingwithsalman.voicenotes.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
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
    val monthlyPrice: String? = null,
    val lifetimePrice: String? = null,
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
        .enablePendingPurchases()
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
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()
        client.queryProductDetailsAsync(subParams) { result, products ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                monthlyDetails = products.firstOrNull()
                val price = monthlyDetails?.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                _pricing.value = _pricing.value.copy(monthlyPrice = price)
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
        client.queryProductDetailsAsync(inappParams) { result, products ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                lifetimeDetails = products.firstOrNull()
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
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.any { it == PRODUCT_MONTHLY || it == PRODUCT_LIFETIME }
                    }
                    scope.launch { entitlementStore.setPro(owned) }
                }
            }
        }
    }

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
                    product.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let(::setOfferToken)
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
                purchase.products.any { it == PRODUCT_MONTHLY || it == PRODUCT_LIFETIME }
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

    companion object {
        const val PRODUCT_MONTHLY = "pro_monthly"
        const val PRODUCT_LIFETIME = "pro_lifetime"
        private const val TAG = "VnBilling"
    }
}
