package com.speakkey.ui.macros

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.PorterDuff // Keep for original toolbar tint if needed, though DynamicThemeApplicator handles it
import android.os.Bundle
import androidx.core.graphics.ColorUtils
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar // Explicit import
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drgraff.speakkey.R // Main app R class
import com.drgraff.speakkey.utils.DynamicThemeApplicator
import com.drgraff.speakkey.utils.ThemeManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.speakkey.data.Macro
import com.speakkey.data.MacroRepository
import kotlinx.coroutines.launch // For lifecycleScope

class MacroListActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var macroRepository: MacroRepository
    private lateinit var macrosAdapter: MacrosAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyMacrosTextView: TextView
    private lateinit var fabAddMacro: FloatingActionButton

    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "MacroListActivity"

    private var mAppliedThemeMode: String? = null
    private var mAppliedTopbarBackgroundColor: Int = 0
    private var mAppliedTopbarTextIconColor: Int = 0
    private var mAppliedMainBackgroundColor: Int = 0
    private var mAppliedAccentGeneralColor: Int = 0
    private var mAppliedButtonTextIconColor: Int = 0

    private val editMacroResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadMacros()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        ThemeManager.applyTheme(sharedPreferences)
        val themeValue = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)
        if (ThemeManager.THEME_OLED == themeValue) {
            setTheme(R.style.AppTheme_OLED)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_macros)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.title_activity_macro_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        macroRepository = MacroRepository(applicationContext)
        recyclerView = findViewById(R.id.macros_recycler_view)
        emptyMacrosTextView = findViewById(R.id.empty_macros_text_view)
        fabAddMacro = findViewById(R.id.fab_add_macro)

        val currentActivityThemeValue = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)
        if (ThemeManager.THEME_OLED == currentActivityThemeValue) {
            DynamicThemeApplicator.applyOledColors(this, sharedPreferences)
            Log.d(TAG, "MacroListActivity: Applied dynamic OLED colors for window/toolbar.")

            val accentGeneralColor = sharedPreferences.getInt("pref_oled_accent_general", DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL)
            val buttonTextIconColor = sharedPreferences.getInt("pref_oled_button_text_icon", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON)

            fabAddMacro.backgroundTintList = ColorStateList.valueOf(accentGeneralColor)
            fabAddMacro.imageTintList = ColorStateList.valueOf(buttonTextIconColor)
            Log.d(TAG, String.format("MacroListActivity: Styled fabAddMacro with BG=0x%08X, IconTint=0x%08X", accentGeneralColor, buttonTextIconColor))

            mAppliedAccentGeneralColor = accentGeneralColor
            mAppliedButtonTextIconColor = buttonTextIconColor
        }

        setupRecyclerView()
        loadMacros()

        fabAddMacro.setOnClickListener {
            val intent = Intent(this, MacroEditorActivity::class.java)
            editMacroResultLauncher.launch(intent)
        }

        this.mAppliedThemeMode = currentActivityThemeValue
        if (ThemeManager.THEME_OLED == currentActivityThemeValue) {
            this.mAppliedTopbarBackgroundColor = sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND)
            this.mAppliedTopbarTextIconColor = sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON)
            this.mAppliedMainBackgroundColor = sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND)
            // mAppliedAccentGeneralColor and mAppliedButtonTextIconColor are already set above if in OLED mode
            Log.d(TAG, "onCreate: Stored mAppliedThemeMode=$mAppliedThemeMode, TopbarBG=0x${Integer.toHexString(mAppliedTopbarBackgroundColor)}, AccentGeneral=0x${Integer.toHexString(mAppliedAccentGeneralColor)}")
        } else {
            this.mAppliedTopbarBackgroundColor = 0
            this.mAppliedTopbarTextIconColor = 0
            this.mAppliedMainBackgroundColor = 0
            this.mAppliedAccentGeneralColor = 0
            this.mAppliedButtonTextIconColor = 0
            Log.d(TAG, "onCreate: Stored mAppliedThemeMode=$mAppliedThemeMode. Not OLED mode.")
        }
    }

    override fun onResume() {
        super.onResume()
        if (mAppliedThemeMode != null) {
            var needsRecreate = false
            val currentThemeValue = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)

            if (mAppliedThemeMode != currentThemeValue) {
                needsRecreate = true
                Log.d(TAG, "onResume: Theme mode changed. OldMode=$mAppliedThemeMode, NewMode=$currentThemeValue")
            } else if (ThemeManager.THEME_OLED == currentThemeValue) {
                if (mAppliedTopbarBackgroundColor != sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND)) needsRecreate = true
                if (mAppliedTopbarTextIconColor != sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON)) needsRecreate = true
                if (mAppliedMainBackgroundColor != sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND)) needsRecreate = true
                if (mAppliedAccentGeneralColor != sharedPreferences.getInt("pref_oled_accent_general", DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL)) needsRecreate = true
                if (mAppliedButtonTextIconColor != sharedPreferences.getInt("pref_oled_button_text_icon", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON)) needsRecreate = true

                if (needsRecreate) {
                     Log.d(TAG, "onResume: OLED color(s) changed for MacroListActivity.")
                }
            }

            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating MacroListActivity.")
                recreate()
                return
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        loadMacros()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged triggered for key: $key")
        if (key == null) return

        val oledColorKeys = arrayOf(
            "pref_oled_topbar_background", "pref_oled_topbar_text_icon",
            "pref_oled_main_background", "pref_oled_surface_background",
            "pref_oled_general_text_primary", "pref_oled_general_text_secondary",
            "pref_oled_button_background", "pref_oled_button_text_icon",
            "pref_oled_textbox_background", "pref_oled_textbox_accent",
            "pref_oled_accent_general"
        )
        val isOledColorKey = oledColorKeys.contains(key)

        if (ThemeManager.PREF_KEY_DARK_MODE == key) {
            Log.d(TAG, "Main theme preference changed. Recreating MacroListActivity.")
            recreate()
        } else if (isOledColorKey) {
            val currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)
            if (ThemeManager.THEME_OLED == currentTheme) {
                Log.d(TAG, "OLED color preference changed: $key. Recreating MacroListActivity.")
                recreate()
            }
        }
    }

    private fun setupRecyclerView() {
        macrosAdapter = MacrosAdapter(
            onEditClick = { macro ->
                val intent = Intent(this, MacroEditorActivity::class.java).apply {
                    putExtra(MacroEditorActivity.EXTRA_MACRO_ID, macro.id)
                }
                editMacroResultLauncher.launch(intent)
            },
            onDeleteClick = { macro ->
                showDeleteConfirmationDialog(macro)
            },
            onActiveChanged = { macro, isActive ->
                val updatedMacro = macro.copy(isActive = isActive)
                macroRepository.updateMacro(updatedMacro)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = macrosAdapter
    }

    private fun loadMacros() {
        val macros = macroRepository.getAllMacros()
        macrosAdapter.submitList(macros)
        updateMacrosEmptyState(macros.isEmpty())
    }

    private fun updateMacrosEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            emptyMacrosTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyMacrosTextView.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmationDialog(macro: Macro) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_macro_confirmation_title))
            .setMessage(getString(R.string.delete_macro_confirmation_message, macro.name))
            .setPositiveButton(getString(R.string.delete_action)) { dialog, _ ->
                macroRepository.deleteMacro(macro.id)
                loadMacros()
                Toast.makeText(this@MacroListActivity, "Macro '${macro.name}' deleted", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    inner class MacrosAdapter(
        private val onEditClick: (Macro) -> Unit,
        private val onDeleteClick: (Macro) -> Unit,
        private val onActiveChanged: (Macro, Boolean) -> Unit
    ) : RecyclerView.Adapter<MacrosAdapter.MacroViewHolder>() {
        private var macros: List<Macro> = emptyList()
        fun submitList(newMacros: List<Macro>) {
            macros = newMacros
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MacroViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_macro, parent, false)
            return MacroViewHolder(view)
        }
        override fun onBindViewHolder(holder: MacroViewHolder, position: Int) {
            val macro = macros[position]
            holder.bind(macro)
        }
        override fun getItemCount(): Int = macros.size
        inner class MacroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.macro_name_text_view)
            private val activeSwitch: SwitchCompat = itemView.findViewById(R.id.macro_active_switch)
            private val editButton: ImageButton = itemView.findViewById(R.id.macro_edit_button)
            private val deleteButton: ImageButton = itemView.findViewById(R.id.macro_delete_button)
            fun bind(macro: Macro) {
                nameTextView.text = macro.name

                // OLED Theming for SwitchCompat
                val prefs = PreferenceManager.getDefaultSharedPreferences(itemView.context)
                val currentTheme = prefs.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)

                if (ThemeManager.THEME_OLED == currentTheme) {
                    val accentColor = prefs.getInt("pref_oled_accent_general", DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL)
                    val secondaryTextColor = prefs.getInt("pref_oled_general_text_secondary", DynamicThemeApplicator.DEFAULT_OLED_GENERAL_TEXT_SECONDARY)
                    val primaryTextColor = prefs.getInt("pref_oled_general_text_primary", DynamicThemeApplicator.DEFAULT_OLED_GENERAL_TEXT_PRIMARY)

                    val thumbTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf(-android.R.attr.state_checked)
                        ),
                        intArrayOf(
                            accentColor,        // Checked
                            primaryTextColor    // Unchecked
                        )
                    )

                    val trackTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf(-android.R.attr.state_checked)
                        ),
                        intArrayOf(
                            ColorUtils.setAlphaComponent(accentColor, 128),        // Checked (50% alpha)
                            ColorUtils.setAlphaComponent(secondaryTextColor, 128) // Unchecked (50% alpha)
                        )
                    )
                    activeSwitch.thumbTintList = thumbTintList
                    activeSwitch.trackTintList = trackTintList
                }

                activeSwitch.setOnCheckedChangeListener(null)
                activeSwitch.isChecked = macro.isActive
                activeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) { // Only trigger if user interacts
                         onActiveChanged(macro, isChecked)
                    }
                }
                editButton.setOnClickListener { onEditClick(macro) }
                deleteButton.setOnClickListener { onDeleteClick(macro) }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
