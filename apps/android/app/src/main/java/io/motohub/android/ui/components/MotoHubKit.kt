// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.theme.MotoHubColors

// The building blocks every screen is drawn from. See documentation/DESIGN_SYSTEM.md for when to
// use which; the short version is one lime button per screen, everything else a quieter button
// or a row in a group.

// Both buttons fill the width unless told otherwise, and that lives in [fillWidth] rather than in
// the default modifier: a caller passing Modifier.padding(...) would otherwise silently lose it.
// While loading they stay in their enabled colours - the button is busy, not unavailable, and a
// grey container would swallow the spinner.

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
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillWidthIf(fillWidth).heightIn(min = 56.dp),
        shape = CircleShape,
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

/** Every other action that deserves a button: grey pill, red text when it undoes something. */
@Composable
fun MhSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
    icon: ImageVector? = null,
    fillWidth: Boolean = true
) {
    val content = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        // 56 like the primary: stacked in a sheet the pair reads as one set, and gloves need it.
        modifier = modifier.fillWidthIf(fillWidth).heightIn(min = 56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MotoHubColors.Fill,
            contentColor = content,
            disabledContainerColor = MotoHubColors.Fill,
            disabledContentColor = if (loading) content else MotoHubColors.TextTertiary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        ButtonContent(text, icon, loading)
    }
}

private fun Modifier.fillWidthIf(fill: Boolean) = if (fill) fillMaxWidth() else this

/**
 * How a sheet's or dialog's main action is drawn. LIME is the one action of that layer; NEUTRAL
 * is for answers the app must not nudge (consent, trust); DESTRUCTIVE is red text on a Fill pill
 * and gives the confirm haptic itself, so no caller has to remember it.
 */
enum class MhActionStyle { LIME, NEUTRAL, DESTRUCTIVE }

/** The stacked pill MhSheet and MhDialog draw for [style]. */
@Composable
internal fun MhActionButton(text: String, style: MhActionStyle, onClick: () -> Unit) {
    val view = LocalView.current
    when (style) {
        MhActionStyle.LIME -> MhPrimaryButton(text, onClick)
        MhActionStyle.NEUTRAL -> MhSecondaryButton(text, onClick)
        MhActionStyle.DESTRUCTIVE -> MhSecondaryButton(
            text,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
            },
            destructive = true
        )
    }
}

/** "Cancel", "Details", "Not now": text with a full-size touch target. */
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
private fun ButtonContent(text: String, icon: ImageVector?, loading: Boolean) {
    if (loading) {
        CircularProgressIndicator(Modifier.size(20.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
    } else if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
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

/** Sentence-case label above a group. */
@Composable
fun MhSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    enabled: Boolean = true,
    showChevron: Boolean = true,
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
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun MhSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MotoHubColors.Background,
            checkedTrackColor = MotoHubColors.Lime,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = MotoHubColors.TextSecondary,
            uncheckedTrackColor = MotoHubColors.Fill,
            uncheckedBorderColor = Color.Transparent
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
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
 * that blinks for a whole ride stops meaning anything. The pulse is read in the draw phase, so it
 * repaints the dot without recomposing the chip sixty times a second.
 */
@Composable
fun MhStatusChip(text: String, tone: MhTone, modifier: Modifier = Modifier) {
    val (fg, bg) = tone.colors()
    val pulse = if (tone == MhTone.PROGRESS) {
        rememberInfiniteTransition(label = "chip").animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "dot"
        )
    } else null
    Row(
        modifier = modifier
            .background(bg, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).graphicsLayer { alpha = pulse?.value ?: 1f }.background(fg, CircleShape))
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Something went wrong or needs attention, inline where it happened: what, one line of why, the
 * one action that fixes it. Anything longer goes in [details], folded away behind "Details".
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
    val (fg, bg) = tone.colors()
    // Keyed on the message: a new failure in the same spot must not open with the last one's
    // details already showing.
    var expanded by rememberSaveable(title, body) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                when (tone) {
                    MhTone.ERROR -> Icons.Rounded.ErrorOutline
                    MhTone.WARNING -> Icons.Rounded.WarningAmber
                    else -> Icons.Rounded.Info
                },
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(22.dp)
            )
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
        if ((actionLabel != null && onAction != null) || details != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actionLabel != null && onAction != null) {
                    MhSecondaryButton(actionLabel, onAction, fillWidth = false)
                }
                if (details != null) {
                    MhTextButton(
                        if (expanded) motoHubText("Hide details") else motoHubText("Details"),
                        onClick = { expanded = !expanded },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (details != null) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(MOTION_MILLIS)) + expandVertically(tween(MOTION_MILLIS)),
                exit = fadeOut(tween(MOTION_MILLIS)) + shrinkVertically(tween(MOTION_MILLIS))
            ) {
                Column(
                    modifier = Modifier.padding(start = 34.dp),
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
    enabled: Boolean = true
) {
    var revealed by rememberSaveable(label) { mutableStateOf(false) }
    val options = keyboardOptions
        .let { if (monospace) it.copy(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None) else it }
        .let { if (isPassword) it.copy(keyboardType = KeyboardType.Password) else it }
    val line = error ?: helper
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        textStyle = MaterialTheme.typography.bodyLarge.let { if (monospace) it.copy(fontFamily = FontFamily.Monospace) else it },
        visualTransformation = if (isPassword && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = options,
        keyboardActions = keyboardActions,
        supportingText = line?.let { { Text(it) } },
        trailingIcon = if (!isPassword) null else {
            {
                MhIconButton(
                    if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (revealed) motoHubText("Hide password") else motoHubText("Show password"),
                    onClick = { revealed = !revealed },
                    tint = MotoHubColors.TextSecondaryOnFill
                )
            }
        },
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
    Text(
        text,
        modifier = modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
