package com.example.drivecare.feature.paywall

import androidx.lifecycle.ViewModel
import com.example.drivecare.data.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(private val billing: BillingRepository) : ViewModel() {

    val uiState = billing.state

    fun restore() = billing.restore()

    fun retry() = billing.refresh()
}
