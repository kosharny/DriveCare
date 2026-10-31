package com.example.drivecare.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.drivecare.BuildConfig
import com.example.drivecare.R
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BillingState(
    val pro: Boolean = false,
    val checked: Boolean = false,
    val configured: Boolean = false,
    val price: String? = null,
    val busy: Boolean = false,
    val message: Int? = null
)

@Singleton
class BillingRepository @Inject constructor(@ApplicationContext context: Context) {

    private val mutableState =
        MutableStateFlow(
            BillingState(configured = BuildConfig.REVENUECAT_PUBLIC_KEY.startsWith("goog_"))
        )
    val state = mutableState.asStateFlow()
    private var lifetime: Package? = null
    private val configured = BuildConfig.REVENUECAT_PUBLIC_KEY.startsWith("goog_")

    private fun debug(message: String) {
        if (BuildConfig.DEBUG) Log.i("DriveCareBilling", message)
    }

    init {
        debug("Configuration available: $configured")

        if (configured) {

            Purchases.configure(
                PurchasesConfiguration.Builder(context, BuildConfig.REVENUECAT_PUBLIC_KEY).build()
            )
            Purchases.sharedInstance.updatedCustomerInfoListener = {
                applyInfo(it)
            }
            refresh()
        } else {

            mutableState.update {
                it.copy(checked = true, message = R.string.purchase_unavailable)
            }
        }
    }

    private fun applyInfo(info: CustomerInfo) {
        debug("Entitlement pro active: ${info.entitlements["pro"]?.isActive == true}")
        mutableState.update {
            it.copy(pro = info.entitlements["pro"]?.isActive == true, checked = true)
        }
    }

    fun refresh() {
        if (!configured) return
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error ->
                debug("Customer info failed: ${error.code}")
                mutableState.update { state ->
                    state.copy(checked = true)
                }
            },
            onSuccess = {
                applyInfo(it)
            }
        )
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                debug("Offerings failed: ${error.code}")
                mutableState.update { state ->
                    state.copy(message = R.string.purchase_unavailable)
                }
            },
            onSuccess = { offerings ->
                lifetime =
                    offerings["default"]?.lifetime?.takeIf {
                        it.product.id == "drivecare_pro_lifetime"
                    }
                debug("Expected lifetime package available: ${lifetime != null}")
                mutableState.update {
                    it.copy(
                        price = lifetime?.product?.price?.formatted,
                        message = if (lifetime == null) R.string.purchase_unavailable else null
                    )
                }
            }
        )
    }

    fun purchase(activity: Activity) {
        val product = lifetime ?: return
        if (!configured || state.value.busy || state.value.pro) return
        mutableState.update {
            it.copy(busy = true, message = null)
        }
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, product).build(),
            onError = { error, cancelled ->
                debug("Purchase ended: ${error.code}; cancelled=$cancelled")
                val message =
                    when {
                        cancelled -> R.string.purchase_cancelled

                        error.code == PurchasesErrorCode.PaymentPendingError ->
                            R.string.purchase_pending

                        error.code == PurchasesErrorCode.ProductAlreadyPurchasedError ->
                            R.string.purchase_already_owned

                        else -> R.string.purchase_failed
                    }
                mutableState.update {
                    it.copy(busy = false, message = message)
                }
                if (error.code == PurchasesErrorCode.ProductAlreadyPurchasedError) refresh()
            },
            onSuccess = { _, info ->
                applyInfo(info)
                mutableState.update {
                    it.copy(
                        busy = false,
                        message = if (it.pro) R.string.pro_active else R.string.purchase_pending
                    )
                }
            }
        )
    }

    fun restore() {
        if (!configured || state.value.busy) return
        mutableState.update {
            it.copy(busy = true, message = null)
        }
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                debug("Restore failed: ${error.code}")
                mutableState.update { state ->
                    state.copy(busy = false, message = R.string.purchase_failed)
                }
            },
            onSuccess = { info ->
                applyInfo(info)
                mutableState.update {
                    it.copy(
                        busy = false,
                        message = if (it.pro) R.string.pro_active else R.string.nothing_to_restore
                    )
                }
            }
        )
    }

    fun acknowledge() {
        mutableState.update {
            it.copy(message = null)
        }
    }
}
