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
import org.groundplatform.android.di.coroutines.DefaultDispatcher
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.feature.pdf.AndroidPdfImageProvider
import org.groundplatform.feature.pdf.AndroidPdfOutputProvider
import org.groundplatform.feature.pdf.AndroidPdfRenderer
import org.groundplatform.feature.pdf.AndroidPdfReportLauncher
import org.groundplatform.feature.pdf.LoiReportExporter
import org.groundplatform.feature.pdf.PdfExportService
import org.groundplatform.feature.pdf.PdfImageProvider
import org.groundplatform.feature.pdf.PdfOutputProvider
import org.groundplatform.feature.pdf.PdfRenderer
import org.groundplatform.feature.pdf.PdfReportLauncher
import org.groundplatform.feature.pdf.mapper.LoiReportMapper
import org.groundplatform.feature.pdf.mapper.TaskValueMapper
import org.groundplatform.ui.util.DateFormatter
import org.groundplatform.ui.util.StringResolver

@Module
@InstallIn(SingletonComponent::class)
object PdfModule {

  @Provides
  @Singleton
  fun providePdfImageProvider(@ApplicationContext context: Context): PdfImageProvider =
    AndroidPdfImageProvider(context = context, logoDrawableRes = R.drawable.ground_logo)

  @Provides
  @Singleton
  fun providePdfOutputFactory(@ApplicationContext context: Context): PdfOutputProvider =
    AndroidPdfOutputProvider(context)

  @Provides
  @Singleton
  fun providePdfRenderer(@IoDispatcher ioDispatcher: CoroutineDispatcher): PdfRenderer =
    AndroidPdfRenderer(ioDispatcher)

  @Provides
  @Singleton
  fun provideTaskValueMapper(
    strings: StringResolver,
    dateFormatter: DateFormatter,
  ): TaskValueMapper = TaskValueMapper(strings = strings, dateFormatter = dateFormatter)

  @Provides
  @Singleton
  fun provideLoiReportMapper(
    taskValueMapper: TaskValueMapper,
    strings: StringResolver,
    dateFormatter: DateFormatter,
  ): LoiReportMapper =
    LoiReportMapper(
      taskValueMapper = taskValueMapper,
      strings = strings,
      dateFormatter = dateFormatter,
    )

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
    @DefaultDispatcher coroutineDispatcher: CoroutineDispatcher,
  ): PdfExportService =
    PdfExportService(
      imageProvider = imageProvider,
      renderer = renderer,
      outputProvider = outputProvider,
      launcher = launcher,
      coroutineDispatcher = coroutineDispatcher,
    )

  @Provides
  @Singleton
  fun provideLoiReportExporter(
    mapper: LoiReportMapper,
    exportService: PdfExportService,
  ): LoiReportExporter = LoiReportExporter(mapper = mapper, exportService = exportService)
}
