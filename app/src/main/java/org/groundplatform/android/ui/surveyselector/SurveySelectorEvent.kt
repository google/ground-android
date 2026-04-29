package org.groundplatform.android.ui.surveyselector

sealed interface SurveySelectorEvent {
  object NavigateToHome : SurveySelectorEvent

  data class ShowError(val errorType: ErrorType) : SurveySelectorEvent

  /** Errors surfaced from the Survey Selector screen for the host to display. */
  sealed interface ErrorType {
    /** Survey loading or activation timed out. */
    data object Timeout : ErrorType

    /** A generic error encountered while loading or activating a survey. */
    data class Generic(val cause: Throwable) : ErrorType

    /** The scanned QR code did not encode a valid survey link. */
    data object InvalidQrCode : ErrorType
  }
}