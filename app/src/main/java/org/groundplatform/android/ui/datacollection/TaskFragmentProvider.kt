package org.groundplatform.android.ui.datacollection

import javax.inject.Inject
import javax.inject.Provider
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskFragment

class TaskFragmentProvider
@Inject
constructor(
  val captureLocationTaskFragmentProvider: Provider<CaptureLocationTaskFragment>,
  val drawAreaTaskFragmentProvider: Provider<DrawAreaTaskFragment>,
  val dropPinTaskFragmentProvider: Provider<DropPinTaskFragment>,
) {

  fun getFragmentForTask(task: Task): AbstractTaskFragment<*> {
    val taskFragment =
      when (task.type) {
        Task.Type.TEXT -> TextTaskFragment()
        Task.Type.MULTIPLE_CHOICE -> MultipleChoiceTaskFragment()
        Task.Type.PHOTO -> PhotoTaskFragment()
        Task.Type.DROP_PIN -> dropPinTaskFragmentProvider.get()
        Task.Type.DRAW_AREA -> drawAreaTaskFragmentProvider.get()
        Task.Type.NUMBER -> NumberTaskFragment()
        Task.Type.DATE -> DateTaskFragment()
        Task.Type.TIME -> TimeTaskFragment()
        Task.Type.CAPTURE_LOCATION -> captureLocationTaskFragmentProvider.get()
        Task.Type.INSTRUCTIONS -> InstructionTaskFragment()
        Task.Type.UNKNOWN ->
          throw UnsupportedOperationException("Unsupported task type: ${task.type}")
      }
    return taskFragment.also { it.taskId = task.id }
  }
}
