package com.redbus.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.redbus.ai.data.model.User

object SessionManager {
    private const val PREF_NAME = "redbus_ai_cross_platform_prefs"
    private const val KEY_TOKEN = "redbus_token"
    private const val KEY_USER = "redbus_user"
    private const val KEY_DARK_MODE = "redbus_dark_mode"
    private const val KEY_LANGUAGE = "redbus_selected_language"
    private const val KEY_READ_NOTIFS = "redbus_read_notifications"
    private const val KEY_DISMISSED_NOTIFS = "redbus_dismissed_notifications"
    private const val KEY_REVIEWED_PNRS = "redbus_reviewed_pnrs"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveSession(token: String, user: User) {
        prefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putString(KEY_USER, gson.toJson(user))
            ?.apply()
    }

    fun saveRawWebSession(token: String, userJson: String) {
        prefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putString(KEY_USER, userJson)
            ?.apply()
    }

    fun updateUser(user: User) {
        prefs?.edit()
            ?.putString(KEY_USER, gson.toJson(user))
            ?.apply()
    }

    fun getToken(): String? {
        return prefs?.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun getUser(): User? {
        val raw = prefs?.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(raw, User::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun getUserJson(): String? {
        return prefs?.getString(KEY_USER, null)
    }

    fun clearSession() {
        prefs?.edit()
            ?.remove(KEY_TOKEN)
            ?.remove(KEY_USER)
            ?.apply()
    }

    fun isDarkMode(): Boolean {
        return prefs?.getBoolean(KEY_DARK_MODE, false) ?: false
    }

    fun setDarkMode(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, enabled)?.apply()
    }

    fun getLanguage(): String {
        return prefs?.getString(KEY_LANGUAGE, "English") ?: "English"
    }

    fun setLanguage(lang: String) {
        prefs?.edit()?.putString(KEY_LANGUAGE, lang)?.apply()
    }

    fun getReadNotificationIds(): Set<String> {
        return prefs?.getStringSet(KEY_READ_NOTIFS, emptySet()) ?: emptySet()
    }

    fun saveReadNotificationIds(ids: Set<String>) {
        prefs?.edit()?.putStringSet(KEY_READ_NOTIFS, ids)?.apply()
    }

    fun getDismissedNotificationIds(): Set<String> {
        return prefs?.getStringSet(KEY_DISMISSED_NOTIFS, emptySet()) ?: emptySet()
    }

    fun saveDismissedNotificationIds(ids: Set<String>) {
        prefs?.edit()?.putStringSet(KEY_DISMISSED_NOTIFS, ids)?.apply()
    }

    fun getReviewedPnrs(): Map<String, Int> {
        val raw = prefs?.getString(KEY_REVIEWED_PNRS, null) ?: return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(raw, type) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveReviewedPnr(pnr: String, rating: Int) {
        val current = getReviewedPnrs().toMutableMap()
        current[pnr] = rating
        prefs?.edit()?.putString(KEY_REVIEWED_PNRS, gson.toJson(current))?.apply()
    }
}

