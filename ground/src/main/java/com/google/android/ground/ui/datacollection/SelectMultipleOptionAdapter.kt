package com.google.android.ground.ui.datacollection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.MultipleChoiceCheckboxItemBinding
import com.google.android.ground.databinding.MultipleChoiceRadiobuttonItemBinding
import com.google.android.ground.ui.datacollection.SelectMultipleOptionAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [OptionViewModel]s with the
 * [ViewHolder] checkbox views.
 */
class SelectMultipleOptionAdapter(private val options: List<OptionViewModel>, context: Context) :
  ArrayAdapter<OptionViewModel>(context, R.layout.multiple_choice_checkbox_item, options) {
  private class ViewHolder {
    lateinit var checkBox: CheckBox
  }

  override fun getCount(): Int = options.size

  override fun getItem(position: Int): OptionViewModel = options[position]

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var convertView = convertView
    val viewHolder: ViewHolder
    val result: View
    if (convertView == null) {
      viewHolder = ViewHolder()
      val binding = MultipleChoiceCheckboxItemBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
      binding.option = options[position].option
      convertView = binding.checkbox
      viewHolder.checkBox = binding.checkbox
      result = convertView
      convertView.tag = viewHolder
    } else {
      viewHolder = convertView.tag as ViewHolder
      result = convertView
    }

    val item = getItem(position)
    viewHolder.checkBox.isChecked = item.isChecked
    return result
  }
}
