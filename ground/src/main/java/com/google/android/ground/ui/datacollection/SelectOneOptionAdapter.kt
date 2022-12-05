package com.google.android.ground.ui.datacollection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.databinding.MultipleChoiceRadiobuttonItemBinding
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.task.Option
import com.google.android.ground.ui.datacollection.SelectOneOptionAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [Option] data with the [ViewHolder]
 * RadioButton views.
 */
class SelectOneOptionAdapter(private val options: List<Option>) :
  RecyclerView.Adapter<ViewHolder>() {

  private var selectedIndex = -1

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    ViewHolder(
      MultipleChoiceRadiobuttonItemBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
    )

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(options[position])

    holder.binding.radioButton.isChecked = position == selectedIndex

    holder.binding.radioButton.setOnClickListener {
      val oldPosition = selectedIndex
      selectedIndex = holder.adapterPosition

      holder.binding.radioButton.post {
        if (oldPosition >= 0) {
          notifyItemChanged(oldPosition)
        }
        notifyItemChanged(selectedIndex)
      }
    }
  }

  override fun getItemCount(): Int = options.size

  /** View item representing the [LocationOfInterest] data in the list. */
  class ViewHolder(internal val binding: MultipleChoiceRadiobuttonItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(option: Option) {
      binding.option = option
    }
  }
}
