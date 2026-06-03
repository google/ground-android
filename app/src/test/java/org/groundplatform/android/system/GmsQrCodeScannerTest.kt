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
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GmsQrCodeScannerTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val client: GmsBarcodeScanner = mock()
  private val task: Task<Barcode> = mock<Task<Barcode>>()
  private val moduleInstallClient: ModuleInstallClient = mock()
  private val moduleInstallTask: Task<ModuleInstallResponse> = mock()
  private lateinit var staticScanning: MockedStatic<GmsBarcodeScanning>
  private lateinit var staticModuleInstall: MockedStatic<ModuleInstall>
  private lateinit var scanner: GmsQrCodeScanner

  @Before
  fun setUp() {
    staticScanning = mockStatic(GmsBarcodeScanning::class.java)
    staticScanning
      .`when`<GmsBarcodeScanner> { GmsBarcodeScanning.getClient(any(), any()) }
      .thenReturn(client)
    staticModuleInstall = mockStatic(ModuleInstall::class.java)
    staticModuleInstall
      .`when`<ModuleInstallClient> { ModuleInstall.getClient(any<Context>()) }
      .thenReturn(moduleInstallClient)
    whenever(client.startScan()).thenReturn(task)
    whenever(task.addOnSuccessListener(any())).thenReturn(task)
    whenever(task.addOnCanceledListener(any())).thenReturn(task)
    whenever(task.addOnFailureListener(any())).thenReturn(task)
    whenever(moduleInstallClient.installModules(any())).thenReturn(moduleInstallTask)
    whenever(moduleInstallTask.addOnFailureListener(any())).thenReturn(moduleInstallTask)
    doAnswer { invocation ->
        @Suppress("UNCHECKED_CAST")
        (invocation.arguments[0] as OnSuccessListener<ModuleInstallResponse>).onSuccess(mock())
        moduleInstallTask
      }
      .whenever(moduleInstallTask)
      .addOnSuccessListener(any())
    scanner = GmsQrCodeScanner(context)
  }

  @After
  fun tearDown() {
    staticScanning.close()
    staticModuleInstall.close()
  }

  @Test
  fun `scan returns Success when barcode has raw value`() = runTest {
    val barcode = mock<Barcode> { whenever(it.rawValue).thenReturn(PAYLOAD) }
    onSuccess(barcode)

    assertEquals(GmsQrCodeScanner.Result.Success(PAYLOAD), scanner.scan())
  }

  @Test
  fun `scan returns Cancelled when barcode raw value is null`() = runTest {
    val barcode = mock<Barcode> { whenever(it.rawValue).thenReturn(null) }
    onSuccess(barcode)

    assertEquals(GmsQrCodeScanner.Result.Cancelled, scanner.scan())
  }

  @Test
  fun `scan returns Cancelled when scanner is closed`() = runTest {
    doAnswer { invocation ->
        (invocation.arguments[0] as OnCanceledListener).onCanceled()
        task
      }
      .whenever(task)
      .addOnCanceledListener(any())

    assertEquals(GmsQrCodeScanner.Result.Cancelled, scanner.scan())
  }

  @Test
  fun `scan returns Error when scanner fails`() = runTest {
    val cause = RuntimeException()
    doAnswer { invocation ->
        (invocation.arguments[0] as OnFailureListener).onFailure(cause)
        task
      }
      .whenever(task)
      .addOnFailureListener(any())

    val result = scanner.scan()
    assertTrue(result is GmsQrCodeScanner.Result.Error)
    assertEquals(cause, (result as GmsQrCodeScanner.Result.Error).cause)
  }

  @Test
  fun `scan returns Error when module install fails`() = runTest {
    val cause = RuntimeException()
    whenever(moduleInstallTask.addOnSuccessListener(any())).thenReturn(moduleInstallTask)
    doAnswer { invocation ->
        (invocation.arguments[0] as OnFailureListener).onFailure(cause)
        moduleInstallTask
      }
      .whenever(moduleInstallTask)
      .addOnFailureListener(any())

    val result = scanner.scan()
    assertTrue(result is GmsQrCodeScanner.Result.Error)
    assertEquals(cause, (result as GmsQrCodeScanner.Result.Error).cause)
  }

  private fun onSuccess(barcode: Barcode) {
    doAnswer { invocation ->
        @Suppress("UNCHECKED_CAST")
        (invocation.arguments[0] as OnSuccessListener<Barcode>).onSuccess(barcode)
        task
      }
      .whenever(task)
      .addOnSuccessListener(any())
  }

  companion object {
    private const val PAYLOAD = "https://ground.example/survey/123"
  }
}
