// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.diagnostics.report

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.motohub.android.data.MotorcycleProfileStore
import io.motohub.android.feature.settings.MotoHubSettings
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.InstallationId
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhMotion
import io.motohub.android.ui.components.MhPop
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTone
import io.motohub.android.ui.components.MotoHubSnackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The body of Settings ▸ Diagnostics: the Support ID a rider reads out when asking for help, the
 * log, sending a report now or automatically, and what a report holds and how it is handled.
 *
 * A build without a collector (`MOTOHUB_DIAGNOSTICS_ENDPOINT` unset: local builds, forks) has
 * nowhere to send to, so the two report controls and their footnote aren't shown at all - a dead
 * row and a switch that does nothing read as broken. The log can still be shared by hand.
 *
 * The logging master switch lives here too, next to the notice that says the log has stopped,
 * so the two share one state and the notice appears the moment the switch goes off - not on the
 * next visit, by which time the rider has already sent the report.
 *
 * The screen keeps its name: the English-only privacy notice and the translated consent copy all
 * send riders to "Settings ▸ Diagnostics" and to the Support ID "shown at the top".
 */
@Composable
fun SupportIdSection(onOpenApplicationLogs: () -> Unit) {
    val context = LocalContext.current
    var supportId by remember { mutableStateOf<String?>(null) }
    var loggingEnabled by remember { mutableStateOf(MotoHubSettings.loggingEnabled(context)) }
    var autoUpload by remember { mutableStateOf(DiagnosticReportSettings.autoUploadEnabled(context)) }
    var reviewingNotice by remember { mutableStateOf(false) }
    var readingPrivacyNotice by remember { mutableStateOf(false) }
    // The last successful upload's time when the rider tapped "Send a report now"; null while no
    // request of theirs is open. A newer time once the upload ends means it went out.
    var requestedAt by remember { mutableStateOf<Long?>(null) }
    // The copy glyph turns into a lime check for a moment: the row's only sign it was copied on
    // Android 13+, where the system's clipboard confirmation replaces the snackbar.
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MILLIS)
            copied = false
        }
    }
    val copyAlpha by animateFloatAsState(if (copied) 0f else 1f, tween(MhMotion.FAST), label = "copy-glyph")
    val canSend = DiagnosticReportUploader.configured
    val status by DiagnosticReportScheduler.status.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        DiagnosticReportScheduler.refreshStatus(context)
        supportId = withContext(Dispatchers.IO) {
            val activeId = runCatching { MotorcycleProfileStore(context).load()?.id }.getOrNull()
            InstallationId.supportId(context, activeId)
        }
    }
    // Only the rider's own request gets a snackbar; automatic uploads stay silent. The store is
    // read rather than the status: the scheduler clears inProgress a moment before it republishes
    // the result, and the store already holds it by then.
    // ponytail: misses the snackbar if the whole upload starts and ends between two frames
    // (StateFlow conflation); the row's subtitle still shows the outcome.
    LaunchedEffect(status.inProgress) {
        val before = requestedAt ?: return@LaunchedEffect
        if (status.inProgress) return@LaunchedEffect
        requestedAt = null
        if (DiagnosticReportSettings.lastUploadAt(context) > before) {
            MotoHubSnackbar.success(context, motoHubText("Report sent"))
        } else {
            MotoHubSnackbar.error(context, motoHubText("Couldn't send the report"))
        }
    }

    // Read when the switch changes, not on every recomposition: the answer only moves while
    // logging is on, and while it is on this is not shown at all.
    val frozenAt = remember(loggingEnabled) {
        if (loggingEnabled) null else ProjectionEventLog.lastEntryAtMillis()
    }
    if (!loggingEnabled) LoggingOffNotice(frozenAt)

    MhSectionHeader(motoHubText("Support"))
    MhListGroup {
        MhListRow(
            title = motoHubText("Support ID"),
            subtitle = motoHubText("Quote it when asking for help"),
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        supportId?.let(InstallationId::shortForm) ?: "…",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).graphicsLayer { alpha = copyAlpha }
                        )
                        MhPop(copied) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            onClick = {
                val id = supportId ?: return@MhListRow
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                    ClipData.newPlainText(motoHubText("MOTO-HUB Support ID"), id)
                )
                copied = true
                // Android 13+ confirms a copy itself; a second confirmation would repeat it.
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    MotoHubSnackbar.success(context, motoHubText("Support ID copied"))
                }
            }
        )
        // Support asks riders for the log, and a build without a collector can only share it.
        MhListRow(
            title = motoHubText("Application logs"),
            subtitle = motoHubText("Copy or share the log"),
            onClick = onOpenApplicationLogs
        )
        if (canSend) {
            MhListRow(
                title = motoHubText("Send a report now"),
                // The frozen log is said here as well as in the notice above, because this is the
                // row the thumb is on when it decides. Still tappable: a report is the rider's to
                // send, and even a stale log carries the identity and version fields support asks for.
                subtitle = when {
                    status.inProgress -> motoHubText("Sending…")
                    !loggingEnabled && frozenAt != null -> motoHubText("Logging is off; the log ends %1\$s", stamp(frozenAt))
                    !loggingEnabled -> motoHubText("Logging is off; there is no log to send")
                    // The raw error stays in the store and in the log, where support reads it.
                    status.lastError != null -> motoHubText("Couldn't send the report")
                    status.lastUploadAt > 0L -> motoHubText("Last sent %1\$s", stamp(status.lastUploadAt))
                    else -> motoHubText("Never sent")
                },
                onClick = {
                    if (status.inProgress) return@MhListRow
                    ProjectionEventLog.record("SUPPORT", "Diagnostics report requested by the rider.")
                    requestedAt = DiagnosticReportSettings.lastUploadAt(context)
                    DiagnosticReportScheduler.sendNow(context)
                }
            )
            MhSwitchRow(
                title = motoHubText("Send reports automatically"),
                subtitle = motoHubText("At most once a day, after an update or a crash"),
                checked = autoUpload,
                onCheckedChange = {
                    autoUpload = it
                    DiagnosticReportSettings.setAutoUploadEnabled(context, it)
                    if (!it) DiagnosticReportSettings.setPending(context, false)
                    ProjectionEventLog.record("SETTINGS", "Automatic diagnostics upload changed to enabled=$it.")
                }
            )
        }
    }
    if (canSend) {
        MhFootnote(
            motoHubText(
                "Reports hold your dashboard and phone models, the Android, Android Auto and MOTO-HUB " +
                    "versions, and the app log. Never passwords, positions or hardware addresses."
            )
        )
    }

    MhSectionHeader(motoHubText("Privacy"))
    MhListGroup {
        MhListRow(title = motoHubText("What gets sent"), onClick = { reviewingNotice = true })
        // The long form behind the summary above, and the only place a rider can find out how to
        // have their reports deleted - which is why it sits here rather than behind the notice.
        // It names where reports go, so a build that cannot send one leaves it out.
        if (canSend) {
            MhListRow(title = motoHubText("How your data is handled"), onClick = { readingPrivacyNotice = true })
        }
        MhSwitchRow(
            title = motoHubText("Keep a diagnostic log"),
            subtitle = motoHubText("Stays on this phone unless you send a report"),
            checked = loggingEnabled,
            onCheckedChange = {
                // Record the "why" before flipping off, and after flipping back on - the
                // gap in between is the point, but a change of this kind should still be
                // visible in the log itself, on either side of it.
                if (it) {
                    MotoHubSettings.setLoggingEnabled(context, true)
                    loggingEnabled = true
                    ProjectionEventLog.record("SETTINGS", "Logging enabled.")
                } else {
                    ProjectionEventLog.record("SETTINGS", "Logging disabled by the user.")
                    MotoHubSettings.setLoggingEnabled(context, false)
                    loggingEnabled = false
                }
            }
        )
    }

    // Answering here is a real answer: a rider who reads what would leave the phone and decides
    // either way must be able to say so at that moment, not be sent looking for a separate
    // switch. Closing it without answering leaves the setting exactly as it was.
    if (reviewingNotice) {
        DiagnosticReportNoticeDialog(
            onAccept = {
                autoUpload = true
                DiagnosticReportSettings.setAutoUploadEnabled(context, true)
                ProjectionEventLog.record("SETTINGS", "Automatic diagnostics upload confirmed from the notice.")
            },
            onDecline = {
                autoUpload = false
                DiagnosticReportSettings.setAutoUploadEnabled(context, false)
                DiagnosticReportSettings.setPending(context, false)
                ProjectionEventLog.record("SETTINGS", "Automatic diagnostics upload declined from the notice.")
            },
            onDismiss = { reviewingNotice = false }
        )
    }
    if (readingPrivacyNotice) {
        PrivacyNoticeDialog(onDismiss = { readingPrivacyNotice = false })
    }
}

/**
 * Says that the log stopped, and when.
 *
 * Turning the log off keeps everything recorded until then - the switch stops new entries, it
 * never erases what is already there - which is exactly what makes this worth saying: what is
 * left still reads like a complete log, and a report sent now carries it. The switch is also
 * per-app, so ADVANCED can be frozen while CORE keeps writing, and the two halves of one report
 * then describe two different weeks.
 *
 * Neutral and offering nothing to tap: nothing is broken, this is a setting the rider chose, and
 * the switch that undoes it is on this screen.
 */
@Composable
private fun LoggingOffNotice(frozenAtMillis: Long?) {
    MhBanner(
        title = motoHubText("Logging is off"),
        body = if (frozenAtMillis != null) {
            motoHubText("Nothing recorded since %1\$s, so reports carry only the old log.", stamp(frozenAtMillis))
        } else {
            motoHubText("Nothing has been recorded yet.")
        },
        tone = MhTone.NEUTRAL
    )
}

/** A log entry's own timestamp, in the phone's locale: this is read against a rider's memory. */
private fun stamp(epochMillis: Long): String = STAMP_FORMAT.format(Date(epochMillis))

private val STAMP_FORMAT = SimpleDateFormat("d MMM HH:mm", Locale.getDefault())

/** How long the Support ID row shows its check after a copy. */
private const val COPIED_MILLIS = 1500L
