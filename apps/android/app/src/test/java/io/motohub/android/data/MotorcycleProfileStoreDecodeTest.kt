// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.data

import javax.crypto.AEADBadTagException
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * One unreadable entry used to make loadAll() clear() the whole store: every motorcycle, its
 * password, photo and the active id, gone on a single Keystore hiccup.
 */
class MotorcycleProfileStoreDecodeTest {
    private val stored = JSONArray(
        """
        [
          {"id":"a","ssid":"BIKE-A","password_iv":"iv-a","password_ciphertext":"ok",
           "photo_path":"/photos/a.jpg"},
          {"id":"b","ssid":"BIKE-B","password_iv":"iv-b","password_ciphertext":"bad"},
          "not a profile",
          {"id":"c","ssid":"BIKE-C"}
        ]
        """
    )

    @Test
    fun `a password that will not decrypt costs that password, not the motorcycles`() {
        val logged = mutableListOf<String>()
        val profiles = MotorcycleProfileStore.decodeProfiles(
            stored,
            decrypt = { _, ciphertext ->
                if (ciphertext == "bad") throw AEADBadTagException() else "secret"
            },
            logUnreadable = { message, _ -> logged += message }
        )

        assertEquals(listOf("a", "b", "c"), profiles.map { it.id })
        assertEquals(listOf("secret", "", ""), profiles.map { it.password })
        assertEquals("/photos/a.jpg", profiles.first().photoPath)
        assertEquals("bad password, missing password, non-object entry", 3, logged.size)
    }
}
