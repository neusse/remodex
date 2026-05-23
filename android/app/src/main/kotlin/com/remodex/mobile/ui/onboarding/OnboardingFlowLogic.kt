package com.remodex.mobile.ui.onboarding

import com.remodex.mobile.R

/** Page index for the five-step first-run onboarding wizard. */
const val ONBOARDING_PAGE_COUNT = 5

enum class OnboardingPrimaryAction {
    Advance,
    Finish,
}

object OnboardingFlowLogic {
    fun primaryActionForPage(pageIndex: Int): OnboardingPrimaryAction =
        if (pageIndex == ONBOARDING_PAGE_COUNT - 1) {
            OnboardingPrimaryAction.Finish
        } else {
            OnboardingPrimaryAction.Advance
        }

    fun ctaLabelRes(pageIndex: Int): Int =
        when (pageIndex) {
            0 -> R.string.onboarding_cta_get_started
            1 -> R.string.onboarding_cta_set_up
            ONBOARDING_PAGE_COUNT - 1 -> R.string.onboarding_cta_scan_qr
            else -> R.string.onboarding_cta_continue
        }

    fun showQrIconOnCta(pageIndex: Int): Boolean = pageIndex == ONBOARDING_PAGE_COUNT - 1
}
