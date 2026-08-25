/*
 * Copyright 2018 Google LLC
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
import android.content.res.Resources
import com.google.android.gms.common.GoogleApiAvailability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton
import org.groundplatform.android.R
import org.groundplatform.android.util.SurveyDeepLinkParser
import org.groundplatform.ui.util.AndroidDateFormatter
import org.groundplatform.ui.util.ComposeStringResolver
import org.groundplatform.ui.util.DateFormatter
import org.groundplatform.ui.util.StringResolver

@InstallIn(SingletonComponent::class)
@Module(includes = [ViewModelModule::class])
object GroundApplicationModule {

  @Provides
  @Singleton
  fun googleApiAvailability(): GoogleApiAvailability = GoogleApiAvailability.getInstance()

  @Provides
  fun provideResources(@ApplicationContext context: Context): Resources = context.resources

  @Provides fun provideLocale(): Locale = Locale.getDefault()

  @Provides
  fun providesSurveyDeepLinkParser(resources: Resources) =
    SurveyDeepLinkParser(
      deepLinkHost = resources.getString(R.string.deeplink_host),
      deepLinkPath = resources.getString(R.string.survey_deeplink_path),
    )

  @Provides
  @Singleton
  fun provideDateFormatter(@ApplicationContext context: Context): DateFormatter =
    AndroidDateFormatter(context)

  @Provides @Singleton fun provideStringResolver(): StringResolver = ComposeStringResolver
}
