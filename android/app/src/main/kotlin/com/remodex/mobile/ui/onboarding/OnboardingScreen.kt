package com.remodex.mobile.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * First-run onboarding entry (parity with [OnboardingView.swift](CodexMobile/CodexMobile/Views/Onboarding/OnboardingView.swift)).
 */
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingFlow(onFinish = onContinue, modifier = modifier)
}
