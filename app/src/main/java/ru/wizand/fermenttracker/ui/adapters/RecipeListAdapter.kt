package ru.wizand.fermenttracker.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Recipe

/**
 * Sealed class для payloads, используемых при частичном обновлении элементов списка.
 */
sealed class RecipePayload {
    data class SelectionChanged(val isSelected: Boolean) : RecipePayload()
}

class RecipeListAdapter(
    private val onSelectionChanged: (Int) -> Unit,
    private val onRecipeClicked: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeListAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    private val selectedItems = mutableSetOf<Recipe>()
    var multiSelectMode = false
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = getItem(position)
        holder.bind(recipe, isSelected(recipe))
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val recipe = getItem(position)
            payloads.forEach { payload ->
                when (payload) {
                    is RecipePayload.SelectionChanged -> {
                        holder.updateSelection(recipe, payload.isSelected)
                    }
                }
            }
        }
    }

    private fun toggleSelection(recipe: Recipe) {
        val position = currentList.indexOf(recipe)
        if (position != -1) {
            val wasSelected = selectedItems.contains(recipe)
            if (wasSelected) {
                selectedItems.remove(recipe)
            } else {
                selectedItems.add(recipe)
            }
            onSelectionChanged(selectedItems.size)
            notifyItemChanged(position, RecipePayload.SelectionChanged(!wasSelected))
        }
    }

    fun getSelectedItems(): List<Recipe> = selectedItems.toList()

    fun clearSelection() {
        val positionsToClear = selectedItems.mapNotNull { recipe ->
            currentList.indexOf(recipe).takeIf { it != -1 }
        }

        selectedItems.clear()
        multiSelectMode = false
        onSelectionChanged(0)

        // Обновляем только элементы, которые были выбраны
        positionsToClear.forEach { position ->
            notifyItemChanged(position, RecipePayload.SelectionChanged(false))
        }
    }

    private fun isSelected(recipe: Recipe) = selectedItems.contains(recipe)

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtRecipeName)
        private val txtType: TextView = itemView.findViewById(R.id.tvRecipeType)

        fun bind(recipe: Recipe, selected: Boolean) {
            txtName.text = recipe.type
            txtType.text = recipe.ingredients

            updateSelection(recipe, selected)

            itemView.setOnClickListener {
                if (multiSelectMode) {
                    toggleSelection(recipe)
                } else {
                    onRecipeClicked(recipe)
                }
            }

            itemView.setOnLongClickListener {
                if (!multiSelectMode) multiSelectMode = true
                toggleSelection(recipe)
                true
            }
        }

        fun updateSelection(recipe: Recipe, selected: Boolean) {
            itemView.setBackgroundColor(
                if (selected) ContextCompat.getColor(itemView.context, android.R.color.holo_blue_light)
                else Color.TRANSPARENT
            )
        }
    }
}

class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
    override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean =
        oldItem.type == newItem.type

    override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean =
        oldItem == newItem
}