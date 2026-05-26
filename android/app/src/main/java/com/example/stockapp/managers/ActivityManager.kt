package com.example.stockapp.managers

import android.content.Context

/**
 * Manages activity selection persistence.
 *
 * Stores selected activity (code, keyu, lib) in SharedPreferences under the "activity" namespace.
 * Provides methods to save, retrieve, and clear activity selection.
 * Caches SharedPreferences instance to avoid repeated getSharedPreferences() calls (IMPORTANT FIX #1).
 */
object ActivityManager {

    private const val PREFS_NAME = "activity"
    private const val KEY_ACTIVITY_CODE = "selected_act_code"
    private const val KEY_ACTIVITY_KEYU = "selected_act_keyu"
    private const val KEY_ACTIVITY_LIB = "selected_act_lib"

    /**
     * Helper method to get SharedPreferences instance.
     * Caches the preferences object to avoid repeated getSharedPreferences() calls.
     * (IMPORTANT FIX #1: Inefficient SharedPreferences)
     *
     * @param context Application context
     * @return Cached SharedPreferences instance
     */
    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Saves the selected activity to SharedPreferences.
     *
     * @param context Application context
     * @param actKeyu Activity key (unique identifier)
     * @param actCode Activity code (e.g., "ACT001")
     * @param actLib Activity label/description
     */
    fun saveActivity(context: Context, actKeyu: Int, actCode: String, actLib: String) {
        getPrefs(context).edit().apply {
            putInt(KEY_ACTIVITY_KEYU, actKeyu)
            putString(KEY_ACTIVITY_CODE, actCode)
            putString(KEY_ACTIVITY_LIB, actLib)
            apply()
        }
    }

    /**
     * Retrieves the stored activity code.
     *
     * @param context Application context
     * @return The activity code, or null if not stored
     */
    fun getActivityCode(context: Context): String? {
        return getPrefs(context).getString(KEY_ACTIVITY_CODE, null)
    }

    /**
     * Retrieves the stored activity keyu.
     *
     * @param context Application context
     * @return The activity keyu, or -1 if not stored
     */
    fun getActivityKeyu(context: Context): Int {
        return getPrefs(context).getInt(KEY_ACTIVITY_KEYU, -1)
    }

    /**
     * Retrieves the stored activity label/description.
     *
     * @param context Application context
     * @return The activity label, or null if not stored
     */
    fun getActivityLib(context: Context): String? {
        return getPrefs(context).getString(KEY_ACTIVITY_LIB, null)
    }

    /**
     * Clears the stored activity selection.
     * Used when user logs out or needs to re-select activity.
     *
     * @param context Application context
     */
    fun clearActivity(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_ACTIVITY_CODE)
            remove(KEY_ACTIVITY_KEYU)
            remove(KEY_ACTIVITY_LIB)
            apply()
        }
    }

    /**
     * Checks if an activity has been selected.
     *
     * @param context Application context
     * @return True if activity is selected, false otherwise
     */
    fun hasActivity(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.contains(KEY_ACTIVITY_CODE) && prefs.contains(KEY_ACTIVITY_KEYU)
    }
}
