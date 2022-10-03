package com.google.android.ground.ui.datacollection

/** Exception thrown when the user attempts to advance past a Task, but the input is incomplete. */
class IncompleteInputException(message: String?) : RuntimeException(message)
