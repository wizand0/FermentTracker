package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.databinding.ItemLoadStateBinding
import android.view.View

class LoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<LoadStateAdapter.VH>() {

    override fun onBindViewHolder(holder: VH, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): VH {
        val binding = ItemLoadStateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding, retry)
    }

    class VH(
        private val binding: ItemLoadStateBinding,
        retry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRetry.setOnClickListener { retry() }
        }

        fun bind(loadState: LoadState) {
            if (loadState is LoadState.Loading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.GONE
                binding.tvErrorMessage.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                if (loadState is LoadState.Error) {
                    binding.tvErrorMessage.text = loadState.error.localizedMessage
                    binding.tvErrorMessage.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.VISIBLE
                } else {
                    binding.tvErrorMessage.visibility = View.GONE
                    binding.btnRetry.visibility = View.GONE
                }
            }
        }
    }
}