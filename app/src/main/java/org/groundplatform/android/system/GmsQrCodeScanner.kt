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
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import org.groundplatform.domain.model.qrscanner.QrScanResult

@Singleton
class GmsQrCodeScanner @Inject constructor(@ApplicationContext private val context: Context) {

  suspend fun scan(): QrScanResult {
    val options =
      GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    return runCatching { GmsBarcodeScanning.getClient(context, options).startScan().await() }
      .fold(
        onSuccess = { barcode ->
          barcode.rawValue?.let(QrScanResult::Success) ?: QrScanResult.Cancelled
        },
        onFailure = { error ->
          if (error is MlKitException && error.errorCode == MlKitException.CODE_SCANNER_CANCELLED) {
            QrScanResult.Cancelled
          } else {
            QrScanResult.Error(error)
          }
        },
      )
  }
}
