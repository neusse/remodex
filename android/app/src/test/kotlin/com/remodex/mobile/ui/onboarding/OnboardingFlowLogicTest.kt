package com.remodex.mobile.ui.onboarding

import com.remodex.mobile.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowLogicTest {
    @Test
    fun `primary action advances until last page`() {
        for (page in 0 until ONBOARDING_PAGE_COUNT - 1) {
            assertEquals(OnboardingPrimaryAction.Advance, OnboardingFlowLogic.primaryActionForPage(page))
        }
    }

    @Test
    fun `primary action finishes on last page`() {
        assertEquals(
            OnboardingPrimaryAction.Finish,
            OnboardingFlowLogic.primaryActionForPage(ONBOARDING_PAGE_COUNT - 1),
        )
    }

    @Test
    fun `cta labels match page index`() {
        assertEquals(R.string.onboarding_cta_get_started, OnboardingFlowLogic.ctaLabelRes(0))
        assertEquals(R.string.onboarding_cta_set_up, OnboardingFlowLogic.ctaLabelRes(1))
        assertEquals(R.string.onboarding_cta_continue, OnboardingFlowLogic.ctaLabelRes(2))
        assertEquals(R.string.onboarding_cta_continue, OnboardingFlowLogic.ctaLabelRes(3))
        assertEquals(R.string.onboarding_cta_scan_qr, OnboardingFlowLogic.ctaLabelRes(4))
    }

    @Test
    fun `qr icon only on final page cta`() {
        assertFalse(OnboardingFlowLogic.showQrIconOnCta(0))
        assertFalse(OnboardingFlowLogic.showQrIconOnCta(3))
        assertTrue(OnboardingFlowLogic.showQrIconOnCta(4))
    }
}
