package com.speakkey.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.drgraff.speakkey.utils.AppLogManager

class MacroRepository(context: Context) { // Removed 'private val' from context to use it only for init

    // Instance properties
    private val sharedPreferences = context.applicationContext.getSharedPreferences(MACROS_PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object { // Single companion object
        private const val TAG = "MacroRepository"
        private const val MACROS_PREFS_NAME = "MacroPrefs" // Centralized SharedPreferences name
        private const val MACROS_KEY = "macros_list"
        private const val MACROS_PER_ROW_KEY = "macros_per_row"
    }

    private fun loadMacros(): MutableList<Macro> {
        val json = sharedPreferences.getString(MACROS_KEY, null)
        return if (json != null) {
            try {
                // Explicitly define the type for deserialization
                val type = object : TypeToken<MutableList<Macro>>() {}.type
                // Use gson.fromJson<MutableList<Macro>>(json, type) if type inference is ambiguous,
                // but typically gson.fromJson(json, type) works.
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Failed to deserialize macros from JSON. Data might be corrupted.", e)
                AppLogManager.getInstance().addEntry("ERROR", "$TAG: Failed to deserialize macros. Data might be corrupted. Details: ${e.message}", null)
                return mutableListOf()
            } catch (e: Exception) { // Catch any other unexpected exceptions
                Log.e(TAG, "Unexpected error loading macros", e)
                AppLogManager.getInstance().addEntry("ERROR", "$TAG: Unexpected error loading macros. Details: ${e.message}", null)
                return mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

    private fun saveMacros(macros: List<Macro>) {
        try {
            val json = gson.toJson(macros)
            sharedPreferences.edit().putString(MACROS_KEY, json).apply()
        } catch (e: Exception) { // Catching generic Exception, as toJson can also throw various errors
            Log.e(TAG, "Failed to serialize macros to JSON", e)
            AppLogManager.getInstance().addEntry("ERROR", "$TAG: Failed to save macros. Details: ${e.message}", null)
        }
    }

    fun addMacro(macro: Macro) {
        val macros = loadMacros()
        if (macros.none { it.id == macro.id }) {
            macros.add(macro)
            saveMacros(macros)
        }
    }

    fun updateMacro(macro: Macro) {
        val macros = loadMacros()
        val index = macros.indexOfFirst { it.id == macro.id }
        if (index != -1) {
            macros[index] = macro
            saveMacros(macros)
        }
    }

    fun deleteMacro(macroId: String) {
        val macros = loadMacros()
        macros.removeAll { it.id == macroId }
        saveMacros(macros)
    }

    fun getMacro(macroId: String): Macro? {
        return loadMacros().find { it.id == macroId }
    }

    fun getAllMacros(): List<Macro> {
        return loadMacros()
    }

    fun getActiveMacros(): List<Macro> {
        return loadMacros().filter { it.isActive }
    }

    fun setMacrosPerRow(count: Int) {
        if (count > 0) {
            sharedPreferences.edit().putInt(MACROS_PER_ROW_KEY, count).apply()
        } else {
            Log.w(TAG, "Attempted to set macrosPerRow to an invalid count: $count. Ignoring.")
            AppLogManager.getInstance().addEntry("WARN", "$TAG: Invalid macrosPerRow count: $count. Not saved.", null)
        }
    }

    fun getMacrosPerRow(defaultVal: Int = 1): Int {
        val storedVal = sharedPreferences.getInt(MACROS_PER_ROW_KEY, defaultVal)
        return if (storedVal > 0) storedVal else defaultVal // Ensure a positive value is returned
    }
}
