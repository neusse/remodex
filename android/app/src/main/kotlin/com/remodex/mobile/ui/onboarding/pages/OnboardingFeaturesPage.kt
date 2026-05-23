package com.remodex.mobile.ui.onboarding.pages

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R
import com.remodex.mobile.ui.onboarding.OnboardingIconTile

private data class OnboardingFeatureRow(
    @param:DrawableRes val iconRes: Int,
    val glowColor: Color,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)

@Composable
fun OnboardingFeaturesPage(modifier: Modifier = Modifier) {
    val features =
        listOf(
            OnboardingFeatureRow(
                iconRes = LucideR.drawable.lucide_ic_zap,
                glowColor = Color(0xFFFFD60A),
                titleRes = R.string.onboarding_feature_fast_title,
                bodyRes = R.string.onboarding_feature_fast_body,
            ),
            OnboardingFeatureRow(
                iconRes = LucideR.drawable.lucide_ic_git_branch,
                glowColor = Color(0xFF35C76A),
                titleRes = R.string.onboarding_feature_git_title,
                bodyRes = R.string.onboarding_feature_git_body,
            ),
            OnboardingFeatureRow(
                iconRes = LucideR.drawable.lucide_ic_shield,
                glowColor = Color(0xFF64B5F6),
                titleRes = R.string.onboarding_feature_encrypted_title,
                bodyRes = R.string.onboarding_feature_encrypted_body,
            ),
            OnboardingFeatureRow(
                iconRes = LucideR.drawable.lucide_ic_audio_lines,
                glowColor = Color(0xFFB388FF),
                titleRes = R.string.onboarding_feature_voice_title,
                bodyRes = R.string.onboarding_feature_voice_body,
            ),
            OnboardingFeatureRow(
                iconRes = LucideR.drawable.lucide_ic_blocks,
                glowColor = Color(0xFFFF9B45),
                titleRes = R.string.onboarding_feature_subagents_title,
                bodyRes = R.string.onboarding_feature_subagents_body,
            ),
        )
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_features_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_features_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            features.forEach { feature ->
                OnboardingFeatureItem(feature = feature)
            }
        }
    }
}

@Composable
private fun OnboardingFeatureItem(
    feature: OnboardingFeatureRow,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OnboardingIconTile(iconRes = feature.iconRes, glowColor = feature.glowColor)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(feature.titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = stringResource(feature.bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
