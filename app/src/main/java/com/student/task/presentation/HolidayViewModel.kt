package com.student.task.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.student.task.R
import com.student.task.domain.model.HolidayCategory
import com.student.task.domain.usecase.GetHolidaysPageUseCase
import com.student.task.presentation.model.CardState
import com.student.task.presentation.model.HolidayUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HolidayViewModel @Inject constructor(
    private val getHolidaysPageUseCase: GetHolidaysPageUseCase
) : ViewModel() {


    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private var currentFilter: Int? = R.id.chip_all

    private var currentPage = 0
    private var totalCount = 0
    private val allHolidays = mutableListOf<HolidayUiModel>()

    private var isRefreshing = false

    fun setRefreshing(value: Boolean){
        isRefreshing = value
    }

    init {
        loadInitial()
    }


    fun filterHolidays(chipId: Int?) {
        currentFilter = chipId
        val currentState = _screenState.value
        if (currentState is ScreenState.Data) {
            _screenState.value = currentState.copy(
                holidays = getFilteredHolidays()
            )
        }
    }

    private fun getFilteredHolidays(): List<HolidayUiModel> {
        return if (currentFilter == null || currentFilter == R.id.chip_all) {
            allHolidays.toList()
        } else {
            allHolidays.filter {
                when (currentFilter) {
                    R.id.chip_folk -> it.holiday.category == HolidayCategory.FOLK
                    R.id.chip_international -> it.holiday.category == HolidayCategory.INTERNATIONAL
                    R.id.chip_official -> it.holiday.category == HolidayCategory.OFFICIAL
                    R.id.chip_professional -> it.holiday.category == HolidayCategory.PROFESSIONAL
                    R.id.chip_ecclesiastical -> false
                    else -> true
                }

            }
        }

    }

    private fun loadInitial() {

        viewModelScope.launch {
            if (isRefreshing) {
                _screenState.value = ScreenState.Refreshing

            }else{
                _screenState.value = ScreenState.Loading
            }

            totalCount = getHolidaysPageUseCase.getTotalCount()

            getHolidaysPageUseCase(page = 0).fold(
                onSuccess = { holidays ->
                    allHolidays.clear()
                    allHolidays.addAll(holidays.map { HolidayUiModel(it) })
                    currentPage = 0
                    _screenState.value = ScreenState.Data(
                        holidays = getFilteredHolidays(),
                        isLoadingMore = false,
                        hasMorePages = allHolidays.size < totalCount,
                        currentPage = currentPage
                    )
                },
                onFailure = { error ->
                    _screenState.value = ScreenState.Error(
                        message = error.message ?: "Неизвестная ошибка"
                    )
                }
            )
        }
    }

    fun loadNextPage() {
        val current = _screenState.value
        if (current !is ScreenState.Data || current.isLoadingMore || !current.hasMorePages) return

        viewModelScope.launch {
            _screenState.value = current.copy(isLoadingMore = true)

            getHolidaysPageUseCase(page = currentPage + 1).fold(
                onSuccess = { holidays ->
                    currentPage++
                    allHolidays.addAll(holidays.map { HolidayUiModel(it) })
                    _screenState.value = ScreenState.Data(
                        holidays = getFilteredHolidays(),
                        isLoadingMore = false,
                        hasMorePages = allHolidays.size < totalCount,
                        currentPage = currentPage
                    )
                },
                onFailure = { error ->
                    _screenState.value = current.copy(isLoadingMore = false)
                }
            )
        }
    }

    fun retry() {
        loadInitial()
    }

    fun toggleCardState(holidayId: Int) {
        val current = _screenState.value
        if (current !is ScreenState.Data) return

        val updatedList = allHolidays.map { uiModel ->
            if (uiModel.holiday.id == holidayId) {
                val newState = when (uiModel.cardState) {
                    CardState.Default -> CardState.Expanded
                    CardState.Expanded -> CardState.Default
                    CardState.Favorite -> CardState.FavoriteExpanded
                    CardState.FavoriteExpanded -> CardState.Favorite

                }
                uiModel.copy(cardState = newState)
            } else {
                uiModel
            }
        }
        allHolidays.clear()
        allHolidays.addAll(updatedList)
        _screenState.value = current.copy(holidays = getFilteredHolidays())
    }

    fun toggleFavorite(holidayId: Int) {
        val current = _screenState.value
        if (current !is ScreenState.Data) return

        val updatedList = allHolidays.map { uiModel ->
            if (uiModel.holiday.id == holidayId) {
                val newState = when (uiModel.cardState) {
                    CardState.Favorite -> CardState.Default
                    CardState.Expanded -> CardState.FavoriteExpanded
                    CardState.FavoriteExpanded -> CardState.Expanded
                    else -> CardState.Favorite
                }
                uiModel.copy(cardState = newState)
            } else {
                uiModel
            }
        }
        allHolidays.clear()
        allHolidays.addAll(updatedList)
        _screenState.value = current.copy(holidays = getFilteredHolidays())
    }
}
