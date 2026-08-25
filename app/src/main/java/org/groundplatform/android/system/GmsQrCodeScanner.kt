/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groundplatform.android.system

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

@Singleton
class GmsQrCodeScanner @Inject constructor(@ApplicationContext private val context: Context) {

  suspend fun scan(): Result = suspendCancellableCoroutine { coroutine ->
    val options =
      GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    val scanner = GmsBarcodeScanning.getClient(context, options)

    // On some devices the code-scanner fails to start with an error code 13 (INTERNAL). Explicitly
    // requesting the module install first is a workaround for this.
    // https://issuetracker.google.com/issues/261579118.
    val moduleInstallRequest = ModuleInstallRequest.newBuilder().addApi(scanner).build()
    ModuleInstall.getClient(context)
      .installModules(moduleInstallRequest)
      .addOnSuccessListener { if (coroutine.isActive) startScan(scanner, coroutine) }
      .addOnFailureListener { e ->
        Timber.e(e, "Failed to install QR code scanner module")
        if (coroutine.isActive) coroutine.resume(Result.Error(e))
      }
  }

  private fun startScan(scanner: GmsBarcodeScanner, coroutine: CancellableContinuation<Result>) {
    scanner
      .startScan()
      .addOnSuccessListener { barcode ->
        Timber.d("Scanned QR code with raw value: ${barcode.rawValue}")
        coroutine.resume(barcode.rawValue?.let(Result::Success) ?: Result.Cancelled)
      }
      .addOnCanceledListener {
        Timber.d("QR code scan cancelled by user")
        coroutine.resume(Result.Cancelled)
      }
      .addOnFailureListener { e ->
        Timber.e(e, "QR code scan failed with exception")
        coroutine.resume(Result.Error(e))
      }
  }

  sealed interface Result {
    /** The scanner returned a decoded payload. */
    data class Success(val text: String) : Result

    /** The user dismissed the scanner without a successful scan. */
    data object Cancelled : Result

    /** The scan failed (camera unavailable, module install failure, etc.). */
    data class Error(val cause: Throwable) : Result
  }
}
