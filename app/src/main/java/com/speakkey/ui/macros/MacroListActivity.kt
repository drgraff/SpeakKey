package com.speakkey.ui.macros

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.drgraff.speakkey.R // Changed R import
import android.widget.Toast // Added Toast import
import com.speakkey.data.Macro
import com.speakkey.data.MacroRepository

class MacroListActivity : AppCompatActivity() {

    private lateinit var macroRepository: MacroRepository
    private lateinit var macrosAdapter: MacrosAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyMacrosTextView: TextView // Added for empty state
    private lateinit var fabAddMacro: FloatingActionButton

    private val editMacroResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadMacros()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_macros)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_macros)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Macros"

        macroRepository = MacroRepository(applicationContext)
        recyclerView = findViewById(R.id.macros_recycler_view)
        emptyMacrosTextView = findViewById(R.id.empty_macros_text_view) // Initialized
        fabAddMacro = findViewById(R.id.fab_add_macro)

        setupRecyclerView()
        loadMacros()

        fabAddMacro.setOnClickListener {
            val intent = Intent(this, MacroEditorActivity::class.java)
            editMacroResultLauncher.launch(intent)
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
                // Optional: provide visual feedback or reload just this item if needed
                // loadMacros() // This is a full reload, could be optimized
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
            .setTitle("Delete Macro")
            .setMessage("Are you sure you want to delete '${macro.name}'?")
            .setPositiveButton("Delete") { dialog, _ ->
                macroRepository.deleteMacro(macro.id)
                loadMacros() // This will also update the empty state
                Toast.makeText(this@MacroListActivity, "Macro '${macro.name}' deleted", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
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
            notifyDataSetChanged() // Consider using DiffUtil for better performance (especially with filtering/sorting)
            // updateMacrosEmptyState(newMacros.isEmpty()) // Called from loadMacros
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
                activeSwitch.isChecked = macro.isActive

                activeSwitch.setOnCheckedChangeListener(null) // Important to clear previous listener
                activeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    // Prevent unnecessary calls if the state is already what is being set
                    if (buttonView.isPressed) { // Only trigger if user interacts
                        onActiveChanged(macro, isChecked)
                        // Optionally, provide immediate feedback via Toast if desired
                        // Toast.makeText(itemView.context, "${macro.name} is now ${if(isChecked) "active" else "inactive"}", Toast.LENGTH_SHORT).show()
                    }
                }

                editButton.setOnClickListener { onEditClick(macro) }
                deleteButton.setOnClickListener { onDeleteClick(macro) }
            }
        }
    }
}
