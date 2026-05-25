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
package org.groundplatform.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import org.groundplatform.android.BuildConfig
import org.groundplatform.android.R
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.ui.system.pdf.AndroidPdfRenderer
import org.groundplatform.ui.system.pdf.PdfExportService
import org.groundplatform.ui.system.pdf.PdfRenderer
import org.groundplatform.ui.system.pdf.image.AndroidPdfImageProvider
import org.groundplatform.ui.system.pdf.image.PdfImageProvider
import org.groundplatform.ui.system.pdf.io.AndroidPdfOutputProvider
import org.groundplatform.ui.system.pdf.io.AndroidPdfReportLauncher
import org.groundplatform.ui.system.pdf.io.PdfOutputProvider
import org.groundplatform.ui.system.pdf.io.PdfReportLauncher

@Module
@InstallIn(SingletonComponent::class)
object PdfReportModule {

  @Provides
  @Singleton
  fun providePdfImageProvider(@ApplicationContext context: Context): PdfImageProvider =
    AndroidPdfImageProvider(context = context, logoDrawableRes = R.drawable.ground_logo)

  @Provides
  @Singleton
  fun providePdfOutputFactory(@ApplicationContext context: Context): PdfOutputProvider =
    AndroidPdfOutputProvider(context)

  @Provides @Singleton fun providePdfRenderer(): PdfRenderer = AndroidPdfRenderer()

  @Provides
  @Singleton
  fun providePdfReportLauncher(@ApplicationContext context: Context): PdfReportLauncher =
    AndroidPdfReportLauncher(context = context, fileProviderAuthority = BuildConfig.APPLICATION_ID)

  @Provides
  @Singleton
  fun providePdfReportService(
    imageProvider: PdfImageProvider,
    renderer: PdfRenderer,
    outputProvider: PdfOutputProvider,
    launcher: PdfReportLauncher,
    @IoDispatcher coroutineDispatcher: CoroutineDispatcher,
  ): PdfExportService =
    PdfExportService(
      imageProvider = imageProvider,
      renderer = renderer,
      outputProvider = outputProvider,
      launcher = launcher,
      coroutineDispatcher = coroutineDispatcher,
    )
}
