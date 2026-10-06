// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText

enum class MhNavIcon { BACK, CLOSE }

/**
 * Every screen that is not a tab: a back (or close) button, a large left-aligned title, content
 * that scrolls, and optionally an action pinned to the bottom that rides above the keyboard.
 *
 * Status-bar padding is applied here, before the scroll, so a screen shown straight from
 * MainActivity does not slide its title under the clock; screens inside the hub sit in a parent
 * that already consumed the inset, so it adds nothing there.
 */
@Composable
fun MhScreen(
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    navIcon: MhNavIcon = MhNavIcon.BACK,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            MhIconButton(
                icon = if (navIcon == MhNavIcon.BACK) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
                contentDescription = if (navIcon == MhNavIcon.BACK) motoHubText("Back") else motoHubText("Close"),
                onClick = onBack
            )
            Spacer(Modifier.weight(1f))
            actions()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            if (title != null) {
                Column(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            content()
            Spacer(Modifier.height(if (bottomBar == null) 24.dp else 8.dp))
        }
        if (bottomBar != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.25f to MaterialTheme.colorScheme.background
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = bottomBar
                )
            }
        }
    }
}

/** A tab's own page: large title on top, scrolling content, no back button. */
@Composable
fun MhTabPage(
    title: String,
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Text(
            title,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 4.dp),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        content()
        Spacer(Modifier.height(24.dp))
    }
}
