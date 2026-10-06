// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.garage

import io.motohub.android.i18n.motoHubText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.motohub.android.androidauto.DisplayGeometry
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.tbox.TBoxCapabilities
import io.motohub.android.tbox.TBoxCapabilitySnapshot
import io.motohub.android.tbox.TBoxPortScanResult
import io.motohub.android.tbox.TBoxPortStatus
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhTone
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The developer inspector, reached from Motorcycle details › Advanced. Read-only. */
@Composable
fun TBoxCapabilityScreen(
    profile: MotorcycleProfile,
    snapshot: TBoxCapabilitySnapshot?,
    geometry: DisplayGeometry?,
    portScanInProgress: Boolean = false,
    portScanResult: TBoxPortScanResult? = null,
    onScanPorts: () -> Unit = {},
    onBack: () -> Unit
) {
    val capabilities = snapshot?.capabilities

    MhScreen(
        title = motoHubText("Dashboard capabilities"),
        subtitle = profile.shownName(),
        onBack = onBack
    ) {
        ObservationBanner(snapshot, capabilities)

        GarageSection(motoHubText("Connection")) {
            InspectorRow(motoHubText("Wi-Fi network"), profile.ssid, monospace = true)
            InspectorRow(
                motoHubText("EasyConn endpoint"),
                snapshot?.host?.let { "${it.ipAddress}:${it.port}" },
                monospace = true
            )
            InspectorRow(motoHubText("NSD package"), snapshot?.host?.packageName, monospace = true)
            InspectorRow(motoHubText("Last discovered"), formatTimestamp(snapshot?.discoveredAtEpochMillis))
        }

        GarageSection(motoHubText("Port check")) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    motoHubText("If discovery keeps failing, probe ports 10915–10935 to see which one answers."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MhSecondaryButton(
                    motoHubText("Scan common EasyConn ports"),
                    onClick = onScanPorts,
                    loading = portScanInProgress
                )
                portScanResult?.let { result -> PortScanResultView(result) }
            }
        }

        GarageSection(motoHubText("Display")) {
            InspectorRow(motoHubText("TFT capture area"), geometry?.let { "${it.width} x ${it.height}" }, monospace = true)
            InspectorRow(motoHubText("Orientation"), geometry?.orientationName())
            InspectorRow(motoHubText("Reported DPI"), capabilities?.dpi?.toString(), monospace = true)
            CapabilityRow(motoHubText("DPI mode"), capabilities?.dpiEnabled)
            InspectorRow(motoHubText("Screen type"), capabilities?.screenType?.toString(), monospace = true)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GarageSection(motoHubText("Reported identity")) {
                InspectorRow(motoHubText("Head unit name"), capabilities?.huName)
                InspectorRow(motoHubText("Vehicle brand"), capabilities?.carBrand)
                InspectorRow(motoHubText("Vehicle model"), capabilities?.carModel)
            }
            MhFootnote(motoHubText("Shown exactly as the dashboard reports them; MOTO-HUB does not guess the model."))
        }

        GarageSection(motoHubText("Software and protocol")) {
            InspectorRow(motoHubText("PXC version"), capabilities?.pxcVersion, monospace = true)
            InspectorRow(motoHubText("SDK version"), capabilities?.sdkVersion, monospace = true)
            InspectorRow(motoHubText("Software version"), capabilities?.versionName, monospace = true)
            InspectorRow(motoHubText("Version code"), capabilities?.versionCode, monospace = true)
            InspectorRow(motoHubText("Reported package"), capabilities?.packageName, monospace = true)
            InspectorRow(motoHubText("Product type"), capabilities?.productType?.toString(), monospace = true)
            InspectorRow(motoHubText("Transport type"), capabilities?.transportType?.toString(), monospace = true)
            InspectorRow(
                motoHubText("Function mask"),
                capabilities?.supportFunction?.let { "0x${it.toString(16).uppercase(Locale.ENGLISH)}" },
                monospace = true
            )
            InspectorRow(
                motoHubText("Wi-Fi socket timeout"),
                capabilities?.socketTimeoutPeriodWifi?.let { "$it ms" },
                monospace = true
            )
        }

        GarageSection(motoHubText("Features")) {
            CapabilityRow(motoHubText("Screen mirroring"), capabilities?.screenMirroring)
            CapabilityRow(motoHubText("Screen touch"), capabilities?.screenTouch)
            CapabilityRow(motoHubText("Overlay touch"), capabilities?.mirrorOverlayTouch)
            CapabilityRow(motoHubText("Mirror reconnect"), capabilities?.mirrorReconnect)
            CapabilityRow(motoHubText("Landscape adaptive"), capabilities?.landscapeAdaptive)
            CapabilityRow(motoHubText("Socket authentication"), capabilities?.socketServerAuth)
            CapabilityRow(motoHubText("Microphone"), capabilities?.microphone)
            CapabilityRow(motoHubText("HID input"), capabilities?.hid)
            CapabilityRow(motoHubText("Third-party apps"), capabilities?.thirdPartyApps)
            CapabilityRow(motoHubText("Phone signal"), capabilities?.phoneSignal)
            CapabilityRow(motoHubText("Time synchronization"), capabilities?.syncCorrectTime)
            CapabilityRow(motoHubText("Bluetooth calls"), capabilities?.bluetoothCall)
            CapabilityRow(motoHubText("Bluetooth settings"), capabilities?.bluetoothSettings)
        }

        MhFootnote(motoHubText("Sensitive CLIENT_INFO fields are intentionally excluded from storage and display."))
    }
}

@Composable
private fun ObservationBanner(
    snapshot: TBoxCapabilitySnapshot?,
    capabilities: TBoxCapabilities?
) {
    val complete = capabilities != null
    val title = when {
        complete -> motoHubText("Capability report captured")
        snapshot?.host != null -> motoHubText("Dashboard found")
        else -> motoHubText("No observations yet")
    }
    val detail = when {
        complete -> motoHubText("CLIENT_INFO was captured during an EasyConn handshake.")
        snapshot?.host != null -> motoHubText("Start mirroring or Android Auto once to capture CLIENT_INFO.")
        else -> motoHubText("Connect this motorcycle, then start mirroring or Android Auto once.")
    }
    val observed = formatTimestamp(snapshot?.capabilitiesObservedAtEpochMillis)
        ?.let { motoHubText("Observed %1\$s", it) }
    MhBanner(
        title = title,
        body = listOfNotNull(detail, observed).joinToString("\n"),
        tone = MhTone.NEUTRAL
    )
}

// Not an MhListRow: its value column is capped at 140 dp and would cut package names. Values here
// are never cut.
@Composable
private fun InspectorRow(
    label: String,
    value: String?,
    monospace: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.46f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: motoHubText("Not reported"),
            modifier = Modifier.weight(0.54f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else valueColor,
            fontFamily = if (monospace && value != null) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
private fun CapabilityRow(label: String, supported: Boolean?) {
    when (supported) {
        true -> InspectorRow(label, motoHubText("Supported"))
        false -> InspectorRow(label, motoHubText("Not supported"), valueColor = MaterialTheme.colorScheme.error)
        null -> InspectorRow(label, null)
    }
}

@Composable
private fun PortScanResultView(result: TBoxPortScanResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (result.peerIp == null) {
            Text(
                motoHubText("Could not derive a peer IP - this network may not have a usable route yet."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            return@Column
        }
        val open = result.entries.filter { it.status == TBoxPortStatus.OPEN }
        Text(
            motoHubText(
                "Peer %1\$s -> %2\$s",
                result.peerIp,
                if (open.isEmpty()) motoHubText("no open ports found")
                else motoHubText("open: %1\$s", open.joinToString { it.port.toString() })
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (open.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        // Only ports worth a second look: an accepted connection, or an explicit refusal (the
        // peer is alive and chose to reject it) - a silent timeout on most of the range is
        // expected and would just be noise here.
        result.entries.filter { it.status != TBoxPortStatus.NO_RESPONSE }.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    motoHubText("Port %1\$d", entry.port),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                // A machine value, shown as the enum names it.
                Text(
                    entry.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.status == TBoxPortStatus.OPEN)
                        MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun DisplayGeometry.orientationName(): String = when {
    width > height -> motoHubText("Landscape")
    width < height -> motoHubText("Portrait")
    else -> motoHubText("Square")
}

private fun formatTimestamp(epochMillis: Long?): String? = epochMillis?.let {
    DATE_FORMATTER.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
}

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy / HH:mm", Locale.ENGLISH)
