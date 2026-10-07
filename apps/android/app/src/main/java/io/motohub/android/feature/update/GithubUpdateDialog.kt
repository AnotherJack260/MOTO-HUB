// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.update

import io.motohub.android.i18n.motoHubText

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.motohub.android.BuildConfig
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhMotion
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhStatusChip
import io.motohub.android.ui.components.MhTextButton
import io.motohub.android.ui.components.MhTone
import io.motohub.android.ui.theme.MotoHubColors

/**
 * The newer release, as a sheet that opens only when there is one: an empty or failed manual check
 * is a snackbar in the host, not this. [error] is an install failure only.
 *
 * Its buttons do not close it. The download runs with the sheet up, so the rider sees it progress
 * and, when it fails, the reason - and swiping it away is "later".
 */
@Composable
fun GithubUpdateDialog(
    release: GithubRelease,
    error: String?,
    installingTag: String?,
    installingProgress: DownloadProgress?,
    onDismiss: () -> Unit,
    onInstall: (GithubRelease) -> Unit,
    onSkip: (GithubRelease) -> Unit,
    onAllowUnknownSources: () -> Unit,
    canInstallUnknownSources: Boolean
) {
    val context = LocalContext.current
    // Asked once per release the rider actually taps, not on every recomposition: the answer is a
    // live capability read, and the moment that matters is the moment they press the button.
    var meteredConfirmation by remember { mutableStateOf<MeteredDownload?>(null) }
    // Off the main thread: choosing the network walks ConnectivityManager.allNetworks and reads
    // capabilities for each, which is binder work, and this runs from a tap.
    val networkScope = rememberCoroutineScope()
    val installing = installingTag == release.tagName
    // Kept for the fold out: the banner leaves with the words it came with.
    val lastError = remember { mutableStateOf(error) }
    if (error != null) lastError.value = error
    // The metered question is the same sheet with its words swapped, not a second window on top.
    // Title, body and actions swap as one block, fading through while the sheet's height follows
    // on the same clock - the kit's own title and body would have snapped while the rest faded.
    MhSheet(onDismiss = onDismiss) { close ->
        AnimatedContent(
            targetState = meteredConfirmation,
            transitionSpec = { MhMotion.fadeThrough(animateHeight = true) },
            label = "update-sheet"
        ) { metered ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetHeading(
                    title = if (metered != null) {
                        motoHubText("Download over %1\$s?", metered.networkDescription)
                    } else {
                        motoHubText("Update available")
                    },
                    body = metered?.let {
                        motoHubText(
                            "MOTO-HUB %1\$s is %2\$s. Your only Internet connection right now is %3\$s, which " +
                                "your operator may charge for.",
                            it.release.versionName,
                            with(GithubUpdateInstaller) { it.release.apkAsset.sizeText() },
                            it.networkDescription
                        )
                    }
                )
                if (metered != null) {
                    MeteredQuestion(
                        onDownload = {
                            meteredConfirmation = null
                            onInstall(metered.release)
                        },
                        onWait = { meteredConfirmation = null }
                    )
                } else {
                    MhListRow(
                        title = motoHubText("New version"),
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (release.isPrerelease) MhStatusChip(motoHubText("Pre-release"), MhTone.NEUTRAL)
                                MonoValue(release.versionName)
                            }
                        }
                    )
                    MhListRow(
                        title = motoHubText("Installed version"),
                        trailing = { MonoValue("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") }
                    )
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (release.title.isNotBlank() && release.title != release.versionName) {
                            Text(release.title, style = MaterialTheme.typography.titleMedium)
                        }
                        if (release.notes.isNotBlank()) {
                            MhSectionHeader(motoHubText("What's new"))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    releaseNotesAnnotated(release.notes),
                                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        AnimatedVisibility(visible = error != null, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
                            MhBanner(
                                title = motoHubText("Couldn't install the update"),
                                details = {
                                    Text(
                                        lastError.value.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                        when {
                            release.apkAsset == null ->
                                MhBanner(title = motoHubText("This release has no APK to install"), tone = MhTone.WARNING)
                            !canInstallUnknownSources ->
                                MhPrimaryButton(motoHubText("Allow installing updates"), onAllowUnknownSources)
                            else -> {
                                AnimatedVisibility(visible = installing, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
                                    DownloadProgressRow(installingProgress)
                                }
                                MhPrimaryButton(
                                    motoHubText("Download and install"),
                                    loading = installing,
                                    onClick = {
                                        // The one place the metered question is asked, so no caller can
                                        // ship an Install button that skips it. A definitely-metered
                                        // network gets a confirmation; everything else - Wi-Fi, an
                                        // unmetered plan, a network Android will not describe - starts
                                        // the download as before.
                                        networkScope.launch {
                                            val network = withContext(Dispatchers.IO) {
                                                GithubUpdateInstaller.downloadNetwork(context)
                                            }
                                            if (network.metered) {
                                                meteredConfirmation = MeteredDownload(release, network.description)
                                            } else {
                                                onInstall(release)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        MhTextButton(
                            motoHubText("Skip this version"),
                            onClick = { close { onSkip(release) } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !installing
                        )
                    }
                }
            }
        }
    }
}

/** What MhSheet draws for its own title and body, drawn here so it can swap with the content. */
@Composable
private fun SheetHeading(title: String, body: String?) {
    val inset = Modifier.padding(horizontal = 16.dp)
    Text(title, modifier = inset.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
    if (body != null) {
        Text(
            body,
            modifier = inset,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A download waiting on the rider's word because it would be paid for. */
private data class MeteredDownload(val release: GithubRelease, val networkDescription: String)

/**
 * Asks before spending a rider's data allowance on an APK.
 *
 * On a motorcycle the only validated network is usually cellular - which is exactly what makes
 * the updater work at all now that it no longer tries to reach GitHub over the dashboard's Wi-Fi
 * (see GithubUpdateInstaller) - so without this the fix would quietly turn "the update button
 * does nothing" into "the update button costs money", and the second is the worse surprise.
 *
 * The sheet's title and body name the network and the size, because those are the two facts the
 * answer turns on, and neither is guessed: the size comes from the GitHub asset and the network
 * from the same function the download then uses. "Wait for Wi-Fi" is a real answer and costs
 * nothing - the release stays offered, and the same button works on Wi-Fi later without asking.
 */
@Composable
private fun MeteredQuestion(onDownload: () -> Unit, onWait: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MhFootnote(motoHubText("Or wait for Wi-Fi: the update will still be here."))
        MhPrimaryButton(motoHubText("Download anyway"), onDownload, modifier = Modifier.padding(top = 8.dp))
        MhSecondaryButton(motoHubText("Wait for Wi-Fi"), onWait)
    }
}

@Composable
private fun MonoValue(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Compact, dependency-free Markdown presentation for GitHub release notes. */
internal fun releaseNotesAnnotated(markdown: String): AnnotatedString = buildAnnotatedString {
    markdown.lines().forEachIndexed { index, originalLine ->
        val line = originalLine.trimEnd()
        val content = when {
            line.startsWith("### ") -> line.removePrefix("### ")
            line.startsWith("## ") -> line.removePrefix("## ")
            line.startsWith("# ") -> line.removePrefix("# ")
            line.startsWith("- ") || line.startsWith("* ") -> "• ${line.drop(2)}"
            else -> line
        }
        val heading = line.startsWith("#")
        if (heading) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInlineMarkdown(content) }
        } else {
            appendInlineMarkdown(content)
        }
        if (index != markdown.lines().lastIndex) append('\n')
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    val token = Regex("\\*\\*([^*]+)\\*\\*|\\[([^]]+)]\\(([^)]+)\\)")
    var cursor = 0
    token.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val bold = match.groups[1]?.value
        val label = match.groups[2]?.value
        val url = match.groups[3]?.value
        when {
            bold != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            label != null && url != null -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(label) }
                append(" ($url)")
            }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

@Composable
private fun DownloadProgressRow(progress: DownloadProgress?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val fraction = progress?.fraction
        if (fraction != null) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MotoHubColors.Fill
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), trackColor = MotoHubColors.Fill)
        }
        Text(
            downloadStatusText(progress),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun downloadStatusText(progress: DownloadProgress?): String {
    if (progress == null) return motoHubText("Starting download…")
    val downloaded = "%.1f".format(progress.bytesDownloaded / 1_000_000.0)
    val total = progress.totalBytes.takeIf { it > 0 }?.let { "%.1f".format(it / 1_000_000.0) }
    return if (total != null) {
        motoHubText("%1\$s of %2\$s MB", downloaded, total)
    } else {
        motoHubText("%1\$s MB", downloaded)
    }
}
