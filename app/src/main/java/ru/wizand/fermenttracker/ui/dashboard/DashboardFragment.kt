package ru.wizand.fermenttracker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.FragmentDashboardBinding
import ru.wizand.fermenttracker.ui.dashboard.adapter.NotificationAdapter
import ru.wizand.fermenttracker.vm.SharedViewModel
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        // 👇 Клик по лейблу события
        binding.tvNextEventName.setOnClickListener {
            val event = viewModel.nextEvent.value
            if (event != null) {
                val batchId = event.third
                val action = DashboardFragmentDirections
                    .actionDashboardFragmentToBatchDetailFragment(batchId)
                findNavController().navigate(action)
            }
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter()
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshDashboardData()
        }
    }

    private fun observeViewModel() {
        // Активные партии
        viewModel.activeBatchesCount.observe(viewLifecycleOwner, Observer { count ->
            if (isAdded()) binding.tvActiveBatchesCount.text = count.toString()
        })

        // Завершённые этапы за неделю
        viewModel.completedStagesThisWeek.observe(viewLifecycleOwner, Observer { count ->
            if (isAdded()) binding.tvCompletedStagesCount.text = count.toString()
        })

        // Средняя потеря веса
        viewModel.avgWeightLoss.observe(viewLifecycleOwner, Observer { weightLoss ->
            if (isAdded()) binding.tvAvgWeightLossValue.text = String.format("%.1f%%", weightLoss)
        })

        // Ближайшее событие
        viewModel.nextEvent.observe(viewLifecycleOwner, Observer { event ->
            if (event != null) {
                binding.tvNextEventName.text = event.first
                binding.tvNextEventTime.text = formatEventTime(event.second)
            } else {
                binding.tvNextEventName.text = getString(R.string.no_upcoming_events)
                binding.tvNextEventTime.text = ""
            }
        })

        // Недавние уведомления
        viewModel.recentCompletedStages.observe(viewLifecycleOwner, Observer { stages ->
            if (isAdded()) notificationAdapter.submitList(stages)
        })

        // Обновление состояния загрузки
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isAdded()) binding.swipeRefreshLayout.isRefreshing = isLoading
        })
    }

    private fun formatEventTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now

        return when {
            diff < 0 -> getString(R.string.event_passed)
            diff < 60 * 60 * 1000 -> { // Меньше часа
                val minutes = diff / (60 * 1000)
                resources.getQuantityString(R.plurals.minutes_until, minutes.toInt(), minutes.toInt())
            }
            diff < 24 * 60 * 60 * 1000 -> { // Меньше дня
                val hours = diff / (60 * 60 * 1000)
                resources.getQuantityString(R.plurals.hours_until, hours.toInt(), hours.toInt())
            }
            diff < 7 * 24 * 60 * 60 * 1000 -> { // Меньше недели
                val days = diff / (24 * 60 * 60 * 1000)
                resources.getQuantityString(R.plurals.days_until, days.toInt(), days.toInt())
            }
            else -> {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем адаптер перед отсоединением
        binding.rvNotifications.adapter = null
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        // Остановить наблюдение при паузе
        viewModel.stopObservingBatches()
    }
}