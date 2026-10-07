// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.theme.MotoHubColors

// The building blocks every screen is drawn from. See documentation/DESIGN_SYSTEM.md for when to
// use which; the short version is one lime button per screen, everything else a quieter button
// or a row in a group.

// Both buttons fill the width unless told otherwise, and that lives in [fillWidth] rather than in
// the default modifier: a caller passing Modifier.padding(...) would otherwise silently lose it.
// While loading they stay in their enabled colours - the button is busy, not unavailable, and a
// grey container would swallow the spinner. Both dip under the finger and ignore a second tap
// inside 500 ms ([Pill]).

/** The one thing to do on this screen. Lime, full width, glove-sized. */
@Composable
fun MhPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    fillWidth: Boolean = true
) {
    Pill(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillWidthIf(fillWidth).heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = if (loading) MaterialTheme.colorScheme.primary else MotoHubColors.SurfaceHighest,
            disabledContentColor = if (loading) MaterialTheme.colorScheme.onPrimary else MotoHubColors.TextTertiary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        ButtonContent(text, icon, loading)
    }
}

/** How tall an [MhSecondaryButton] is. */
enum class MhButtonSize {
    /** 56 dp, like the primary: a screen's or a sheet's own actions. */
    LARGE,

    /**
     * 40 dp drawn in a 48 dp target, 15 sp label: an action that lives inside something else - a
     * banner, the end of a row - and must not weigh as much as the screen's lime button.
     */
    COMPACT
}

/**
 * Every other action that deserves a button: grey pill, red text when it undoes something.
 * [MhButtonSize.COMPACT] wraps its label unless told otherwise, so it drops into a banner or a
 * row's `trailing` slot as is.
 */
@Composable
fun MhSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
    icon: ImageVector? = null,
    size: MhButtonSize = MhButtonSize.LARGE,
    fillWidth: Boolean = size == MhButtonSize.LARGE
) {
    SecondaryPill(text, onClick, modifier, enabled, destructive, loading, icon, size, fillWidth, MotoHubColors.Fill)
}

@Composable
private fun SecondaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
    icon: ImageVector? = null,
    size: MhButtonSize = MhButtonSize.LARGE,
    fillWidth: Boolean = true,
    container: Color = MotoHubColors.Fill
) {
    val content = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val compact = size == MhButtonSize.COMPACT
    Pill(
        onClick = onClick,
        enabled = enabled && !loading,
        // 56 like the primary: stacked in a sheet the pair reads as one set, and gloves need it.
        // Compact only draws 40: Material's button already reserves a 48 dp target around it.
        modifier = modifier.fillWidthIf(fillWidth).heightIn(min = if (compact) 40.dp else 56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = if (loading) content else MotoHubColors.TextTertiary
        ),
        contentPadding = if (compact) {
            PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        } else {
            PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        }
    ) {
        val label = MaterialTheme.typography.labelLarge
        ButtonContent(text, icon, loading, if (compact) label.copy(fontSize = 15.sp) else label)
    }
}

private fun Modifier.fillWidthIf(fill: Boolean) = if (fill) fillMaxWidth() else this

/**
 * Every kit pill: Material's button, plus the 98 % dip under the finger and the glove guard. The
 * dip is the tap's acknowledgement on the frame it lands; the guard means a glove that lands
 * twice runs Stop, Save or Connect once. Disabled and loading pills emit no press, so they don't
 * dip.
 */
@Composable
private fun Pill(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    colors: ButtonColors,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Button(
        onClick = rememberGuardedClick(onClick),
        enabled = enabled,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        shape = CircleShape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content
    )
}

@Composable
private fun rememberGuardedClick(onClick: () -> Unit): () -> Unit {
    val current by rememberUpdatedState(onClick)
    val guard = remember { ClickGuard() }
    return remember { { if (guard.pass(SystemClock.uptimeMillis())) current() } }
}

/**
 * Lets one click through per [windowMs], counted from the last one it let through. A window
 * rather than a "busy" flag: nothing has to clear it, so a failure can never leave a pill dead.
 */
internal class ClickGuard(private val windowMs: Long = 500) {
    private var last = -windowMs

    fun pass(nowMs: Long): Boolean {
        if (nowMs - last < windowMs) return false
        last = nowMs
        return true
    }
}

/**
 * How a sheet's or dialog's main action is drawn. LIME is the one action of that layer; NEUTRAL
 * is for answers the app must not nudge (consent, trust); DESTRUCTIVE is red text on a red-tinted
 * pill and gives the confirm haptic itself, so no caller has to remember it.
 */
enum class MhActionStyle { LIME, NEUTRAL, DESTRUCTIVE }

/** The stacked pill MhSheet and MhDialog draw for [style]. */
@Composable
internal fun MhActionButton(text: String, style: MhActionStyle, onClick: () -> Unit) {
    val view = LocalView.current
    when (style) {
        MhActionStyle.LIME -> MhPrimaryButton(text, onClick)
        MhActionStyle.NEUTRAL -> MhSecondaryButton(text, onClick)
        // Tinted, not Fill: on a grey pill "Remove" weighed exactly what "Cancel" under it did,
        // and the white Cancel looked like the answer. Only the confirm gets it; a red pill on a
        // screen ("Stop streaming") stays on Fill.
        MhActionStyle.DESTRUCTIVE -> SecondaryPill(
            text,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
            },
            destructive = true,
            container = DestructiveFill
        )
    }
}

// Error at 24%, translucent for the reason Fill is: the opaque errorContainer is darker than the
// sheet and read as a hole, not a button. Over a sheet it keeps ErrorText at 4.9:1.
private val DestructiveFill = MotoHubColors.Error.copy(alpha = 0.24f)

/** "Cancel", "Skip", "Not now": text with a full-size touch target. */
@Composable
fun MhTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(contentColor = color, disabledContentColor = MotoHubColors.TextTertiary)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    loading: Boolean,
    style: TextStyle = MaterialTheme.typography.labelLarge
) {
    if (loading) {
        CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
    } else if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = style, textAlign = TextAlign.Center)
}

/** A 48 dp icon-only button - back, close, info. */
@Composable
fun MhIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/** An icon in a neutral circle: how every list row and hero names what it is about. */
@Composable
fun MhIconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    container: Color = MotoHubColors.Fill
) {
    Box(
        modifier = modifier.size(size).background(container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * Sentence-case label above a group, on the card's edge. White 17 sp SemiBold: a grey 15 sp
 * header sat too close to the 16 sp rows under it, and the ladder (title, header, row) went flat.
 * SemiBold against the rows' Medium keeps the two apart.
 */
@Composable
fun MhSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, lineHeight = 22.sp),
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** A rounded card that holds rows. No dividers: the padding of each row separates them. */
@Composable
fun MhListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        content = content
    )
}

/**
 * The row every list is made of. The subtitle is never cut: a longer translation makes the row
 * taller. [trailing] replaces the value/chevron pair when the row ends in a control.
 *
 * The chevron means "opens a screen", so a row on an [MhSheet] has none by default: there a row
 * acts (picks, imports, removes) and never navigates. See [LocalMhInSheet].
 *
 * [subtitleColor] is for a parent row whose screen has a broken prerequisite: the subtitle says
 * what is off ("Accessibility service is off") in `MotoHubColors.Warning`, so the rider sees it
 * without drilling in.
 */
@Composable
fun MhListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    iconContainer: Color = MotoHubColors.Fill,
    value: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    showChevron: Boolean = !LocalMhInSheet.current,
    role: Role = Role.Button,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) 56.dp else 64.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = role, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.45f),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) MhIconCircle(icon, tint = iconTint, container = iconContainer)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            if (value != null) {
                Text(
                    value,
                    modifier = Modifier.widthIn(max = 140.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
            if (onClick != null && showChevron) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MotoHubColors.TextTertiary
                )
            }
        }
    }
}

/**
 * An on/off setting. The whole row toggles, not just the switch, and the row is the one control
 * TalkBack sees: it announces the title with its on/off state, and the switch inside only draws.
 */
@Composable
fun MhSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    MhListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        enabled = enabled,
        trailing = { MhSwitch(checked, null, enabled) }
    )
}

/**
 * Off is a full-size white thumb on Fill, a choice as live as on: Material's off state, a 16 dp
 * grey dot, read as "unavailable". Only a disabled switch greys its thumb, inside a row that fades.
 */
@Composable
fun MhSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        // Any non-null slot keeps the 24 dp thumb in both states; Material shrinks an empty one.
        thumbContent = {},
        colors = SwitchDefaults.colors(
            checkedThumbColor = MotoHubColors.Background,
            checkedTrackColor = MotoHubColors.Lime,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = MotoHubColors.TextPrimary,
            uncheckedTrackColor = MotoHubColors.Fill,
            uncheckedBorderColor = Color.Transparent,
            disabledUncheckedThumbColor = MotoHubColors.TextTertiary,
            disabledUncheckedTrackColor = MotoHubColors.Fill,
            disabledUncheckedBorderColor = Color.Transparent
        )
    )
}

/**
 * One of several exclusive choices in a group: a lime check marks the chosen one. Selectable
 * rather than clickable, so TalkBack says which one is chosen and not just that it can be tapped.
 */
@Composable
fun MhChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    MhListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = modifier.selectable(selected, onClick = onClick, role = Role.RadioButton),
        trailing = {
            // The pop is the only sign an in-place list (Video quality, Language) took the choice.
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                MhPop(selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )
}

enum class MhTone { LIVE, PROGRESS, NEUTRAL, WARNING, ERROR }

private fun MhTone.colors(): Pair<Color, Color> = when (this) {
    MhTone.LIVE -> MotoHubColors.Lime to MotoHubColors.LimeContainer
    MhTone.PROGRESS -> MotoHubColors.Lime to MotoHubColors.Fill
    MhTone.NEUTRAL -> MotoHubColors.TextSecondaryOnFill to MotoHubColors.Fill
    MhTone.WARNING -> MotoHubColors.Warning to MotoHubColors.WarningContainer
    MhTone.ERROR -> MotoHubColors.Error to MotoHubColors.ErrorContainer
}

/**
 * "Live", "Connecting", "Offline": a dot and a word in a pill. Only PROGRESS pulses - something
 * that blinks for a whole ride stops meaning anything. A change of state sweeps the colours and
 * crossfades the word while the pill's width follows, all on the shared clock, so the eye is
 * drawn there once and then nothing moves.
 *
 * The pill and the dot are painted in the draw phase, so the pulse and the sweep repaint without
 * recomposing; only the word's colour recomposes the chip, for 220 ms per change.
 */
@Composable
fun MhStatusChip(text: String, tone: MhTone, modifier: Modifier = Modifier) {
    val (fgTarget, bgTarget) = tone.colors()
    val sweep = tween<Color>(MhMotion.BASE, easing = MhMotion.Standard)
    val fg by animateColorAsState(fgTarget, sweep, label = "chip-fg")
    val bg by animateColorAsState(bgTarget, sweep, label = "chip-bg")
    // 0.25 to 1, not 1 to 0.25: with animations off an infinite transition jumps to its target
    // and stays there, and the target has to be the resting look - a fully lit dot, not a dim one.
    val pulse = if (tone == MhTone.PROGRESS) {
        rememberInfiniteTransition(label = "chip").animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "dot"
        )
    } else null
    Row(
        modifier = modifier
            .drawBehind { drawRoundRect(bg, cornerRadius = CornerRadius(size.height / 2)) }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).drawBehind { drawCircle(fg, alpha = pulse?.value ?: 1f) })
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                fadeIn(tween(MhMotion.FAST)) togetherWith fadeOut(tween(MhMotion.FAST)) using
                    SizeTransform(clip = false) { _, _ -> tween(MhMotion.BASE, easing = MhMotion.Standard) }
            },
            label = "chip-text"
        ) { word ->
            Text(word, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Something went wrong or needs attention, inline where it happened: what, one line of why, the
 * one action that fixes it. Anything longer goes in [details], folded away behind "Details".
 *
 * A card like the groups around it, with the colour only on its filled glyph: a red or brown slab
 * was the loudest thing on Ride after the lime button, and a neutral one in Fill sat lighter than
 * the cards next to it. On a sheet or dialog it is Fill, one step above the sheet.
 */
@Composable
fun MhBanner(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    tone: MhTone = MhTone.ERROR,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    details: (@Composable ColumnScope.() -> Unit)? = null
) {
    val (glyph, glyphTint) = when (tone) {
        MhTone.ERROR -> Icons.Rounded.Error to MotoHubColors.Error
        MhTone.WARNING -> Icons.Rounded.Warning to MotoHubColors.Warning
        MhTone.NEUTRAL -> Icons.Rounded.Info to MotoHubColors.TextSecondary
        MhTone.LIVE, MhTone.PROGRESS -> Icons.Rounded.Info to MotoHubColors.Lime
    }
    val container = if (LocalMhInSheet.current) MotoHubColors.Fill else MaterialTheme.colorScheme.surface
    // Keyed on the message: a new failure in the same spot must not open with the last one's
    // details already showing.
    var expanded by rememberSaveable(title, body) { mutableStateOf(false) }
    val hasAction = actionLabel != null && onAction != null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            // The action row's 48 dp targets bring their own air below what they draw, so the
            // banner pads 12 under them: a compact pill still ends 16 from the edge.
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (hasAction || details != null) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(glyph, contentDescription = null, tint = glyphTint, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (body != null) {
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onDismiss != null) {
                // Pulled into the corner so the glyph lines up with the banner's padding while
                // the target stays 48 dp.
                MhIconButton(
                    Icons.Rounded.Close,
                    contentDescription = motoHubText("Close"),
                    onClick = onDismiss,
                    modifier = Modifier.offset(x = 12.dp, y = (-12).dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (hasAction || details != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actionLabel != null && onAction != null) {
                    // Compact: a full-size pill here outweighed the screen's own lime button.
                    MhSecondaryButton(actionLabel, onAction, size = MhButtonSize.COMPACT)
                }
                if (details != null) {
                    // Plain text, not MhTextButton: a TextButton pads its label 12 dp and centres
                    // it in a 58 dp minimum, so alone in the row it never lined up with the body.
                    // Here it starts on the body's line; after a pill the padding keeps the gap
                    // inside the target.
                    Text(
                        if (expanded) motoHubText("Hide details") else motoHubText("Details"),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = Role.Button) { expanded = !expanded }
                            .minimumInteractiveComponentSize()
                            .padding(start = if (hasAction) 12.dp else 0.dp, end = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (details != null) {
            AnimatedVisibility(visible = expanded, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
                Column(
                    modifier = Modifier.padding(start = 34.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = details
                )
            }
        }
    }
}

/**
 * A filled text field on the raised surface, with an optional password eye and helper line.
 *
 * The helper or error line goes in the field's own supporting-text slot, so TalkBack reads the
 * error while the field has focus. [monospace] marks a machine value (SSID, key, hex): it also
 * turns off autocorrect and capitalisation, or the keyboard "fixes" an SSID with a space in it.
 * Focus shows as a lime label - the cursor is lime already, so the field needs no glowing border.
 *
 * [placeholder] is what an empty field stands for ("My motorcycle" for an unnamed one). It shows
 * in grey under the label, focused or not, and the value stays empty.
 *
 * While it has focus and text, a field shows a clear button: one tap beats holding backspace in
 * gloves. A password field keeps its eye instead.
 */
@Composable
fun MhTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    error: String? = null,
    isPassword: Boolean = false,
    monospace: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    placeholder: String? = null
) {
    var revealed by rememberSaveable(label) { mutableStateOf(false) }
    val options = keyboardOptions
        .let { if (monospace) it.copy(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None) else it }
        .let { if (isPassword) it.copy(keyboardType = KeyboardType.Password) else it }
    val line = error ?: helper
    // Material 1.3 shows its placeholder only while the field has focus; unfocused and empty, the
    // field showed nothing but its label, which read as data missing. So the placeholder is drawn
    // as the field's text instead, in grey: the label then sits on top as it does over a value.
    // TalkBack reads it as the value, which it is in effect - it is the name shown everywhere else.
    val shownPlaceholder = placeholder?.takeIf { value.isEmpty() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        textStyle = MaterialTheme.typography.bodyLarge.let { if (monospace) it.copy(fontFamily = FontFamily.Monospace) else it },
        visualTransformation = when {
            shownPlaceholder != null -> VisualTransformation {
                TransformedText(
                    AnnotatedString(shownPlaceholder, SpanStyle(color = MotoHubColors.TextSecondaryOnFill)),
                    EmptyValueOffsets
                )
            }
            isPassword && !revealed -> PasswordVisualTransformation()
            else -> VisualTransformation.None
        },
        keyboardOptions = options,
        keyboardActions = keyboardActions,
        supportingText = line?.let { { Text(it) } },
        trailingIcon = when {
            isPassword -> {
                {
                    MhIconButton(
                        if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (revealed) motoHubText("Hide password") else motoHubText("Show password"),
                        onClick = { revealed = !revealed },
                        tint = MotoHubColors.TextSecondaryOnFill
                    )
                }
            }
            focused && enabled && value.isNotEmpty() -> {
                {
                    MhIconButton(
                        Icons.Rounded.Cancel,
                        contentDescription = motoHubText("Clear"),
                        onClick = { onValueChange("") },
                        tint = MotoHubColors.TextSecondaryOnFill
                    )
                }
            }
            else -> null
        },
        interactionSource = interaction,
        shape = MaterialTheme.shapes.small,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MotoHubColors.Fill,
            unfocusedContainerColor = MotoHubColors.Fill,
            disabledContainerColor = MotoHubColors.Fill,
            errorContainerColor = MotoHubColors.Fill,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MotoHubColors.TextSecondaryOnFill,
            focusedPlaceholderColor = MotoHubColors.TextSecondaryOnFill,
            unfocusedPlaceholderColor = MotoHubColors.TextSecondaryOnFill,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// The value under a placeholder is empty: every position in the grey text maps to its start.
private object EmptyValueOffsets : OffsetMapping {
    override fun originalToTransformed(offset: Int) = 0
    override fun transformedToOriginal(offset: Int) = 0
}

/** Nothing here yet: what it is, one line, the way to fill it. */
@Composable
fun MhEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: ImageVector? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MhIconCircle(icon, size = 56.dp)
        Spacer(Modifier.size(2.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.size(4.dp))
            MhPrimaryButton(actionLabel, onAction, icon = actionIcon)
        }
    }
}

/** A short paragraph under a group: the one sentence a setting needs and its row has no room for. */
@Composable
fun MhFootnote(text: String, modifier: Modifier = Modifier) {
    // On the card's edge, like the header above the group.
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
