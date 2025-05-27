package com.speakkey.data

import android.view.KeyEvent

enum class ActionType {
    TEXT,          // For typing regular text
    SPECIAL_KEY,   // For special keys like CTRL, ALT, SHIFT, or combinations like CTRL+C
    TAB,           // For pressing the TAB key
    ENTER,         // For pressing the ENTER key
    DELAY,         // For introducing a delay between actions
    PAUSE_CONFIRMATION // For pausing execution and waiting for user confirmation (optional)
}

data class MacroAction(
    val type: ActionType,
    val value: String? = null,      // Content for TEXT, or key identifier (e.g., "CTRL", "ALT+SHIFT") for SPECIAL_KEY
    val keyCode: Int? = null,       // Android KeyEvent constant (e.g., KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_ENTER)
    val delayMillis: Int? = null,   // Duration for DELAY action
    val actionName: String? = null  // User-friendly display name (e.g., "Type: Hello", "Press: Tab", "Delay: 500ms")
) {
    // Secondary constructor or helper function could be added here for easier creation if needed,
    // for example, to auto-generate actionName based on type and value.

    // Example of how actionName could be generated if not provided:
    fun getDisplayName(): String {
        if (actionName != null) return actionName // User-defined name takes precedence

        val MAX_VALUE_LENGTH = 20 // Max length for displaying value in TEXT type

        return when (type) {
            ActionType.TEXT -> {
                val textValue = value ?: ""
                val displayValue = if (textValue.length > MAX_VALUE_LENGTH) {
                    textValue.substring(0, MAX_VALUE_LENGTH) + "..."
                } else {
                    textValue
                }
                "Type: \"$displayValue\"" // Added quotes for clarity
            }
            ActionType.SPECIAL_KEY -> "Key: ${value ?: "Not set"}"
            ActionType.TAB -> "Press: Tab"
            ActionType.ENTER -> "Press: Enter"
            ActionType.DELAY -> "Delay: ${delayMillis ?: "0"}ms"
            ActionType.PAUSE_CONFIRMATION -> "Pause & Confirm"
        }
    }
}
