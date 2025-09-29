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

    private fun toggleSelection(recipe: Recipe) {
        if (selectedItems.contains(recipe)) {
            selectedItems.remove(recipe)
        } else {
            selectedItems.add(recipe)
        }
        onSelectionChanged(selectedItems.size)
        notifyItemChanged(currentList.indexOf(recipe))  // Только обновляем изменившийся элемент
    }

    fun getSelectedItems(): List<Recipe> = selectedItems.toList()

    fun clearSelection() {
        selectedItems.clear()
        multiSelectMode = false
        onSelectionChanged(0)
        notifyDataSetChanged()  // Здесь ок, так как очищаем все
    }

    private fun isSelected(recipe: Recipe) = selectedItems.contains(recipe)

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtRecipeName)
        private val txtType: TextView = itemView.findViewById(R.id.tvRecipeType)

        fun bind(recipe: Recipe, selected: Boolean) {
            txtName.text = recipe.type
            txtType.text = recipe.ingredients

            itemView.setBackgroundColor(
                if (selected) ContextCompat.getColor(itemView.context, android.R.color.holo_blue_light)
                else Color.TRANSPARENT
            )

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
    }
}

class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
    override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean =
        oldItem.type == newItem.type

    override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean =
        oldItem == newItem
}