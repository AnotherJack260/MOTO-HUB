// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * The app's motion, in one place, the way the colours are in MotoHubColors. See the Motion section
 * of documentation/DESIGN_SYSTEM.md for when to use which. The short version: motion answers
 * "what just changed?" or "did it hear me?", everything that moves for one change shares [BASE]
 * and one easing, and only a confirmation glyph may overshoot.
 */
object MhMotion {
    /** Exits, icon and label swaps, small colour changes. */
    const val FAST = 120

    /** State swaps, heights, expand and collapse, screen slides. */
    const val BASE = 220

    /** Something changing in place (M3 standard). */
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Something arriving (M3 emphasized decelerate). */
    val Enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Something leaving (M3 emphasized accelerate). */
    val Exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Press and release: critically damped, about 100 ms, and a release mid-dip just turns back. */
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)

    /** Confirmation glyphs only: about 9 % overshoot, settled in about 240 ms. */
    fun <T> pop(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /**
     * A state swap in place. The old content leaves in 80 ms and the new one fades in from 60 ms,
     * so two blocks of text never blend. With [animateHeight] the container's height follows on
     * the same 220 ms clock instead of snapping; without it the size is not animated (or measured
     * for animation) at all, which is what a full-screen swap wants.
     */
    fun fadeThrough(animateHeight: Boolean = false): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tween(160, delayMillis = 60, easing = Standard)),
        initialContentExit = fadeOut(tween(80, easing = Exit)),
        // Not the default SizeTransform: that one is a spring, off the shared clock, and clips.
        sizeTransform = if (animateHeight) SizeTransform(clip = false) { _, _ -> tween(BASE, easing = Standard) } else null
    )

    /**
     * Something folding open under what is above it - a banner arriving, a footnote, banner
     * details: it grows from its top edge, so the content below glides down on the shared clock.
     * Pair with [foldOut] in `AnimatedVisibility`.
     */
    val foldIn: EnterTransition =
        fadeIn(tween(BASE, easing = Standard)) + expandVertically(tween(BASE, easing = Enter), expandFrom = Alignment.Top)

    /** The way back from [foldIn]: the fade is quick, the height takes the same 220 ms. */
    val foldOut: ExitTransition =
        fadeOut(tween(FAST, easing = Exit)) + shrinkVertically(tween(BASE, easing = Exit), shrinkTowards = Alignment.Top)

    /** How far a pressed pill or card dips. Rows, icon buttons and the dock ripple instead. */
    internal const val PRESSED_SCALE = 0.98f
}

/**
 * The dip under a finger, as a scale for `graphicsLayer`. Read it there, in the draw phase: a
 * press then repaints and never recomposes. Disabled controls emit no press, so they don't dip.
 */
@Composable
internal fun pressScale(source: InteractionSource): State<Float> {
    val pressed = source.collectIsPressedAsState()
    return animateFloatAsState(if (pressed.value) MhMotion.PRESSED_SCALE else 1f, MhMotion.press(), label = "press")
}

/**
 * Clickable, ripple and the 98 % press dip, clipped to [shape]: what a kit pill does, for a custom
 * card (the Garage card, the Ride name). Put it first in the chain, before the background, so the
 * card dips as one piece and the ripple stays inside its corners. Rows and icon buttons don't use
 * it - a whole list dipping looks like it is breathing.
 */
// ponytail: composed is fine for a couple of cards; move to Modifier.Node if it spreads to lists.
fun Modifier.mhPressable(
    shape: Shape,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        this.shape = shape
        clip = true
    }.clickable(source, LocalIndication.current, enabled = enabled, role = role, onClick = onClick)
}

/**
 * A confirmation glyph - a check, a success icon - that pops in when [visible] turns true: scale
 * 0.8 to 1 on [MhMotion.pop] while it fades in, and a plain quick fade out. Already visible on
 * first composition means no pop, so opening a screen never sets its checks bouncing. Give it a
 * fixed-size parent, or the space it takes collapses after it has faded out.
 */
@Composable
fun MhPop(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(MhMotion.pop(), initialScale = 0.8f) + fadeIn(tween(MhMotion.FAST)),
        exit = fadeOut(tween(MhMotion.FAST))
    ) {
        content()
    }
}
