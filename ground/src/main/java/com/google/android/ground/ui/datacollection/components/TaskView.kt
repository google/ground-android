package com.google.android.ground.ui.datacollection.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.ground.databinding.TaskFragWithHeaderBinding
import com.google.android.ground.databinding.TaskFragWithoutHeaderBinding
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel

sealed interface TaskView {

  fun addTaskView(view: View)

  val bottomControls: ViewGroup

  val root: View
}

class TaskViewWithHeader(private val binding: TaskFragWithHeaderBinding) : TaskView {
  override fun addTaskView(view: View) {
    binding.taskContainer.addView(view)
  }

  override val bottomControls = binding.bottomControls

  override val root = binding.root

  companion object {
    fun create(
      container: ViewGroup?,
      layoutInflater: LayoutInflater,
      fragment: Fragment,
      viewModel: AbstractTaskViewModel
    ): TaskView {
      val binding = TaskFragWithHeaderBinding.inflate(layoutInflater, container, false)
      binding.viewModel = viewModel
      binding.lifecycleOwner = fragment
      return TaskViewWithHeader(binding)
    }
  }
}

class TaskViewWithoutHeader(private val binding: TaskFragWithoutHeaderBinding) : TaskView {
  override fun addTaskView(view: View) {
    binding.taskContainer.addView(view)
  }

  override val bottomControls = binding.bottomControls

  override val root = binding.root

  companion object {
    fun create(
      container: ViewGroup?,
      layoutInflater: LayoutInflater,
      fragment: Fragment,
      viewModel: AbstractTaskViewModel
    ): TaskView {
      val binding = TaskFragWithoutHeaderBinding.inflate(layoutInflater, container, false)
      binding.viewModel = viewModel
      binding.lifecycleOwner = fragment
      return TaskViewWithoutHeader(binding)
    }
  }
}
