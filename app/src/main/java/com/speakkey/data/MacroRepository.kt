package com.speakkey.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.drgraff.speakkey.utils.AppLogManager // Added for logging

class MacroRepository(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("MacroPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private companion object {
        const val TAG = "MacroRepository" // Added for logging
        const val MACROS_KEY = "macros_list"
    private val gson = Gson()

    private companion object {
        const val MACROS_KEY = "macros_list"
        const val MACROS_PER_ROW_KEY = "macros_per_row"
    }

    private fun loadMacros(): MutableList<Macro> {
        val json = sharedPreferences.getString(MACROS_KEY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<Macro>>() {}.type
                return gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Failed to deserialize macros from JSON", e)
                AppLogManager.getInstance().addEntry("ERROR", "$TAG: Failed to deserialize macros. Data might be corrupted.", e.toString())
                return mutableListOf()
            } catch (e: Exception) { // Catch any other unexpected exceptions
                Log.e(TAG, "Unexpected error loading macros", e)
                AppLogManager.getInstance().addEntry("ERROR", "$TAG: Unexpected error loading macros.", e.toString())
                return mutableListOf()
            }
        } else {
            return mutableListOf()
        }
    }

    private fun saveMacros(macros: List<Macro>) {
        try {
            val json = gson.toJson(macros)
            sharedPreferences.edit().putString(MACROS_KEY, json).apply()
        } catch (e: Exception) { // Catching generic Exception, as toJson can also throw various errors
            Log.e(TAG, "Failed to serialize macros to JSON", e)
            AppLogManager.getInstance().addEntry("ERROR", "$TAG: Failed to save macros.", e.toString())
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
        sharedPreferences.edit().putInt(MACROS_PER_ROW_KEY, count).apply()
    }

    fun getMacrosPerRow(defaultVal: Int = 1): Int {
        return sharedPreferences.getInt(MACROS_PER_ROW_KEY, defaultVal)
    }
}
