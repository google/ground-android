package com.google.android.ground.e2etest

import com.google.android.ground.model.task.Task

object TestConfig {
  const val LONG_TIMEOUT = 3000L
  const val SHORT_TIMEOUT = 1000L
  const val GROUND_PACKAGE = "com.google.android.ground"
  val TEST_SURVEY_TASKS_ADHOC =
    listOf(
      Task.Type.DROP_PIN,
      Task.Type.TEXT,
      Task.Type.MULTIPLE_CHOICE,
      Task.Type.MULTIPLE_CHOICE,
      Task.Type.NUMBER,
      Task.Type.DATE,
      Task.Type.TIME,
      Task.Type.PHOTO,
      Task.Type.CAPTURE_LOCATION,
    )
  const val TEST_SURVEY_IDENTIFIER = "test"
}
