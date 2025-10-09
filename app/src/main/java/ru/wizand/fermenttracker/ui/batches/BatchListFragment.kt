package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.FragmentBatchListBinding
import ru.wizand.fermenttracker.ui.adapters.BatchPagingAdapter
import ru.wizand.fermenttracker.ui.adapters.LoadStateAdapter
import ru.wizand.fermenttracker.vm.SharedViewModel

class BatchListFragment : Fragment() {

    private var _binding: FragmentBatchListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SharedViewModel by activityViewModels()

    private val adapter = BatchPagingAdapter(
        onItemClick = { batch ->
            val bundle = Bundle().apply { putString("batchId", batch.id) }
            findNavController().navigate(R.id.action_batchList_to_batchDetail, bundle)
        },
        onDeleteClick = { batch ->
            viewModel.deleteBatch(batch.id)
            Toast.makeText(context, getString(R.string.batch_deleted), Toast.LENGTH_SHORT).show()
        },
        onEditClick = { batch ->
            val bundle = Bundle().apply { putString("batchId", batch.id) }
            findNavController().navigate(R.id.action_batchList_to_createBatch, bundle)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView
        searchView.queryHint = getString(R.string.search)

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.updateFilterQuery(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.updateFilterQuery(newText ?: "")
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val options = arrayOf(
            getString(R.string.filter_by_name),
            getString(R.string.filter_by_date),
            getString(R.string.filter_by_status)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.filter_by)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.updateFilterCriteria("name")
                    1 -> viewModel.updateFilterCriteria("date")
                    2 -> viewModel.updateFilterCriteria("status")
                }
            }
            .show()
    }

    private fun setupRecyclerView() {
        binding.rvBatches.layoutManager = LinearLayoutManager(requireContext())
        val footerAdapter = LoadStateAdapter { adapter.retry() }
        val concatAdapter = adapter.withLoadStateFooter(footerAdapter)
        binding.rvBatches.adapter = concatAdapter

        adapter.addLoadStateListener { loadState ->
            binding.progressBar.visibility =
                if (loadState.refresh is LoadState.Loading) View.VISIBLE else View.GONE

            if (loadState.refresh is LoadState.Error) {
                val error = (loadState.refresh as LoadState.Error).error
                binding.tvErrorMessage.text = error.localizedMessage
                binding.tvErrorMessage.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
            } else {
                binding.tvErrorMessage.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
            }

            val isEmpty =
                loadState.refresh is LoadState.NotLoading &&
                        loadState.append.endOfPaginationReached &&
                        adapter.itemCount == 0
            binding.cardEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.batchesPaged.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnRetry.setOnClickListener { adapter.retry() }
        binding.fabAddBatch.setOnClickListener {
            findNavController().navigate(R.id.action_batchList_to_createBatch)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
