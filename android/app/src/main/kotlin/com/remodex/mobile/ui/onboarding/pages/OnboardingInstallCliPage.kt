package com.remodex.mobile.ui.onboarding.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R

@Composable
fun OnboardingInstallCliPage(modifier: Modifier = Modifier) {
    OnboardingSetupStepPage(
        stepNumber = 1,
        iconRes = LucideR.drawable.lucide_ic_square_terminal,
        iconGlowColor = Color(0xFF4A90E2),
        titleRes = R.string.onboarding_install_cli_title,
        bodyRes = R.string.onboarding_install_cli_body,
        commandRes = R.string.onboarding_command_codex_cli,
        modifier = modifier,
    )
}
