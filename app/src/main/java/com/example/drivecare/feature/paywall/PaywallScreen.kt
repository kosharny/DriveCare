package com.example.drivecare.feature.paywall

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivecare.R
import com.example.drivecare.core.ui.*

@Composable
fun PaywallRoute(onPurchase: () -> Unit) {
    val vm: PaywallViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        vm.retry()
    }
    PageColumn(Artwork.PRO) {
        Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineLarge)
        InfoCard(stringResource(R.string.one_time), stringResource(R.string.pro_benefits)) {
            if (state.pro) {
                Text(stringResource(R.string.pro_active), color = MaterialTheme.colorScheme.primary)
            }
            state.price?.let {
                Text(it, style = MaterialTheme.typography.headlineMedium)
            }
            Button(
                onClick = onPurchase,
                enabled = state.price != null && !state.busy && !state.pro
            ) {
                Text(stringResource(R.string.unlock_pro))
            }
            OutlinedButton(onClick = vm::restore, enabled = !state.busy && state.configured) {
                Text(stringResource(R.string.restore_purchases))
            }
            state.message?.let {
                Text(stringResource(it))
            }
            if (state.busy) LinearProgressIndicator()
        }
    }
}
