package com.remodex.mobile.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R

@Composable
fun OnboardingOpenSourceFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repoUrl = stringResource(R.string.onboarding_github_repo_url)
    FilledTonalButton(
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Icon(
            painter = painterResource(LucideR.drawable.lucide_ic_github),
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.onboarding_open_source),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
