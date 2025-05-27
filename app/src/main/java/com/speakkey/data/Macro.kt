package com.speakkey.data

import java.util.UUID

data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var isActive: Boolean = false,
    val actions: List<MacroAction> = emptyList()
)
