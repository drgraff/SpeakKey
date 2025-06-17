package com.speakkey.ui.macros

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem // Added for onOptionsItemSelected
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drgraff.speakkey.R // Ensure this is the correct R
import com.speakkey.data.ActionType
import com.speakkey.data.Macro
import com.speakkey.data.MacroAction
import com.speakkey.data.MacroRepository
import com.drgraff.speakkey.utils.DynamicThemeApplicator
import com.drgraff.speakkey.utils.ThemeManager
import com.google.android.material.textfield.TextInputEditText
import android.content.res.ColorStateList // Added for ColorStateList
import androidx.core.widget.CompoundButtonCompat // For potential future checkbox styling
import com.google.android.material.button.MaterialButton // For casting if needed


import java.util.Collections
import java.util.UUID

class MacroEditorActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        const val EXTRA_MACRO_ID = "extra_macro_id"
    }

    private lateinit var macroRepository: MacroRepository
    private lateinit var actionsAdapter: MacroActionsAdapter

    private lateinit var editMacroName: TextInputEditText
    private lateinit var actionsRecyclerView: RecyclerView
    private lateinit var emptyActionsTextView: TextView
    private lateinit var btnSaveMacro: Button
    private lateinit var btnCancelMacro: Button

    private var currentMacro: Macro? = null
    private val currentActions = mutableListOf<MacroAction>()

    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "MacroEditorActivity"

    // Member variables for theme state tracking
    private var mAppliedThemeMode: String? = null
    private var mAppliedTopbarBackgroundColor: Int = 0
    private var mAppliedTopbarTextIconColor: Int = 0
    private var mAppliedMainBackgroundColor: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        ThemeManager.applyTheme(this.sharedPreferences)
        val themeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)
        if (ThemeManager.THEME_OLED == themeValue) {
            setTheme(R.style.AppTheme_OLED)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_macro) // Ensure this layout uses R.id.toolbar

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar) // Standardized ID
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Title is set later

        // Apply custom OLED colors if OLED theme is active
        val currentActivityThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)
        if (ThemeManager.THEME_OLED == currentActivityThemeValue) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences)
            Log.d(TAG, "MacroEditorActivity: Applied dynamic OLED colors for window/toolbar.")

            // Retrieve common colors
            val buttonBackgroundColor = this.sharedPreferences.getInt(
                "pref_oled_button_background",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND
            )
            val buttonTextIconColor = this.sharedPreferences.getInt(
                "pref_oled_button_text_icon",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
            )
            val textboxBackgroundColor = this.sharedPreferences.getInt(
                "pref_oled_textbox_background",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_TEXTBOX_BACKGROUND
            )
            // val accentGeneralColor = this.sharedPreferences.getInt( // Not used for these specific elements yet
            //     "pref_oled_accent_general",
            //     com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL
            // )

            // Style Macro Name EditText
            if (::editMacroName.isInitialized && editMacroName != null) {
                editMacroName.setBackgroundColor(textboxBackgroundColor)
                Log.d(TAG, String.format("MacroEditorActivity: Styled editMacroName BG: 0x%08X", textboxBackgroundColor))
            }

            // Style Save Button (MaterialButton)
            if (::btnSaveMacro.isInitialized && btnSaveMacro != null) {
                btnSaveMacro.backgroundTintList = ColorStateList.valueOf(buttonBackgroundColor)
                btnSaveMacro.setTextColor(buttonTextIconColor)
                Log.d(TAG, String.format("MacroEditorActivity: Styled btnSaveMacro with BG=0x%08X, Text=0x%08X", buttonBackgroundColor, buttonTextIconColor))
            }

            // Style Cancel Button (Material TextButton)
            if (::btnCancelMacro.isInitialized && btnCancelMacro != null) {
                btnCancelMacro.setTextColor(buttonBackgroundColor)
                Log.d(TAG, String.format("MacroEditorActivity: Styled btnCancelMacro TextColor: 0x%08X", buttonBackgroundColor))
            }

            // Style "Add Action" Buttons (Outlined Buttons)
            val addActionButtons = arrayOf(
                findViewById<Button>(R.id.btn_add_text_action),
                findViewById<Button>(R.id.btn_add_special_key_action),
                findViewById<Button>(R.id.btn_add_tab_action),
                findViewById<Button>(R.id.btn_add_enter_action),
                findViewById<Button>(R.id.btn_add_delay_action),
                findViewById<Button>(R.id.btn_add_pause_action)
            )
            val addActionButtonNames = arrayOf(
                "btn_add_text_action", "btn_add_special_key_action", "btn_add_tab_action",
                "btn_add_enter_action", "btn_add_delay_action", "btn_add_pause_action"
            )

            for (i in addActionButtons.indices) {
                val button = addActionButtons[i]
                val buttonName = addActionButtonNames[i]
                if (button != null) {
                    if (button is MaterialButton) {
                        button.setTextColor(buttonBackgroundColor)
                        button.strokeColor = ColorStateList.valueOf(buttonBackgroundColor)
                        Log.d(TAG, String.format("MacroEditorActivity: Styled %s (MaterialOutlined) with Text/Stroke=0x%08X", buttonName, buttonBackgroundColor))
                    } else {
                        button.setTextColor(buttonBackgroundColor)
                        Log.d(TAG, String.format("MacroEditorActivity: Styled %s (Button) with Text=0x%08X", buttonName, buttonBackgroundColor))
                    }
                } else {
                    Log.w(TAG, "MacroEditorActivity: Add Action button " + buttonName + " is null, cannot style.")
                }
            }
        }

        macroRepository = MacroRepository(applicationContext)

        editMacroName = findViewById(R.id.edit_macro_name)
        actionsRecyclerView = findViewById(R.id.macro_actions_recycler_view)
        emptyActionsTextView = findViewById(R.id.empty_actions_text_view)
        btnSaveMacro = findViewById(R.id.btn_save_macro)
        btnCancelMacro = findViewById(R.id.btn_cancel_macro_edit)

        setupActionButtons()
        setupRecyclerView()

        val macroId = intent.getStringExtra(EXTRA_MACRO_ID)
        if (macroId != null) {
            currentMacro = macroRepository.getMacro(macroId)
            currentMacro?.let {
                editMacroName.setText(it.name)
                currentActions.addAll(it.actions)
                supportActionBar?.title = getString(R.string.title_edit_macro)
            }
        } else {
            supportActionBar?.title = getString(R.string.title_create_macro)
        }
        updateActionsEmptyState()

        btnSaveMacro.setOnClickListener { saveMacro() }
        btnCancelMacro.setOnClickListener { finish() }

        // Store applied theme state
        this.mAppliedThemeMode = currentActivityThemeValue
        if (ThemeManager.THEME_OLED == currentActivityThemeValue) {
            this.mAppliedTopbarBackgroundColor = sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND)
            this.mAppliedTopbarTextIconColor = sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON)
            this.mAppliedMainBackgroundColor = sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND)
            Log.d(TAG, "MacroEditorActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor))
        } else {
            this.mAppliedTopbarBackgroundColor = 0
            this.mAppliedTopbarTextIconColor = 0
            this.mAppliedMainBackgroundColor = 0
            Log.d(TAG, "MacroEditorActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.")
        }
    }

    private fun setupRecyclerView() {
        actionsAdapter = MacroActionsAdapter(
            actions = currentActions,
            onDeleteClick = { action ->
                val index = currentActions.indexOf(action)
                if (index != -1) {
                    val removedActionName = currentActions[index].getDisplayName()
                    currentActions.removeAt(index)
                    actionsAdapter.notifyItemRemoved(index)
                    actionsAdapter.notifyItemRangeChanged(index, currentActions.size - index)
                    updateActionsEmptyState()
                    Toast.makeText(this@MacroEditorActivity, "Action removed: $removedActionName", Toast.LENGTH_SHORT).show()
                }
            },
            onMoveUp = { position ->
                if (position > 0) {
                    Collections.swap(currentActions, position, position - 1)
                    actionsAdapter.notifyItemMoved(position, position - 1)
                    Toast.makeText(this@MacroEditorActivity, "Action moved up", Toast.LENGTH_SHORT).show()
                }
            },
            onMoveDown = { position ->
                if (position < currentActions.size - 1) {
                    Collections.swap(currentActions, position, position + 1)
                    actionsAdapter.notifyItemMoved(position, position + 1)
                    Toast.makeText(this@MacroEditorActivity, "Action moved down", Toast.LENGTH_SHORT).show()
                }
            }
        )
        actionsRecyclerView.layoutManager = LinearLayoutManager(this)
        actionsRecyclerView.adapter = actionsAdapter
        actionsAdapter.submitList(currentActions)

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
                    if (fromPosition != toPosition) {
                        Collections.swap(currentActions, fromPosition, toPosition)
                        actionsAdapter.notifyItemMoved(fromPosition, toPosition)
                        Toast.makeText(this@MacroEditorActivity, "Action reordered", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(actionsRecyclerView)
    }

    private fun setupActionButtons() {
        findViewById<Button>(R.id.btn_add_text_action).setOnClickListener {
            showInputDialog("Add Text Action", "Enter text to type:") { value ->
                addAction(ActionType.TEXT, value = value)
            }
        }
        findViewById<Button>(R.id.btn_add_special_key_action).setOnClickListener {
             showInputDialog("Add Special Key", "Enter key (e.g., CTRL, ALT+SHIFT, CTRL+C):") { value ->
                addAction(ActionType.SPECIAL_KEY, value = value)
            }
        }
        findViewById<Button>(R.id.btn_add_tab_action).setOnClickListener { addAction(ActionType.TAB) }
        findViewById<Button>(R.id.btn_add_enter_action).setOnClickListener { addAction(ActionType.ENTER) }
        findViewById<Button>(R.id.btn_add_delay_action).setOnClickListener {
            showInputDialog("Add Delay Action", "Enter delay in milliseconds:", inputType = InputType.TYPE_CLASS_NUMBER) { value ->
                val delayMs = value.toIntOrNull()
                if (delayMs != null && delayMs > 0) {
                    addAction(ActionType.DELAY, delayMillis = delayMs)
                } else {
                    Toast.makeText(this@MacroEditorActivity, "Invalid delay value", Toast.LENGTH_SHORT).show()
                }
            }
        }
        findViewById<Button>(R.id.btn_add_pause_action).setOnClickListener { addAction(ActionType.PAUSE_CONFIRMATION) }
    }

    private fun addAction(type: ActionType, value: String? = null, delayMillis: Int? = null) {
        val action = MacroAction(type = type, value = value, delayMillis = delayMillis)
        currentActions.add(action)
        actionsAdapter.notifyItemInserted(currentActions.size - 1)
        updateActionsEmptyState()
        Toast.makeText(this@MacroEditorActivity, "Action added: ${action.getDisplayName()}", Toast.LENGTH_SHORT).show()
    }

    private fun updateActionsEmptyState() {
        if (currentActions.isEmpty()) {
            actionsRecyclerView.visibility = View.GONE
            emptyActionsTextView.visibility = View.VISIBLE
        } else {
            actionsRecyclerView.visibility = View.VISIBLE
            emptyActionsTextView.visibility = View.GONE
        }
    }

    private fun showInputDialog(title: String, message: String, inputType: Int = InputType.TYPE_CLASS_TEXT, onConfirm: (String) -> Unit) {
        val editText = EditText(this).apply { this.inputType = inputType }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                onConfirm(editText.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveMacro() {
        val name = editMacroName.text.toString().trim()
        if (name.isEmpty()) {
            editMacroName.error = "Macro name cannot be empty"
            return
        }
        if (currentActions.isEmpty()) {
            Toast.makeText(this@MacroEditorActivity, "Macro must have at least one action", Toast.LENGTH_SHORT).show()
            return
        }
        val macroToSave = currentMacro?.copy(
            name = name,
            actions = currentActions.toList()
        ) ?: Macro(
            id = UUID.randomUUID().toString(),
            name = name,
            isActive = false,
            actions = currentActions.toList()
        )
        if (currentMacro == null) {
            macroRepository.addMacro(macroToSave)
            Toast.makeText(this@MacroEditorActivity, "Macro saved: ${macroToSave.name}", Toast.LENGTH_SHORT).show()
        } else {
            macroRepository.updateMacro(macroToSave)
            Toast.makeText(this@MacroEditorActivity, "Macro updated: ${macroToSave.name}", Toast.LENGTH_SHORT).show()
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (mAppliedThemeMode != null) {
            var needsRecreate = false
            val currentThemeValue = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)
            if (mAppliedThemeMode != currentThemeValue) {
                needsRecreate = true
                Log.d(TAG, "onResume: Theme mode changed. OldMode=" + mAppliedThemeMode + ", NewMode=" + currentThemeValue)
            } else if (ThemeManager.THEME_OLED == currentThemeValue) {
                if (mAppliedTopbarBackgroundColor != sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND)) needsRecreate = true
                if (mAppliedTopbarTextIconColor != sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON)) needsRecreate = true
                if (mAppliedMainBackgroundColor != sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND)) needsRecreate = true
                if (needsRecreate) {
                     Log.d(TAG, "onResume: OLED color(s) changed for MacroEditorActivity.")
                }
            }
            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating MacroEditorActivity.")
                recreate()
                return
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged triggered for key: " + key)
        if (key == null) return
        val oledColorKeys = arrayOf(
            "pref_oled_topbar_background", "pref_oled_topbar_text_icon",
            "pref_oled_main_background", "pref_oled_surface_background",
            "pref_oled_general_text_primary", "pref_oled_general_text_secondary",
            "pref_oled_button_background", "pref_oled_button_text_icon",
            "pref_oled_textbox_background", "pref_oled_textbox_accent",
            "pref_oled_accent_general"
        )
        var isOledColorKey = false
        for (oledKey in oledColorKeys) {
            if (oledKey == key) {
                isOledColorKey = true
                break
            }
        }
        if (ThemeManager.PREF_KEY_DARK_MODE == key) {
            Log.d(TAG, "Main theme preference changed. Recreating MacroEditorActivity.")
            recreate()
        } else if (isOledColorKey) {
            val currentTheme = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)
            if (ThemeManager.THEME_OLED == currentTheme) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating MacroEditorActivity.")
                recreate()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { // Added for back navigation
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class MacroActionsAdapter(
        private val actions: MutableList<MacroAction>,
        private val onDeleteClick: (MacroAction) -> Unit,
        private val onMoveUp: (Int) -> Unit,
        private val onMoveDown: (Int) -> Unit
    ) : RecyclerView.Adapter<MacroActionsAdapter.ActionViewHolder>() {
        fun submitList(newActions: List<MacroAction>) {
            actions.clear()
            actions.addAll(newActions)
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_macro_action, parent, false)
            return ActionViewHolder(view)
        }
        override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
            holder.bind(actions[position], position)
        }
        override fun getItemCount(): Int = actions.size
        inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val descriptionTextView: TextView = itemView.findViewById(R.id.action_description_text_view)
            private val deleteButton: ImageButton = itemView.findViewById(R.id.action_delete_button)
            private val moveUpButton: ImageButton = itemView.findViewById(R.id.action_move_up_button)
            private val moveDownButton: ImageButton = itemView.findViewById(R.id.action_move_down_button)
            fun bind(action: MacroAction, position: Int) {
                descriptionTextView.text = action.getDisplayName()
                deleteButton.setOnClickListener { onDeleteClick(action) }
                moveUpButton.setOnClickListener { onMoveUp(adapterPosition) }
                moveDownButton.setOnClickListener { onMoveDown(adapterPosition) }
                moveUpButton.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
                moveDownButton.visibility = if (position == actions.size - 1) View.INVISIBLE else View.VISIBLE
            }
        }
    }
}
