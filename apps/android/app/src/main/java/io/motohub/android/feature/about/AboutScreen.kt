// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.about

import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.i18n.motoHubText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.motohub.android.BuildConfig
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhIconCircle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSectionHeader

const val MOTO_HUB_GITHUB_URL = "https://github.com/vincenzobpt/MOTO-HUB"
const val MOTO_HUB_DISCORD_URL = "https://discord.gg/FzhXZtPhC8"

/** Taps on the version row that reveal an edition's hidden prototype page. */
private const val PROTOTYPE_UNLOCK_TAP_COUNT = 10

@Composable
fun AboutScreen(
    onOpenGithub: () -> Unit,
    onOpenDiscord: () -> Unit,
    onCheckUpdates: () -> Unit,
    onBack: () -> Unit,
    /** Editions with a hidden prototype pass this; where it is null the version
     *  row is inert and no unlock exists. This screen is shared by both
     *  flavors, so it never names what it unlocks. */
    onUnlockPrototype: (() -> Unit)? = null,
    /** Only the edition that actually draws maps passes true. CORE ships no map, no geocoder and
     *  no routing, so crediting OpenStreetMap there would claim a dependency it does not have. */
    showsMaps: Boolean = false,
    /** A check is in flight - the rider's own or the automatic one at launch. The row waits for it
     *  rather than taking a tap the running check would swallow. */
    checkingForUpdates: Boolean = false
) {
    val context = LocalContext.current
    // Android developer-options style easter egg. The count resets every time the About screen is
    // reopened, and the row is not clickable at all in an edition that passes no unlock.
    var tapCount by remember { mutableIntStateOf(0) }

    MhScreen(title = null, onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MhIconCircle(Icons.Rounded.TwoWheeler, size = 56.dp)
            Text("MOTO-HUB", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                motoHubText("Mirroring and Android Auto for dashboards that pair over EasyConn."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MhListGroup {
            MhListRow(
                title = motoHubText("Version"),
                icon = Icons.Rounded.Info,
                showChevron = false,
                trailing = {
                    Text(
                        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = onUnlockPrototype?.let { unlock ->
                    {
                        tapCount++
                        val remaining = PROTOTYPE_UNLOCK_TAP_COUNT - tapCount
                        when {
                            remaining <= 0 -> {
                                tapCount = 0
                                MotoHubSnackbar.success(context, motoHubText("Prototype unlocked"))
                                unlock()
                            }
                            remaining <= 3 -> MotoHubSnackbar.show(context, motoHubText("%d taps away from the prototype", remaining))
                        }
                    }
                }
            )
            MhListRow(
                title = motoHubText("Check for updates"),
                icon = Icons.Rounded.SystemUpdate,
                enabled = !checkingForUpdates,
                trailing = if (checkingForUpdates) {
                    { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
                } else {
                    null
                },
                onClick = onCheckUpdates
            )
        }
        MhSectionHeader(motoHubText("Community"))
        MhListGroup {
            MhListRow(
                title = motoHubText("Discord"),
                subtitle = motoHubText("Ask questions and report a problem"),
                icon = Icons.Rounded.Forum,
                onClick = onOpenDiscord
            )
            MhListRow(
                title = motoHubText("GitHub"),
                subtitle = motoHubText("Source code, releases and issues"),
                icon = Icons.Rounded.Code,
                onClick = onOpenGithub
            )
        }
        if (showsMaps) MapCredits()
        MhFootnote(
            motoHubText(
                "Experimental software, tested on a CFMOTO 700MT-ADV with OnePlus 13 and Galaxy Z Fold4 " +
                    "phones. Other motorcycles and phones may behave differently or not connect. Don't " +
                    "rely on it for critical navigation; use it at your own risk."
            )
        )
        MhFootnote(
            motoHubText(
                "MOTO-HUB is an independent project. It is not affiliated with, endorsed by, " +
                    "or sponsored by Carbit, CFMOTO, any other manufacturer whose dashboard uses " +
                    "EasyConn, Google, or Android Auto. All product names and marks belong to " +
                    "their respective owners."
            )
        )
    }
}

/**
 * Where the map data comes from, and who it belongs to.
 *
 * OpenStreetMap is under the ODbL and its attribution guidance asks for the credit to be visible
 * to the person looking at the map - on the map, or one step away from it - which a line in the
 * repository README does not satisfy. MapLibre Native is BSD-2-Clause and asks for its notice to
 * travel with the binary. The phone maps also show MapLibre's own attribution control; this
 * section is what covers the dashboard, where a TFT glanced at mid-ride has no room for one.
 */
@Composable
private fun MapCredits() {
    MhSectionHeader(motoHubText("Maps and data"))
    MhFootnote(
        motoHubText(
            "Maps, addresses and routes are built on data by © OpenStreetMap " +
                "contributors, licensed under the ODbL."
        )
    )
    MhFootnote(
        motoHubText(
            "Map rendering by MapLibre Native (BSD-2-Clause). Vector tiles by OpenFreeMap, " +
                "to the OpenMapTiles schema; raster tiles by the OpenStreetMap " +
                "Foundation. Address search by Photon. Routing by Valhalla, hosted by " +
                "Stadia Maps or the FOSSGIS demo server. Places by Overpass. Weather by " +
                "Open-Meteo. Petrol prices published as open data by Spain's Ministerio " +
                "para la Transición Ecológica, Portugal's DGEG, the French Ministère de " +
                "l'Économie and Italy's MIMIT. DGEG's data may not be used commercially."
        )
    )
}
