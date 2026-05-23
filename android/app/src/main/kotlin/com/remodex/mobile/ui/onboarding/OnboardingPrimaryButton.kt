package com.remodex.mobile.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR

@Composable
fun OnboardingPrimaryButton(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    showQrIcon: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        if (showQrIcon) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_scan_qr_code),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = if (showQrIcon) Modifier.padding(start = 8.dp) else Modifier,
        )
    }
}
