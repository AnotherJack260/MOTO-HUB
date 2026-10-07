// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.theme.MotoHubColors

enum class MhNavIcon { BACK, CLOSE }

/**
 * The 56 dp bar at the top of every screen that is not a tab. Back (or close, for full-screen
 * flows and modal pages) is always top-left, at the same spot everywhere - a plain 24 dp glyph in
 * a 48 dp target, the glyph 16 dp from the edge - never a text button and never on the right.
 * [actions] go on the right as [MhTopBarAction] circles, the last one 16 dp from the edge.
 *
 * [title] is the compact, centred one-liner: it repeats the large title, so cutting it with an
 * ellipsis is fine here. [MhScreen] and [MhTabPage] fade it in as their large title scrolls away.
 *
 * Usable on its own where [MhScreen] cannot be - the camera scanner, the Android Auto preview, a
 * screen whose body is a LazyColumn. It pads the status bar itself (nothing, where a parent
 * already did) and is transparent. It only draws: pair it with a `BackHandler` that does what
 * [onBack] does. A null [onBack] leaves the start empty, for a tab page.
 */
@Composable
fun MhTopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    navIcon: MhNavIcon = MhNavIcon.BACK,
    title: String? = null,
    titleAlpha: Float = 1f,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopBar(onBack, modifier, navIcon, title, titleAlpha, actions, overHeader = false)
}

// [overHeader]: the bar floats over an MhScreen header (a photo), so back sits in a Fill circle
// like the actions do - a bare glyph can vanish into a bright picture.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier,
    navIcon: MhNavIcon,
    title: String?,
    titleAlpha: Float,
    actions: @Composable RowScope.() -> Unit,
    overHeader: Boolean
) {
    CenterAlignedTopAppBar(
        title = {
            if (title != null) {
                Text(
                    title,
                    // While it is invisible it must not be read either: the large title is.
                    modifier = Modifier
                        .graphicsLayer { alpha = titleAlpha }
                        .then(if (titleAlpha == 0f) Modifier.clearAndSetSemantics {} else Modifier),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier,
        // The bar insets its slots 4 dp; a 48 dp button centres its 24 dp glyph, so the glyph
        // lands 16 dp from the edge.
        navigationIcon = {
            if (onBack != null) {
                val icon = if (navIcon == MhNavIcon.BACK) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close
                val label = if (navIcon == MhNavIcon.BACK) motoHubText("Back") else motoHubText("Close")
                if (overHeader) MhTopBarAction(icon, label, onBack) else MhIconButton(icon, label, onBack)
            }
        },
        actions = {
            actions()
            Spacer(Modifier.width(8.dp))
        },
        expandedHeight = 56.dp,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * A trailing action in [MhTopBar]: an icon in a 40 dp Fill circle, with a 48 dp target. [active]
 * is for a toggle that is on (a torch, a filter): the glyph turns lime, the way a switch does.
 */
@Composable
fun MhTopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MotoHubColors.Fill,
            contentColor = if (active) MotoHubColors.Lime else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

/**
 * Every screen that is not a tab: [MhTopBar], a large left-aligned title, content that scrolls,
 * and optionally an action pinned to the bottom that rides above the keyboard. As the large title
 * scrolls away it fades out, and a compact one fades into the bar.
 *
 * Without a [bottomBar] the scroll ends clear of the navigation bar and the keyboard, so the last
 * row can always be scrolled into reach. [scrollable] = false is for a body that scrolls itself
 * (a LazyColumn): the content then gets the remaining height instead, and the title stays put.
 *
 * [header] replaces the large title with something full-bleed - a photo - that starts at the top
 * of the window, under a bar that floats over it with back in a circle. It fades and collapses
 * like the title, the bar turning solid as the compact [title] fades in. Draw the screen's name in
 * it (with `semantics { heading() }`); [subtitle] is not shown.
 *
 * A [bottomBar] reports its height to the snackbar ([reserveSnackbarClearance]), so a message
 * never lands on the pinned action.
 *
 * System back calls [onBack] like the back arrow does, so a new screen cannot forget it. A
 * `BackHandler` registered further in - a step inside the screen - still wins.
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
    scrollable: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onBack)
    val scroll = rememberScrollState()
    val collapse = rememberTitleCollapse(scroll)
    val background = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(background)) {
        Column(Modifier.fillMaxSize()) {
            if (header == null) CollapsingTopBar(onBack, navIcon, title, collapse, actions)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier)
                    .then(if (bottomBar == null) Modifier.navigationBarsPadding().imePadding() else Modifier),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // The gutter is on each block rather than on this column, so a header can be full-bleed.
                val gutter = Modifier.padding(horizontal = 16.dp)
                if (header != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { collapse.titleBottom = it.height }
                            .graphicsLayer { alpha = collapse.largeAlpha() }
                    ) { header() }
                } else if (title != null) {
                    LargeTitle(title, subtitle, collapse, gutter)
                }
                if (scrollable) {
                    Column(gutter, verticalArrangement = Arrangement.spacedBy(spacing)) {
                        content()
                        Spacer(Modifier.height(if (bottomBar == null) 24.dp else 8.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().then(gutter),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        content = content
                    )
                }
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
                            // Inside the insets: the snackbar host pads those itself.
                            .reserveSnackbarClearance()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = bottomBar
                    )
                }
            }
        }
        if (header != null) {
            // Over the header: a dark wash so the status bar and back stay legible on a bright photo,
            // then the background fading in with the compact title, so nothing scrolls under it.
            Box(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { collapse.overlap = it.height }
                    .drawBehind {
                        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)))
                        drawRect(background, alpha = collapse.compactAlpha)
                    }
            ) {
                CollapsingTopBar(onBack, navIcon, title, collapse, actions, overHeader = true)
            }
        }
    }
}

/**
 * The collapsing header that [MhScreen] and [MhTabPage] share. Progress runs from 0, the large
 * title (or header) in full view, to 1, scrolled entirely under the bar. The large title fades
 * out over all of it, so no half-cut glyphs hang under the bar; the compact title fades in over
 * the last 24 dp, so the two hand over at the bar. Both follow the finger rather than a timed
 * animation, and both are read where they draw: scrolling repaints, it does not recompose the
 * screen.
 */
@Stable
private class TitleCollapse(private val scroll: ScrollState, private val fadePx: Float) {
    /** Where the large title ends, in scroll pixels; MAX until it has been laid out (or never is). */
    var titleBottom by mutableIntStateOf(Int.MAX_VALUE)

    /** How much of it a floating bar covers from the start (a header's); 0 for a title. */
    var overlap by mutableIntStateOf(0)

    private fun span() = (titleBottom - overlap).coerceAtLeast(1).toFloat()

    fun largeAlpha() = 1f - (scroll.value / span()).coerceIn(0f, 1f)

    // Derived and clamped, so it changes - and recomposes the bar - only inside the last 24 dp.
    val compactAlpha by derivedStateOf { ((scroll.value - span() + fadePx) / fadePx).coerceIn(0f, 1f) }
}

@Composable
private fun rememberTitleCollapse(scroll: ScrollState): TitleCollapse {
    val fadePx = with(LocalDensity.current) { 24.dp.toPx() }
    return remember(scroll, fadePx) { TitleCollapse(scroll, fadePx) }
}

// Its own scope, so the compact title's fade recomposes the bar and not the whole screen.
@Composable
private fun CollapsingTopBar(
    onBack: (() -> Unit)?,
    navIcon: MhNavIcon,
    title: String?,
    collapse: TitleCollapse,
    actions: @Composable RowScope.() -> Unit,
    overHeader: Boolean = false
) {
    TopBar(onBack, Modifier, navIcon, title, collapse.compactAlpha, actions, overHeader)
}

/** The large title both [MhScreen] and [MhTabPage] use, so the two line up exactly. */
@Composable
private fun LargeTitle(title: String, subtitle: String?, collapse: TitleCollapse, modifier: Modifier = Modifier) {
    val top = 4.dp
    val topPx = with(LocalDensity.current) { top.roundToPx() }
    // On the card edge, no inset: the title, headers and footnotes all start where the cards do.
    Column(
        modifier = modifier.padding(top = top, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            title,
            modifier = Modifier
                .semantics { heading() }
                .onSizeChanged { collapse.titleBottom = topPx + it.height }
                .graphicsLayer { alpha = collapse.largeAlpha() },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A tab's own page: the same bar slot and large title as [MhScreen], so a title does not jump
 * when the rider drills in, but no back button. [actions] sit on the right of the bar, and the
 * title collapses into it the same way.
 */
@Composable
fun MhTabPage(
    title: String,
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()
    val collapse = rememberTitleCollapse(scroll)
    Column(modifier.fillMaxSize()) {
        CollapsingTopBar(onBack = null, navIcon = MhNavIcon.BACK, title = title, collapse = collapse, actions = actions)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            LargeTitle(title, subtitle = null, collapse = collapse)
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}
