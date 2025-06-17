package com.speakkey.ui.macros

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drgraff.speakkey.R
import com.speakkey.data.Macro
import com.speakkey.data.MacroRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.drgraff.speakkey.utils.DynamicThemeApplicator
import com.drgraff.speakkey.utils.ThemeManager
import kotlinx.coroutines.launch

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
    // For FAB styling, store applied colors if needed for onResume check beyond onSharedPreferenceChanged
    private var mAppliedAccentGeneralColor: Int = 0
    private var mAppliedButtonTextIconColor: Int = 0


    private val editMacroResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
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

            val accentGeneralColor = sharedPreferences.getInt(
                "pref_oled_accent_general",
                DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL
            )
            val buttonTextIconColor = sharedPreferences.getInt(
                "pref_oled_button_text_icon",
                DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
            )
            fabAddMacro.backgroundTintList = ColorStateList.valueOf(accentGeneralColor)
            fabAddMacro.imageTintList = ColorStateList.valueOf(buttonTextIconColor)
            Log.d(TAG, String.format("MacroListActivity: Styled fabAddMacro with BG=0x%08X, IconTint=0x%08X", accentGeneralColor, buttonTextIconColor))

            // Store for onResume check
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
            Log.d(TAG, "MacroListActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor))
        } else {
            this.mAppliedTopbarBackgroundColor = 0
            this.mAppliedTopbarTextIconColor = 0
            this.mAppliedMainBackgroundColor = 0
            this.mAppliedAccentGeneralColor = 0 // Reset FAB tracked colors too
            this.mAppliedButtonTextIconColor = 0
            Log.d(TAG, "MacroListActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.")
        }
    }

    private fun setupRecyclerView() {
        macrosAdapter = MacrosAdapter(
            onEditClickListener = { macro ->
                val intent = Intent(this, MacroEditorActivity::class.java)
                intent.putExtra(MacroEditorActivity.EXTRA_MACRO_ID, macro.id)
                editMacroResultLauncher.launch(intent)
            },
            onDeleteClickListener = { macro ->
                showDeleteConfirmationDialog(macro)
            },
            onToggleActiveListener = { macro, isActive ->
                lifecycleScope.launch {
                    macro.isActive = isActive
                    macroRepository.updateMacro(macro)
                    // Optional: provide feedback to the user
                }
            },
            onReorderListener = { macros ->
                lifecycleScope.launch {
                    macroRepository.updateMacroOrder(macros)
                }
            }
        )
        recyclerView.adapter = macrosAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val itemTouchHelperCallback = MacroItemTouchHelperCallback(macrosAdapter)
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun loadMacros() {
        lifecycleScope.launch {
            val macros = macroRepository.getAllMacrosOrdered()
            macrosAdapter.submitList(macros)
            updateMacrosEmptyState(macros.isEmpty())
        }
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
            .setPositiveButton(getString(R.string.delete_action)) { _, _ ->
                lifecycleScope.launch {
                    macroRepository.deleteMacro(macro)
                    loadMacros() // Refresh the list
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
                // Check FAB colors
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
            Log.d(TAG, "Main theme preference changed. Recreating MacroListActivity.")
            recreate()
        } else if (isOledColorKey) {
            val currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT)
            if (ThemeManager.THEME_OLED == currentTheme) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating MacroListActivity.")
                recreate()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
