package com.google.android.ground.domain.usecases.submission

import androidx.room.Transaction
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.submission.DropAPinTaskData
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.system.auth.AuthenticationManager
import timber.log.Timber
import javax.inject.Inject

class SubmitDataUseCase
@Inject
constructor(
  private val authManager: AuthenticationManager,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val submissionRepository: SubmissionRepository,
) {

  /**
   * Creates a Submission for the given [job] with the user's responses from the [taskDataDeltas].
   * If [loiId] is null a new LOI is created based on the first [TaskDataDelta] since the Suggest
   * LOI task is the first task in the Data Collection flow when a new LOI is being suggested.
   */
  @Transaction
  operator fun invoke(
    loiId: String?,
    job: Job,
    surveyId: String,
    taskDataDeltas: List<TaskDataDelta>
  ) {
    var loiIdToSubmit = loiId
    val taskDataDeltasToSubmit = taskDataDeltas.toMutableList()

    if (loiId == null) {
      // loiIds are null for Suggest LOI data collection flows
      when (val suggestLoiTaskData = taskDataDeltasToSubmit.removeAt(0).newTaskData.orElse(null)) {
        is DropAPinTaskData -> {
          loiIdToSubmit = saveLoi(suggestLoiTaskData.getPoint(), job, surveyId).id
        }
        else ->
          // TODO(#1351): Process result of DRAW_POLYGON task
          throw IllegalStateException("No suggest LOI Task found when loi ID was null")
      }
    }

    if (loiIdToSubmit == null) {
      throw IllegalStateException("No LOI found present for submission")
    }

    submissionRepository.saveSubmission(surveyId, loiIdToSubmit, taskDataDeltasToSubmit)
  }

  private fun saveLoi(geometry: Geometry, job: Job, surveyId: String): LocationOfInterest {
    val loi = locationOfInterestRepository.createLocationOfInterest(geometry, job, surveyId)
    locationOfInterestRepository
      .applyAndEnqueue(loi.toMutation(Mutation.Type.CREATE, authManager.currentUser.id))
      .blockingAwait()

    return loi
  }
}
