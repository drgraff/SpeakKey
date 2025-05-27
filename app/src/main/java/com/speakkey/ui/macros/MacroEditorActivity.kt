package com.speakkey.ui.macros

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.drgraff.speakkey.R // Changed R import
import com.speakkey.data.ActionType
import com.speakkey.data.Macro
import com.speakkey.data.MacroAction
import com.speakkey.data.MacroRepository
import java.util.Collections
import java.util.UUID

class MacroEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MACRO_ID = "extra_macro_id"
    }

    private lateinit var macroRepository: MacroRepository
    private lateinit var actionsAdapter: MacroActionsAdapter

    private lateinit var editMacroName: TextInputEditText
    private lateinit var actionsRecyclerView: RecyclerView
    private lateinit var emptyActionsTextView: TextView // Added for empty state
    private lateinit var btnSaveMacro: Button
    private lateinit var btnCancelMacro: Button

    private var currentMacro: Macro? = null
    private val currentActions = mutableListOf<MacroAction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_macro)

        macroRepository = MacroRepository(applicationContext)

        editMacroName = findViewById(R.id.edit_macro_name)
        actionsRecyclerView = findViewById(R.id.macro_actions_recycler_view)
        emptyActionsTextView = findViewById(R.id.empty_actions_text_view) // Initialized
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
                // actionsAdapter.submitList(currentActions) // submitList is called in setupRecyclerView
                supportActionBar?.title = "Edit Macro"
            }
        } else {
            supportActionBar?.title = "Create New Macro"
        }
        updateActionsEmptyState() // Initial check

        btnSaveMacro.setOnClickListener { saveMacro() }
        btnCancelMacro.setOnClickListener { finish() }
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
                    actionsAdapter.notifyItemRangeChanged(index, currentActions.size - index) // To update subsequent items' positions
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
            // Edit functionality can be added later
        )
        actionsRecyclerView.layoutManager = LinearLayoutManager(this)
        actionsRecyclerView.adapter = actionsAdapter
        actionsAdapter.submitList(currentActions) // Submit initial list

        // Drag and drop for reordering
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0 // No swipe actions
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
                    if (fromPosition != toPosition) { // Only show Toast if position actually changed
                        Collections.swap(currentActions, fromPosition, toPosition)
                        actionsAdapter.notifyItemMoved(fromPosition, toPosition)
                        Toast.makeText(this@MacroEditorActivity, "Action reordered", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
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
        findViewById<Button>(R.id.btn_add_tab_action).setOnClickListener {
            addAction(ActionType.TAB)
        }
        findViewById<Button>(R.id.btn_add_enter_action).setOnClickListener {
            addAction(ActionType.ENTER)
        }
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
        findViewById<Button>(R.id.btn_add_pause_action).setOnClickListener {
            addAction(ActionType.PAUSE_CONFIRMATION)
        }
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
            actions = currentActions.toList() // Ensure it's a new list
        ) ?: Macro(
            id = UUID.randomUUID().toString(),
            name = name,
            isActive = false, // Default for new macros
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

    inner class MacroActionsAdapter(
        private val actions: MutableList<MacroAction>,
        private val onDeleteClick: (MacroAction) -> Unit,
        private val onMoveUp: (Int) -> Unit,
        private val onMoveDown: (Int) -> Unit
        // private val onEditClick: (MacroAction) -> Unit (for future use)
    ) : RecyclerView.Adapter<MacroActionsAdapter.ActionViewHolder>() {

        fun submitList(newActions: List<MacroAction>) {
            actions.clear()
            actions.addAll(newActions)
            notifyDataSetChanged() // Consider DiffUtil
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_macro_action, parent, false)
            return ActionViewHolder(view)
        }

        override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
            val action = actions[position]
            holder.bind(action, position)
        }

        override fun getItemCount(): Int = actions.size

        inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val descriptionTextView: TextView = itemView.findViewById(R.id.action_description_text_view)
            private val deleteButton: ImageButton = itemView.findViewById(R.id.action_delete_button)
            private val moveUpButton: ImageButton = itemView.findViewById(R.id.action_move_up_button)
            private val moveDownButton: ImageButton = itemView.findViewById(R.id.action_move_down_button)
            // private val editButton: ImageButton = itemView.findViewById(R.id.action_edit_button) // For future

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
