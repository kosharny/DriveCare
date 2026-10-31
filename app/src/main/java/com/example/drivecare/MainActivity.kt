package com.example.drivecare

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.drivecare.app.DriveCareApp
import com.example.drivecare.data.billing.BillingRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var billing: BillingRepository
    private var systemSplashGone by mutableStateOf(Build.VERSION.SDK_INT < 31)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31) {
            splashScreen.setOnExitAnimationListener { splash ->
                splash.remove()
                systemSplashGone = true
            }
        }
        enableEdgeToEdge()
        setContent {
            DriveCareApp(
                onPurchase = {
                    billing.purchase(this)
                },
                reminderVehicleId = intent.getStringExtra("vehicleId"),
                reminderPlanId = intent.getStringExtra("planId"),
                systemSplashGone = systemSplashGone
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::billing.isInitialized) billing.refresh()
    }
}
