package com.remodex.mobile.ui.sidebar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R
import com.remodex.mobile.ui.theme.RemodexDropdownMenu

enum class SidebarListTab {
    Projects,
    Chats,
}

/** Shared circular icon control used in sidebar header and turn toolbar. */
@Composable
fun RemodexCircleIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    colors: SidebarColorPalette = rememberSidebarColorPalette(),
    icon: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .remodexFlatControlChrome(CircleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
fun SidebarBrandHeader(
    colors: SidebarColorPalette,
    onMoreMenu: SidebarMoreMenuCallbacks,
    onOpenDesktop: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var moreExpanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.remodex_icon),
                    contentDescription = stringResource(R.string.sidebar_brand_name),
                    modifier = Modifier.size(34.dp),
                )
            }
            Text(
                text = stringResource(R.string.sidebar_brand_name),
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = colors.primaryText,
            )
        }
        SidebarHeaderIconButton(
            icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(20.dp)) },
            contentDescription = stringResource(R.string.sidebar_more_menu_cd),
            colors = colors,
            onClick = { moreExpanded = true },
        )
        Box {
            RemodexDropdownMenu(
                expanded = moreExpanded,
                onDismissRequest = { moreExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sidebar_new_chat)) },
                    onClick = {
                        moreExpanded = false
                        onMoreMenu.onNewChat()
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sidebar_quick_chat)) },
                    onClick = {
                        moreExpanded = false
                        onMoreMenu.onQuickCloudChat()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_cloud),
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_archived_chats)) },
                    onClick = {
                        moreExpanded = false
                        onMoreMenu.onOpenArchivedChats()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.cd_refresh_thread_list)) },
                    onClick = {
                        moreExpanded = false
                        onMoreMenu.onRefreshThreads()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_refresh_cw),
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        SidebarHeaderIconButton(
            icon = { Icon(Icons.Outlined.Computer, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(20.dp)) },
            contentDescription = stringResource(R.string.sidebar_desktop_cd),
            colors = colors,
            onClick = onOpenDesktop,
        )
        SidebarHeaderIconButton(
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(20.dp)) },
            contentDescription = stringResource(R.string.nav_settings),
            colors = colors,
            onClick = onOpenSettings,
        )
    }
}

data class SidebarMoreMenuCallbacks(
    val onNewChat: () -> Unit,
    val onQuickCloudChat: () -> Unit,
    val onOpenArchivedChats: () -> Unit,
    val onRefreshThreads: () -> Unit,
)

@Composable
private fun SidebarHeaderIconButton(
    icon: @Composable () -> Unit,
    contentDescription: String,
    colors: SidebarColorPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RemodexCircleIconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        colors = colors,
        icon = icon,
    )
}

@Composable
fun SidebarListTabRow(
    selected: SidebarListTab,
    onSelected: (SidebarListTab) -> Unit,
    onToggleCollapseAll: () -> Unit,
    colors: SidebarColorPalette,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SidebarTabChip(
                label = stringResource(R.string.sidebar_tab_projects),
                selected = selected == SidebarListTab.Projects,
                colors = colors,
                onClick = { onSelected(SidebarListTab.Projects) },
            )
            SidebarTabChip(
                label = stringResource(R.string.sidebar_tab_chats),
                selected = selected == SidebarListTab.Chats,
                colors = colors,
                onClick = { onSelected(SidebarListTab.Chats) },
            )
        }
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(onClick = onToggleCollapseAll),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_chevrons_up_down),
                contentDescription = stringResource(R.string.sidebar_collapse_all_cd),
                tint = colors.mutedText,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun SidebarTabChip(
    label: String,
    selected: Boolean,
    colors: SidebarColorPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier =
            modifier
                .height(32.dp)
                .clip(shape)
                .background(if (selected) colors.accent else colors.surface)
                .border(1.dp, if (selected) colors.accent else colors.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
            color = if (selected) colors.onAccent else colors.primaryText,
        )
    }
}

@Composable
fun SidebarFloatingActionBar(
    terminalEnabled: Boolean,
    chatEnabled: Boolean,
    chatBusy: Boolean,
    onTerminal: () -> Unit,
    onChat: () -> Unit,
    colors: SidebarColorPalette,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SidebarFloatingPillButton(
            label = stringResource(R.string.sidebar_terminal),
            icon = {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_square_terminal),
                    contentDescription = null,
                    tint = colors.primaryText,
                    modifier = Modifier.size(16.dp),
                )
            },
            background = colors.surface,
            borderColor = colors.border,
            labelColor = colors.primaryText,
            enabled = terminalEnabled,
            onClick = onTerminal,
        )
        SidebarFloatingPillButton(
            label = stringResource(R.string.sidebar_chat),
            icon = {
                if (chatBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.onAccent,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
            background = colors.accent,
            borderColor = colors.accent,
            labelColor = colors.onAccent,
            enabled = chatEnabled && !chatBusy,
            onClick = onChat,
        )
    }
}

@Composable
private fun SidebarFloatingPillButton(
    label: String,
    icon: @Composable () -> Unit,
    background: Color,
    borderColor: Color,
    labelColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier =
            modifier
                .height(44.dp)
                .clip(shape)
                .background(background.copy(alpha = if (enabled) 1f else 0.45f))
                .border(1.dp, borderColor, shape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        icon()
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                ),
            color = labelColor.copy(alpha = if (enabled) 1f else 0.5f),
            maxLines = 1,
        )
    }
}
