package com.userndot.sdk.android

import android.content.Context
import android.content.SharedPreferences

class StorageHelper {
    companion object {
        fun putString(context: Context, key: String, value: String) {
            val prefs = getPreferences(context)
            val editor = prefs.edit().putString(key, value)
            persist(editor)
        }

        fun getString(context: Context, key: String, defaultValue: String): String? {
            return getPreferences(context).getString(key, defaultValue)
        }

        fun getString(context: Context, nameSpace: String, key: String, defaultValue: String): String? {
            return getPreferences(context, nameSpace).getString(key, defaultValue)
        }

        fun putLong(context: Context, key: String, value: Long) {
            val prefs = getPreferences(context)
            val editor = prefs.edit().putLong(key, value)
            persist(editor)
        }

        fun getLong(context: Context, key: String, defaultValue: Long): Long {
            return getPreferences(context).getLong(key, defaultValue)
        }

        fun getLong(context: Context, nameSpace: String, key: String, defaultValue: Long): Long {
            return getPreferences(context, nameSpace).getLong(key, defaultValue)
        }

        fun putInt(context: Context, key: String, value: Int) {
            val prefs = getPreferences(context)
            val editor = prefs.edit().putInt(key, value)
            persist(editor)
        }


        fun getInt(context: Context, key: String, defaultValue: Int): Int {
            return getPreferences(context).getInt(key, defaultValue)
        }

        fun putBoolean(context: Context, key: String, value: Boolean) {
            val prefs = getPreferences(context)
            val editor = prefs.edit().putBoolean(key, value)
            persist(editor)
        }

        fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
            return getPreferences(context).getBoolean(key, defaultValue)
        }

        fun getPreferences(context: Context, namespace: String?): SharedPreferences {
            var path = Constants.USERNDOT_STORAGE_TAG

            if (namespace != null) {
                path += "_$namespace"
            }
            return context.getSharedPreferences(path, Context.MODE_PRIVATE)
        }

        fun getPreferences(context: Context): SharedPreferences {
            return getPreferences(context, null)
        }

        fun persist(editor: SharedPreferences.Editor) {
            try {
                editor.apply()
            } catch (t: Throwable) {
//                Logger.v("CRITICAL: Failed to persist shared preferences!", t)
            }

        }
    }

}