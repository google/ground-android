/*
 * Copyright 2020 Google LLC
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
package org.groundplatform.android

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.persistence.local.room.LocalDatabase

@InstallIn(SingletonComponent::class)
@Module
object LocalDatabaseModule {
  // Double-checked locking singleton pattern
  @Volatile private var INSTANCE: LocalDatabase? = null

  @Provides
  @Singleton
  fun localDatabase(
    @ApplicationContext context: Context,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
  ): LocalDatabase {
    return INSTANCE
      ?: Room.databaseBuilder(context, LocalDatabase::class.java, Config.DB_NAME)
        // Use a separate thread for Room transactions to avoid deadlocks. This means that tests
        // that run Room
        // transactions can't use testCoroutines.scope.runBlockingTest, and have to simply use
        // runBlocking instead.
        .setTransactionExecutor(Executors.newSingleThreadExecutor())
        // Run queries and transactions on background I/O thread.
        .setQueryExecutor(ioDispatcher.asExecutor())
        .build()
        .also { INSTANCE = it }
  }
}
