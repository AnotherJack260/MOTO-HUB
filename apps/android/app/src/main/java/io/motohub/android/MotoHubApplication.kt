// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android

import android.app.Application
import android.app.UiModeManager
import io.motohub.android.i18n.MotoHubStrings
import io.motohub.android.session.CrashRecovery
import io.motohub.android.session.ProcessExitReport
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.session.SentryIntegration

class MotoHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // The app is dark whatever the phone is set to; tell the platform once. Every window
        // drawn for us then picks light system-bar icons too - ModalBottomSheet sets its own
        // window's icons from isSystemInDarkTheme(), and on a light-mode phone every sheet turned
        // the clock and gesture handle dark. Persisted per app, so after the first launch this
        // is a no-op.
        getSystemService(UiModeManager::class.java).setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
        MotoHubStrings.initialize(this)
        SentryIntegration.initialize(this)
        ProjectionEventLog.initialize(this)
        CrashRecovery.install(this)
        CrashRecovery.restorePreviousCrash(this)
        // After the crash report, and covering what it cannot: a process that was killed rather
        // than crashed leaves no exception behind, so Android's own record is the only account
        // of it. Both run at startup because the two answer different halves of "what happened
        // last time".
        ProcessExitReport.reportPreviousExits(this)
    }
}
