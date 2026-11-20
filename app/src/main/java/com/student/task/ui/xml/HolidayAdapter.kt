package com.student.task.ui.xml

import android.content.Context
import android.content.res.ColorStateList
import android.transition.TransitionManager
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.student.task.R
import com.student.task.databinding.ItemHolidayCardBinding
import com.student.task.databinding.ItemLoadingMoreBinding
import com.student.task.presentation.model.CardState
import com.student.task.presentation.model.HolidayUiModel

class HolidayAdapter(
    private val onCardClick: (Int) -> Unit,
    private val onFavoriteClick: (Int) -> Unit
) : ListAdapter<HolidayAdapter.ListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private var isLoadingMore = false

    sealed class ListItem {
        data class HolidayItem(val uiModel: HolidayUiModel) : ListItem()
        data object LoadingItem : ListItem()
    }

    fun submitHolidays(holidays: List<HolidayUiModel>, loadingMore: Boolean) {
        isLoadingMore = loadingMore
        val items = mutableListOf<ListItem>()
        items.addAll(holidays.map { ListItem.HolidayItem(it) })
        if (loadingMore) {
            items.add(ListItem.LoadingItem)
        }
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.HolidayItem -> VIEW_TYPE_HOLIDAY
            is ListItem.LoadingItem -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HOLIDAY -> {
                val binding = ItemHolidayCardBinding.inflate(inflater, parent, false)
                HolidayViewHolder(binding)
            }
            VIEW_TYPE_LOADING -> {
                val binding = ItemLoadingMoreBinding.inflate(inflater, parent, false)
                LoadingViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }


    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.HolidayItem -> (holder as HolidayViewHolder).bind(item.uiModel)
            is ListItem.LoadingItem -> { }
        }
    }

    inner class HolidayViewHolder(
        private val binding: ItemHolidayCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uiModel: HolidayUiModel) {
            val holiday = uiModel.holiday

            binding.emojiText.text = holiday.category.emoji
            binding.categoryBadge.text = holiday.category.displayName
            binding.holidayName.text = holiday.name
            binding.holidayDate.text = holiday.date
            binding.officialBadge.visibility = if (holiday.isOfficial) View.VISIBLE else View.GONE
            binding.description.text = holiday.description

            binding.cardRoot.setOnClickListener {
                onCardClick(holiday.id)
            }

            binding.favoriteIcon.setOnClickListener {
                onFavoriteClick(holiday.id)
            }

            when (uiModel.cardState) {
                CardState.Default -> {
                    bindDefault()
                }
                CardState.Expanded -> {
                    bindExpanded()
                }
                CardState.Favorite -> {
                    bindFavorite()
                }

                CardState.FavoriteExpanded -> {
                    bindFavoriteExpanded()
                }
            }
        }

        private fun bindDefault() {
            TransitionManager.beginDelayedTransition(
                binding.cardRoot as ViewGroup
            )
            binding.description.visibility = View.GONE
            binding.cardRoot.setCardBackgroundColor(ContextCompat.getColor(binding.root.context,R.color.white))
            binding.favoriteIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, R.color.gray)
            )
        }

        private fun bindFavorite(){
            TransitionManager.beginDelayedTransition(
                binding.cardRoot as ViewGroup
            )
            binding.description.visibility = View.GONE
            binding.cardRoot.setCardBackgroundColor(ContextCompat.getColor(binding.root.context,R.color.favorite))
            binding.favoriteIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, R.color.red)
            )
        }

        private fun bindExpanded(){
            TransitionManager.beginDelayedTransition(
                binding.cardRoot as ViewGroup
            )
            binding.description.visibility = View.VISIBLE

            binding.cardRoot.setCardBackgroundColor(ContextCompat.getColor(binding.root.context,R.color.white))
            binding.favoriteIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, R.color.gray)
            )


        }

        private fun bindFavoriteExpanded(){
            TransitionManager.beginDelayedTransition(
                binding.cardRoot as ViewGroup
            )
            binding.description.visibility = View.VISIBLE

            binding.cardRoot.setCardBackgroundColor(ContextCompat.getColor(binding.root.context,R.color.favorite))
            binding.favoriteIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, R.color.red)
            )


        }




    }

    class LoadingViewHolder(binding: ItemLoadingMoreBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return when {
                oldItem is ListItem.HolidayItem && newItem is ListItem.HolidayItem ->
                    oldItem.uiModel.holiday.id == newItem.uiModel.holiday.id
                oldItem is ListItem.LoadingItem && newItem is ListItem.LoadingItem -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_HOLIDAY = 0
        private const val VIEW_TYPE_LOADING = 1
    }
}
