package com.student.task.ui.xml

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.student.task.databinding.FragmentHolidayXmlBinding
import com.student.task.presentation.HolidayViewModel
import com.student.task.presentation.ScreenState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HolidayXmlFragment : Fragment() {

    private var _binding: FragmentHolidayXmlBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HolidayViewModel by viewModels()

    private val adapter = HolidayAdapter(
        onCardClick = { holidayId -> viewModel.toggleCardState(holidayId) },
        onFavoriteClick = { holidayId -> viewModel.toggleFavorite(holidayId) }
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHolidayXmlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeState()

        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }

        binding.swipeRefresh.setOnRefreshListener{
            viewModel.setRefreshing(true)
            viewModel.retry()

        }

        binding.chipGroup.setOnCheckedStateChangeListener { group, ints ->
            val checkedId = ints.firstOrNull()
            viewModel.filterHolidays(checkedId)
        }

    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (lastVisibleItem >= totalItemCount - 2) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screenState.collect { state ->
                    when (state) {
                        is ScreenState.Loading -> showLoading()
                        is ScreenState.Error -> showError(state.message)
                        is ScreenState.Data -> showData(state)
                        is ScreenState.Refreshing -> {}
                    }
                }
            }
        }

    }

    private fun showLoading() {
        binding.recyclerView.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.recyclerView.visibility = View.GONE
        binding.loadingLayout.visibility = View.GONE

        binding.errorLayout.visibility = View.VISIBLE

        binding.errorMessage.text = message
    }

    private fun showData(state: ScreenState.Data) {
        binding.swipeRefresh.isRefreshing = false
        binding.loadingLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        if (state.holidays.isEmpty()){
            binding.recyclerView.visibility = View.GONE
            showEmpty()
            return
        }
        binding.emptyListLayout.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        adapter.submitHolidays(state.holidays, state.isLoadingMore)
    }

    private fun showEmpty(){
        binding.emptyListLayout.visibility = View.VISIBLE
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
