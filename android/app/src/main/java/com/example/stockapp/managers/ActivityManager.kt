package com.example.stockapp.managers

import android.content.Context

/**
 * Manages activity selection persistence.
 *
 * Stores selected activity (code, keyu, lib) in SharedPreferences under the "activity" namespace.
 * Provides methods to save, retrieve, and clear activity selection.
 */
object ActivityManager {

    private const val PREFS_NAME = "activity"
    private const val KEY_ACTIVITY_CODE = "selected_act_code"
    private const val KEY_ACTIVITY_KEYU = "selected_act_keyu"
    private const val KEY_ACTIVITY_LIB = "selected_act_lib"

    /**
     * Saves the selected activity to SharedPreferences.
     *
     * @param context Application context
     * @param actKeyu Activity key (unique identifier)
     * @param actCode Activity code (e.g., "ACT001")
     * @param actLib Activity label/description
     */
    fun saveActivity(context: Context, actKeyu: Int, actCode: String, actLib: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVITY_CODE, null)
    }

    /**
     * Retrieves the stored activity keyu.
     *
     * @param context Application context
     * @return The activity keyu, or -1 if not stored
     */
    fun getActivityKeyu(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACTIVITY_KEYU, -1)
    }

    /**
     * Retrieves the stored activity label/description.
     *
     * @param context Application context
     * @return The activity label, or null if not stored
     */
    fun getActivityLib(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVITY_LIB, null)
    }

    /**
     * Clears the stored activity selection.
     * Used when user logs out or needs to re-select activity.
     *
     * @param context Application context
     */
    fun clearActivity(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_ACTIVITY_CODE) && prefs.contains(KEY_ACTIVITY_KEYU)
    }
}
