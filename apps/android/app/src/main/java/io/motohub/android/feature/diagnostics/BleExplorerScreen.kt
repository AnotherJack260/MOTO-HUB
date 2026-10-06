// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.diagnostics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.tbox.ThinkerRideGate
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTextButton
import io.motohub.android.ui.components.MhTextField
import io.motohub.android.ui.theme.MotoHubColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A workbench for any Bluetooth LE device, not just the ones MOTO-HUB already knows.
 *
 * The need is concrete: a handlebar remote that advertises no service UUIDs cannot be identified
 * by any app, and the only way to learn what it is, is to connect and ask it. Everything a rider
 * needs to do that is here — scan, connect, walk the tree, read, write, subscribe — and every byte
 * lands in the diagnostic log, so a remote can be worked out from a report without the device ever
 * leaving its owner.
 */
@Composable
fun BleExplorerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // A scan left running after the screen is gone keeps the radio awake with nobody watching.
    // Leaving the screen ends the scan and the link the same way the learn wizard does.
    DisposableEffect(Unit) {
        onDispose {
            BleExplorer.stopScan(context)
            BleExplorer.disconnect()
        }
    }

    val scanning by BleExplorer.scanning.collectAsState()
    val devices by BleExplorer.devices.collectAsState()
    val linkState by BleExplorer.linkState.collectAsState()
    val connected by BleExplorer.connected.collectAsState()
    val services by BleExplorer.services.collectAsState()
    val traffic by BleExplorer.traffic.collectAsState()
    val mtu by BleExplorer.mtu.collectAsState()

    var filter by remember { mutableStateOf("") }
    var namedOnly by remember { mutableStateOf(false) }
    var writing by remember { mutableStateOf<BleCharacteristicNode?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants -> if (grants.values.all { it }) BleExplorer.startScan(context) }
    val scanWithPermissions: () -> Unit = {
        if (ThinkerRideGate.hasBlePermissions(context)) {
            BleExplorer.startScan(context)
        } else {
            permissionLauncher.launch(ThinkerRideGate.blePermissions)
        }
    }

    val visible = devices.filter { entry ->
        (!namedOnly || entry.name?.isNotBlank() == true) &&
            (filter.isBlank() ||
                entry.label.contains(filter, ignoreCase = true) ||
                entry.address.contains(filter, ignoreCase = true) ||
                entry.serviceUuids.any { it.contains(filter, ignoreCase = true) })
    }

    MhScreen(
        title = motoHubText("Bluetooth LE explorer"),
        subtitle = motoHubText(
            "Find any Bluetooth LE device, open it, and see what it is really made of: " +
                "its services, its characteristics, and every byte it sends. Made for " +
                "remotes that announce nothing about themselves - press their buttons " +
                "while subscribed and the protocol writes itself into the log."
        ),
        onBack = onBack
    ) {
        if (scanning) {
            MhSecondaryButton(motoHubText("Stop scanning"), onClick = { BleExplorer.stopScan(context) })
        } else {
            MhPrimaryButton(motoHubText("Scan for devices"), onClick = scanWithPermissions)
        }

        if (connected != null) {
            ConnectedDevice(
                entry = connected,
                linkState = linkState,
                mtu = mtu,
                services = services,
                onDisconnect = { BleExplorer.disconnect() },
                onRediscover = { BleExplorer.rediscoverServices() },
                onRequestMtu = { BleExplorer.requestMtu(517) },
                onReadRssi = { BleExplorer.readRemoteRssi() },
                onRead = { BleExplorer.read(it) },
                onWrite = { writing = it },
                onToggleNotify = { BleExplorer.setNotifying(it, !it.notifying) }
            )
        }

        MhSectionHeader(motoHubText("Devices (%1\$d)", visible.size))
        MhTextField(
            value = filter,
            onValueChange = { filter = it },
            label = motoHubText("Filter by name, address or service")
        )
        MhListGroup {
            MhSwitchRow(title = motoHubText("Named devices only"), checked = namedOnly, onCheckedChange = { namedOnly = it })
        }
        if (visible.isEmpty()) {
            MhFootnote(
                if (scanning) {
                    motoHubText(
                        "Nothing yet. A remote that sleeps only advertises for a few seconds " +
                            "after a button is pressed - press one and keep it close."
                    )
                } else {
                    motoHubText("Start a scan to see what is around.")
                }
            )
        } else {
            MhListGroup {
                visible.forEach { entry ->
                    DeviceRow(entry = entry, onClick = { BleExplorer.connect(context, entry) })
                }
            }
        }

        TrafficView(traffic = traffic, onClear = { BleExplorer.clearTraffic() })
    }

    writing?.let { node ->
        WriteSheet(node = node, onDismiss = { writing = null })
    }
}

@Composable
private fun DeviceRow(entry: BleScanEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                entry.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${entry.rssi} dBm",
                style = MaterialTheme.typography.labelMedium,
                color = signalColour(entry.rssi)
            )
        }
        Text(
            if (entry.connectable) entry.address else motoHubText("%1\$s · not connectable", entry.address),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // The three fields that identify an unknown device, when it offers any of them: the
        // services it claims, the vendor id in its manufacturer data, and whatever it puts in
        // service data. A remote that shows none of these can only be identified by connecting.
        if (entry.serviceUuids.isNotEmpty()) {
            DeviceDetail(
                motoHubText(
                    "Services: %1\$s",
                    entry.serviceUuids.joinToString { BleNames.short(java.util.UUID.fromString(it)) }
                )
            )
        }
        if (entry.manufacturer.isNotEmpty()) {
            DeviceDetail(motoHubText("Manufacturer: %1\$s", entry.manufacturer.joinToString()))
        }
        if (entry.serviceData.isNotEmpty()) {
            DeviceDetail(motoHubText("Service data: %1\$s", entry.serviceData.joinToString()))
        }
    }
}

@Composable
private fun DeviceDetail(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ConnectedDevice(
    entry: BleScanEntry?,
    linkState: BleLinkState,
    mtu: Int,
    services: List<BleServiceNode>,
    onDisconnect: () -> Unit,
    onRediscover: () -> Unit,
    onRequestMtu: () -> Unit,
    onReadRssi: () -> Unit,
    onRead: (BleCharacteristicNode) -> Unit,
    onWrite: (BleCharacteristicNode) -> Unit,
    onToggleNotify: (BleCharacteristicNode) -> Unit
) {
    MhSectionHeader(motoHubText("Connected"))
    MhListGroup {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(entry?.label.orEmpty(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "${entry?.address.orEmpty()}  -  ${linkState.name.lowercase()}  -  MTU $mtu",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MhSecondaryButton(motoHubText("Disconnect"), onDisconnect, Modifier.weight(1f))
                MhSecondaryButton(motoHubText("Rediscover"), onRediscover, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MhSecondaryButton(motoHubText("MTU 517"), onRequestMtu, Modifier.weight(1f))
                MhSecondaryButton(motoHubText("Read RSSI"), onReadRssi, Modifier.weight(1f))
            }
        }
    }
    services.forEach { service ->
        Text(
            BleNames.describe(service.uuid),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MhListGroup {
            service.characteristics.forEach { node ->
                CharacteristicRow(
                    node = node,
                    onRead = { onRead(node) },
                    onWrite = { onWrite(node) },
                    onToggleNotify = { onToggleNotify(node) }
                )
            }
        }
    }
}

@Composable
private fun CharacteristicRow(
    node: BleCharacteristicNode,
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onToggleNotify: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            BleNames.describe(node.uuid),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            node.propertyLabels().joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        node.lastValue?.let { value ->
            Text(
                BleHex.encode(value) + BleHex.ascii(value).let { text ->
                    if (text.any { it != '.' }) "   \"$text\"" else ""
                },
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (node.canRead) {
                MhSecondaryButton(motoHubText("Read"), onRead, Modifier.weight(1f))
            }
            if (node.canWrite || node.canWriteNoResponse) {
                MhSecondaryButton(motoHubText("Write"), onWrite, Modifier.weight(1f))
            }
            if (node.canNotify || node.canIndicate) {
                MhSecondaryButton(
                    if (node.notifying) motoHubText("Unsubscribe") else motoHubText("Subscribe"),
                    onToggleNotify,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

/** Bytes for one characteristic. Swiping the sheet away cancels; a write closes it first. */
@Composable
private fun WriteSheet(node: BleCharacteristicNode, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val bytes = BleHex.decode(text)
    MhSheet(onDismiss = onDismiss, title = motoHubText("Write to %1\$s", BleNames.short(node.uuid))) { close ->
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MhFootnote(
                motoHubText(
                    "Bytes in hex - \"01 FF 0A\", \"01ff0a\" and \"0x01,0xFF\" all work. " +
                        "This writes straight to the device: on some hardware the wrong value " +
                        "changes settings that are hard to change back."
                )
            )
            MhTextField(
                value = text,
                onValueChange = { text = it },
                label = motoHubText("Hex"),
                helper = bytes?.let { motoHubText("%1\$d byte(s)", it.size) },
                error = if (bytes == null) motoHubText("Not valid hex yet.") else null,
                monospace = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )
            MhPrimaryButton(
                motoHubText("Write"),
                onClick = { bytes?.let { value -> close { BleExplorer.write(node, value, true) } } },
                enabled = bytes != null && node.canWrite
            )
            if (node.canWriteNoResponse) {
                MhSecondaryButton(
                    motoHubText("Write without response"),
                    onClick = { bytes?.let { value -> close { BleExplorer.write(node, value, false) } } },
                    enabled = bytes != null
                )
            }
        }
    }
}

@Composable
private fun TrafficView(traffic: List<BleTrafficLine>, onClear: () -> Unit) {
    val clock = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MhSectionHeader(motoHubText("Traffic"))
        Spacer(Modifier.weight(1f))
        MhTextButton(motoHubText("Clear"), onClick = onClear)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 340.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (traffic.isEmpty()) {
                Text(
                    motoHubText("Nothing yet."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            traffic.forEach { line ->
                Text(
                    "${clock.format(Date(line.atMillis))}  ${prefix(line.kind)} ${line.text}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = when (line.kind) {
                        BleTrafficLine.Kind.IN -> MaterialTheme.colorScheme.onSurface
                        BleTrafficLine.Kind.OUT -> MaterialTheme.colorScheme.onSurfaceVariant
                        BleTrafficLine.Kind.FAILURE -> MaterialTheme.colorScheme.error
                        BleTrafficLine.Kind.INFO -> MotoHubColors.TextTertiary
                    }
                )
            }
        }
    }
    MhFootnote(
        motoHubText(
            "Every line here is also in the application log, so a session can be sent in a " +
                "diagnostic report."
        )
    )
}

private fun prefix(kind: BleTrafficLine.Kind): String = when (kind) {
    BleTrafficLine.Kind.IN -> "<-"
    BleTrafficLine.Kind.OUT -> "->"
    BleTrafficLine.Kind.FAILURE -> "!!"
    BleTrafficLine.Kind.INFO -> "  "
}

private fun signalColour(rssi: Int): Color = when {
    rssi >= -60 -> MotoHubColors.Lime
    rssi >= -80 -> MotoHubColors.Warning
    else -> MotoHubColors.TextTertiary
}
