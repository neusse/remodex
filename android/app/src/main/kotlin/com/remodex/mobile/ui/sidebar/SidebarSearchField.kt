package com.remodex.mobile.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R

@Composable
fun SidebarSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SidebarSearchField(
        query = query,
        onQueryChange = onQueryChange,
        placeholderText = stringResource(R.string.sidebar_search_hint),
        modifier = modifier,
    )
}

@Composable
fun SidebarSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String? = null,
    modifier: Modifier = Modifier,
) {
    val hint = placeholderText ?: stringResource(R.string.sidebar_search_hint)
    val colors = rememberSidebarColorPalette()
    val fieldShape = RoundedCornerShape(22.dp)
    val textStyle =
        MaterialTheme.typography.bodyLarge.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = colors.primaryText,
            lineHeight = 18.sp,
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(fieldShape)
                .background(colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(LucideR.drawable.lucide_ic_search),
            contentDescription = null,
            modifier =
                Modifier
                    .padding(start = 14.dp)
                    .size(18.dp),
            tint = colors.mutedText,
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.primaryText),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = hint,
                            style = textStyle.copy(color = colors.mutedText),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_x),
                    contentDescription = stringResource(R.string.cd_clear_search),
                    modifier = Modifier.size(14.dp),
                    tint = colors.mutedText,
                )
            }
        } else {
            Box(modifier = Modifier.size(8.dp))
        }
    }
}
