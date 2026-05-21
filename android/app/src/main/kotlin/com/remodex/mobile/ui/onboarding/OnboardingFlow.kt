package com.remodex.mobile.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remodex.mobile.ui.onboarding.pages.OnboardingFeaturesPage
import com.remodex.mobile.ui.onboarding.pages.OnboardingInstallBridgePage
import com.remodex.mobile.ui.onboarding.pages.OnboardingInstallCliPage
import com.remodex.mobile.ui.onboarding.pages.OnboardingPairingIntroPage
import com.remodex.mobile.ui.onboarding.pages.OnboardingWelcomePage
import kotlinx.coroutines.launch

/**
 * Five-step first-run onboarding (welcome → features → setup steps → pairing intro).
 * Completing page 5 invokes [onFinish], which transitions to [QrScannerScreen] via [RootViewModel].
 */
@Composable
fun OnboardingFlow(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    BackHandler(enabled = currentPage > 0) {
        scope.launch {
            pagerState.animateScrollToPage(currentPage - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage(modifier = Modifier.fillMaxSize())
                    1 -> OnboardingFeaturesPage(modifier = Modifier.fillMaxSize())
                    2 -> OnboardingInstallCliPage(modifier = Modifier.fillMaxSize())
                    3 -> OnboardingInstallBridgePage(modifier = Modifier.fillMaxSize())
                    4 -> OnboardingPairingIntroPage(modifier = Modifier.fillMaxSize())
                }
            }
            OnboardingPageIndicator(
                currentPage = currentPage,
                pageCount = ONBOARDING_PAGE_COUNT,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            OnboardingPrimaryButton(
                labelRes = OnboardingFlowLogic.ctaLabelRes(currentPage),
                showQrIcon = OnboardingFlowLogic.showQrIconOnCta(currentPage),
                onClick = {
                    when (OnboardingFlowLogic.primaryActionForPage(currentPage)) {
                        OnboardingPrimaryAction.Advance -> {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        }
                        OnboardingPrimaryAction.Finish -> onFinish()
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OnboardingOpenSourceFooter()
        }
    }
}
