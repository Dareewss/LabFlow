package com.labflow.companion

import android.content.Context

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("labflow_companion", Context.MODE_PRIVATE)

    var host: String
        get() = preferences.getString("host", "") ?: ""
        set(value) = preferences.edit().putString("host", value).apply()

    var port: String
        get() = preferences.getString("port", "8080") ?: "8080"
        set(value) = preferences.edit().putString("port", value).apply()

    var apiKey: String
        get() = preferences.getString("apiKey", "LABFLOW_LOCAL_API_KEY") ?: "LABFLOW_LOCAL_API_KEY"
        set(value) = preferences.edit().putString("apiKey", value).apply()

    var userId: Int
        get() = preferences.getInt("userId", 0)
        set(value) = preferences.edit().putInt("userId", value).apply()

    var username: String
        get() = preferences.getString("username", "") ?: ""
        set(value) = preferences.edit().putString("username", value).apply()

    var fullName: String
        get() = preferences.getString("fullName", "") ?: ""
        set(value) = preferences.edit().putString("fullName", value).apply()

    var role: String
        get() = preferences.getString("role", "") ?: ""
        set(value) = preferences.edit().putString("role", value).apply()

    var currentLabId: Int
        get() = preferences.getInt("currentLabId", 0)
        set(value) = preferences.edit().putInt("currentLabId", value).apply()

    var themePalette: String
        get() = preferences.getString("themePalette", CompanionPalette.RED.name) ?: CompanionPalette.RED.name
        set(value) = preferences.edit().putString("themePalette", value).apply()

    var themeMode: String
        get() = preferences.getString("themeMode", CompanionMode.DARK.name) ?: CompanionMode.DARK.name
        set(value) = preferences.edit().putString("themeMode", value).apply()

    var notificationsEnabled: Boolean
        get() = preferences.getBoolean("notificationsEnabled", true)
        set(value) = preferences.edit().putBoolean("notificationsEnabled", value).apply()

    var appLanguage: String
        get() = (preferences.getString("appLanguage", "en") ?: "en").lowercase()
        set(value) = preferences.edit().putString("appLanguage", value.lowercase()).apply()

    val isLoggedIn: Boolean
        get() = userId > 0 && username.isNotBlank()

    fun paletteEnum(): CompanionPalette {
        return runCatching { CompanionPalette.valueOf(themePalette) }.getOrDefault(CompanionPalette.RED)
    }

    fun modeEnum(): CompanionMode {
        return runCatching { CompanionMode.valueOf(themeMode) }.getOrDefault(CompanionMode.DARK)
    }

    fun authorization(): String = "Bearer $apiKey"

    fun saveUser(user: UserDto) {
        userId = user.id
        username = user.username.orEmpty()
        fullName = user.fullName.orEmpty()
        role = user.role.orEmpty()
    }

    fun clearUser() {
        preferences.edit()
            .remove("userId")
            .remove("username")
            .remove("fullName")
            .remove("role")
            .apply()
    }
}
