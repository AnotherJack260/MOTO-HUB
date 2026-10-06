// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.androidauto

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.tbox.CompanionAppRegistry
import io.motohub.android.tbox.CompanionConflictGate
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhTextButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One gate, two flavors. CORE and ADVANCED each have their own `MainActivity`, and the old
 * companion-app warning lived as two near-identical copies in both; the check this replaces it
 * with has real logic in it - a socket probe, a runtime guard, a coroutine - and that is not
 * something to keep in duplicate.
 *
 * A caller wraps whatever starts a projection: `gate("Android Auto") { continueAndroidAutoStart() }`
 * runs the block immediately when the ports are free, and otherwise raises
 * [CompanionConflictGateDialog] instead of letting the rider walk into a handshake that cannot
 * succeed.
 */
@Stable
class CompanionConflictGateState internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val alsoActive: () -> Boolean
) {
    internal data class Pending(
        val conflict: CompanionConflictGate.Verdict.Conflict,
        val actionLabel: String,
        val proceed: () -> Unit
    )

    internal var pending by mutableStateOf<Pending?>(null)
        private set

    /**
     * @param actionLabel what the rider asked for, in the log ("Android Auto", "Mirroring").
     */
    fun gate(actionLabel: String, onProceed: () -> Unit) {
        scope.launch {
            when (val verdict = CompanionConflictGate.evaluate(context, alsoActive)) {
                CompanionConflictGate.Verdict.Clear -> onProceed()
                is CompanionConflictGate.Verdict.Conflict -> {
                    ProjectionEventLog.record(
                        "ANDROID_AUTO",
                        "$actionLabel start held back: the EasyConn reverse ports are already in " +
                            "use, so the rider is asked to free them first."
                    )
                    pending = Pending(verdict, actionLabel, onProceed)
                }
            }
        }
    }

    internal fun clear() {
        pending = null
    }
}

@Composable
fun rememberCompanionConflictGate(
    alsoActive: () -> Boolean = { false }
): CompanionConflictGateState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) { CompanionConflictGateState(context, scope, alsoActive) }
}

/**
 * States the conflict as the fact it is, and offers the only remedy Android leaves: the rider
 * force-stopping the holder from its app settings.
 *
 * There is deliberately no "do not show this again". The warning that had one was a guess about
 * an installed app; this one only ever appears when the ports are held at that very instant, and
 * a rider who silences it silences the one screen that explains why nothing works.
 *
 * The sheet stays open while the rider is away in the other app's settings, so "Try anyway" is
 * right there when they come back - which is also why a settings page that will not open is said
 * inside the sheet rather than in a snackbar that would draw underneath it. With no companion
 * detected there is nothing lime: "Try anyway" is a risky choice, so it is never the primary.
 */
@Composable
fun CompanionConflictGateDialog(state: CompanionConflictGateState) {
    val pending = state.pending ?: return
    val context = LocalContext.current
    val companion = pending.conflict.companionApp
    val holderName = companion?.displayName
    var openFailed by remember(pending) { mutableStateOf(false) }
    MhSheet(
        onDismiss = { state.clear() },
        title = if (holderName != null) {
            motoHubText("%1\$s is using the dashboard", holderName)
        } else {
            motoHubText("Another app is using the dashboard")
        },
        body = if (holderName != null) {
            motoHubText("Force-stop it in its app settings, then try again.")
        } else {
            motoHubText("Force-stop your motorcycle's companion app, then try again.")
        }
    ) { close ->
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MhFootnote(motoHubText("Running a session in your other MOTO-HUB app? Stop it there instead."))
            Text(
                motoHubText("Busy ports: %1\$s", pending.conflict.busyPorts.joinToString()),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (companion != null) {
                MhPrimaryButton(
                    motoHubText("Open %1\$s app settings", companion.displayName),
                    onClick = { openFailed = !CompanionAppRegistry.openAppSettings(context, companion) },
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (openFailed) {
                    Text(
                        motoHubText("Couldn't open app settings"),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            MhSecondaryButton(
                motoHubText("Try anyway"),
                onClick = {
                    // pending, not state.pending: close() clears the state before this runs.
                    close {
                        ProjectionEventLog.record(
                            "ANDROID_AUTO",
                            "Rider started ${pending.actionLabel} anyway, with the reverse ports held."
                        )
                        pending.proceed()
                    }
                }
            )
            MhTextButton(motoHubText("Cancel"), onClick = { close {} }, modifier = Modifier.fillMaxWidth())
        }
    }
}
