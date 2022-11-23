package com.google.android.ground.ui.datacollection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.MultipleChoiceCheckboxItemBinding
import com.google.android.ground.model.task.Option
import com.google.android.ground.ui.datacollection.SelectMultipleOptionAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [OptionViewModel]s with the
 * [ViewHolder] checkbox views.
 */
class SelectMultipleOptionAdapter(private val options: List<OptionViewModel>) :
  RecyclerView.Adapter<ViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    ViewHolder(
      MultipleChoiceCheckboxItemBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
    )

  private val selectedPositions = mutableSetOf<Int>()

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(options[position])

    holder.binding.checkbox.isChecked = position in selectedPositions

    holder.binding.checkbox.setOnClickListener {
      val clickedPosition = holder.adapterPosition
//      val option = options[clickedPosition]
      if (clickedPosition in selectedPositions) {
        selectedPositions.remove(clickedPosition)
      } else {
        selectedPositions.add(clickedPosition)
      }
//      option.isChecked = !option.isChecked
      notifyItemChanged(clickedPosition)
//      holder.binding.checkbox.post { notifyItemChanged(clickedPosition) }
    }
  }

  override fun getItemCount(): Int = options.size

  override fun getItemId(position: Int): Long = position.toLong()

  class ViewHolder(internal val binding: MultipleChoiceCheckboxItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(option: OptionViewModel) {
      binding.viewModel = option
    }
  }
}
