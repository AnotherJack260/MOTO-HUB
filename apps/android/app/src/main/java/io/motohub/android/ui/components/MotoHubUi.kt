// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import io.motohub.android.i18n.motoHubText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.motohub.android.ui.theme.MotoHubColors

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

@Composable
fun MotoHubBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.background),
            content = content
        )
    }
}

/** A hairline under the status bar: grey, lime while connecting, lime when connected. */
@Composable
fun ConnectionRail(state: ConnectionState, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                when (state) {
                    ConnectionState.DISCONNECTED -> Color.Transparent
                    ConnectionState.CONNECTING -> MotoHubColors.Lime.copy(alpha = 0.4f)
                    ConnectionState.CONNECTED -> MotoHubColors.Lime
                }
            )
    )
}

/** Kept for the screens that still draw it; the hub no longer has a top bar of its own. */
@Composable
fun HubAppBar(
    motorcycleName: String?,
    isConnected: Boolean,
    onMotorcycleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        MhStatusChip(
            text = motorcycleName ?: motoHubText("No motorcycle"),
            tone = if (isConnected) MhTone.LIVE else MhTone.NEUTRAL,
            // The chip is ~26 dp tall; the minimum size gives it a glove-sized 48 dp target.
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onMotorcycleTap)
        )
    }
}

enum class HubTab { RIDE, NAV, TRIPS, GARAGE, SETTINGS }

/**
 * The dock: full width on the Surface colour, which runs on under the gesture bar - the inset is
 * padded inside, after the background, so callers add nothing. Filled icon and white label for
 * the selected tab, outlined and grey for the rest; no indicator pill, no top border. [rideLive]
 * puts a small lime dot on Ride while something is streaming, so the rider can see it from any tab.
 */
@Composable
fun HubBottomNavigation(
    selected: HubTab,
    onSelect: (HubTab) -> Unit,
    modifier: Modifier = Modifier,
    rideLive: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MotoHubColors.Surface)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavItem(motoHubText("Ride"), Icons.Rounded.TwoWheeler, Icons.Outlined.TwoWheeler, selected == HubTab.RIDE, Modifier.weight(1f), dot = rideLive) { onSelect(HubTab.RIDE) }
        // Nav and Trips are PRO-only features. CORE ships without them (see build.gradle.kts flavors).
        if (io.motohub.android.BuildConfig.IS_PRO) {
            NavItem(motoHubText("Nav"), Icons.Rounded.Navigation, Icons.Outlined.Navigation, selected == HubTab.NAV, Modifier.weight(1f)) { onSelect(HubTab.NAV) }
            NavItem(motoHubText("Trips"), Icons.Rounded.Route, Icons.Outlined.Route, selected == HubTab.TRIPS, Modifier.weight(1f)) { onSelect(HubTab.TRIPS) }
        }
        NavItem(motoHubText("Garage"), Icons.Rounded.Garage, Icons.Outlined.Garage, selected == HubTab.GARAGE, Modifier.weight(1f)) { onSelect(HubTab.GARAGE) }
        NavItem(motoHubText("Settings"), Icons.Rounded.Settings, Icons.Outlined.Settings, selected == HubTab.SETTINGS, Modifier.weight(1f)) { onSelect(HubTab.SETTINGS) }
    }
}

@Composable
private fun NavItem(
    label: String,
    activeIcon: ImageVector,
    idleIcon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    dot: Boolean = false,
    onClick: () -> Unit
) {
    // Selected is white, not lime: lime stays for the one action on the screen. Idle is the
    // secondary text colour - the tertiary grey was 3.9:1, too faint for a 12 sp label.
    val color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(active, role = Role.Tab, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        Box {
            Icon(if (active) activeIcon else idleIcon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            if (dot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-1).dp)
                        .size(6.dp)
                        .background(MotoHubColors.Lime, CircleShape)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Section label above a group. Was a monospace eyebrow; now the design system's section header. */
@Deprecated("Use MhSectionHeader; textAlign is ignored.", ReplaceWith("MhSectionHeader(text, modifier)"))
@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    MhSectionHeader(text, modifier)
}

@Composable
fun LivePill(text: String, modifier: Modifier = Modifier) {
    MhStatusChip(motoHubText(text), MhTone.LIVE, modifier)
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    MhStatusChip(text, if (color == MaterialTheme.colorScheme.error) MhTone.ERROR else MhTone.WARNING, modifier)
}

/** Top row of the full-screen pages that predate [MhScreen]: just the trailing action now. */
@Composable
fun MotoHubHeader(
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        trailing?.invoke()
    }
}

/** A drill-down settings-style screen: back button, large title, scrolling content. */
@Deprecated("Use MhScreen; backLabel is no longer shown.", ReplaceWith("MhScreen(title = title, onBack = onBack) { content() }"))
@Composable
fun MotoHubDetailScreen(
    title: String,
    onBack: () -> Unit,
    backLabel: String = "‹ Back",
    content: @Composable () -> Unit
) {
    MhScreen(title = title, onBack = onBack) { content() }
}

/** A card wrapping a group of rows. */
@Composable
fun MotoHubCardGroup(content: @Composable () -> Unit) {
    MhListGroup { content() }
}

/** A tappable row with a title, description, optional current value, and a chevron. */
@Deprecated(
    "Use MhListRow inside an MhListGroup.",
    ReplaceWith("MhListRow(title = title, subtitle = description, value = value, onClick = onClick)")
)
@Composable
fun MotoHubActionRow(
    title: String,
    description: String,
    value: String? = null,
    onClick: () -> Unit
) {
    MhListRow(title = title, subtitle = description.takeIf { it.isNotBlank() }, value = value, onClick = onClick)
}

/** A single on/off setting. */
@Deprecated(
    "Use MhSwitchRow inside an MhListGroup.",
    ReplaceWith(
        "MhSwitchRow(title = title, checked = checked, onCheckedChange = onCheckedChange, subtitle = description, enabled = enabled)"
    )
)
@Composable
fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    MhSwitchRow(
        title = title,
        subtitle = description.takeIf { it.isNotBlank() },
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled
    )
}

/** One option among several exclusive choices. Group consecutive ones in an [MhListGroup]. */
@Deprecated(
    "Use MhChoiceRow; put the choices together in one MhListGroup.",
    ReplaceWith("MhChoiceRow(title = title, selected = selected, onClick = onClick, subtitle = description)")
)
@Composable
fun MotoHubRadioRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)) {
        MhChoiceRow(
            title = title,
            subtitle = description.takeIf { it.isNotBlank() },
            selected = selected,
            onClick = onClick
        )
    }
}
