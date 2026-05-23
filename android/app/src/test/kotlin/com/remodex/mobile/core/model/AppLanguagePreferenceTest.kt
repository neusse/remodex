package com.remodex.mobile.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguagePreferenceTest {
    @Test
    fun defaultPreferenceFollowsSystemLanguage() {
        assertEquals(AppLanguagePreference.system, AppLanguagePreference.default)
        assertEquals(AppLanguagePreference.system, AppLanguagePreference.fromStorage(null))
        assertEquals(AppLanguagePreference.system, AppLanguagePreference.fromStorage(""))
        assertEquals(AppLanguagePreference.system, AppLanguagePreference.fromStorage("unknown"))
    }

    @Test
    fun storedPreferenceIsRestoredWhenValid() {
        assertEquals(AppLanguagePreference.english, AppLanguagePreference.fromStorage("english"))
        assertEquals(AppLanguagePreference.system, AppLanguagePreference.fromStorage("system"))
    }
}
