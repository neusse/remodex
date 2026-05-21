package com.remodex.mobile.ui.onboarding.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R

@Composable
fun OnboardingPairingIntroPage(modifier: Modifier = Modifier) {
    OnboardingSetupStepPage(
        stepNumber = 3,
        iconRes = LucideR.drawable.lucide_ic_scan_qr_code,
        iconGlowColor = Color(0xFF4A90E2),
        titleRes = R.string.onboarding_pairing_title,
        bodyRes = R.string.onboarding_pairing_body,
        commandRes = R.string.onboarding_command_pairing,
        modifier = modifier,
    )
}
