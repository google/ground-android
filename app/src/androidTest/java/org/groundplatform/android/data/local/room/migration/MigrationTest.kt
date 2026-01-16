/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.data.local.room.migration

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.data.local.room.LocalDatabase
import org.groundplatform.android.data.local.room.migration.MigrationTestDataGenerator.getConditionContentValues
import org.groundplatform.android.data.local.room.migration.MigrationTestDataGenerator.getExpressionContentValues
import org.groundplatform.android.data.local.room.migration.MigrationTestDataGenerator.getJobContentValues
import org.groundplatform.android.data.local.room.migration.MigrationTestDataGenerator.getSurveyContentValues
import org.groundplatform.android.data.local.room.migration.MigrationTestDataGenerator.getTaskContentValues
import org.groundplatform.android.model.Survey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
  private val testDatabase = "test.db"
  private val migrations = arrayOf(Migration_124_125, Migration_125_126)

  @get:Rule
  val helper =
    MigrationTestHelper(
      instrumentation = InstrumentationRegistry.getInstrumentation(),
      assetsFolder = LocalDatabase::class.java.canonicalName!!,
      openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

  @Test
  @Throws(IOException::class)
  fun migrate124To125() = runBlocking {
    val id = "124-125"
    val title = "Test 124 to 125"
    val description = "migration description"
    val generalAccess =
      org.groundplatform.android.proto.Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED.ordinal

    helper.createDatabase(testDatabase, 124).apply {
      insert(
        "survey",
        SQLiteDatabase.CONFLICT_REPLACE,
        getSurveyContentValues(id, title, description, generalAccess),
      )
      close()
    }

    helper.runMigrationsAndValidate(testDatabase, 125, true, *migrations)

    with(getMigratedRoomDatabase(migrations)) {
      val migratedSurvey = surveyDao().getAll().first()[0].surveyEntity
      assertEquals(id, migratedSurvey.id)
      assertEquals(title, migratedSurvey.title)
      assertEquals(description, migratedSurvey.description)
      assertEquals(generalAccess, migratedSurvey.generalAccess)
      close()
    }
  }

  @Test
  @Throws(IOException::class)
  fun migrate125To126() = runBlocking {
    val correctSurvey = "survey1"
    val faultySurvey = "survey2"
    val jobId1 = "job1"
    val jobId2 = "job2"
    val taskId1 = "task1"
    val taskId2 = "task2"

    helper.createDatabase(testDatabase, 125).apply {
      // Insert correct survey: Survey + Job + Task + Condition + Expression
      insert("survey", SQLiteDatabase.CONFLICT_REPLACE, getSurveyContentValues(correctSurvey))
      insert("job", SQLiteDatabase.CONFLICT_REPLACE, getJobContentValues(jobId1, correctSurvey))
      insert("task", SQLiteDatabase.CONFLICT_REPLACE, getTaskContentValues(taskId1, jobId1))
      insert("condition", SQLiteDatabase.CONFLICT_REPLACE, getConditionContentValues(taskId1))
      insert("expression", SQLiteDatabase.CONFLICT_REPLACE, getExpressionContentValues(taskId1))

      // Insert faulty survey: Survey + Job + Task + Condition WITHOUT Expression
      insert("survey", SQLiteDatabase.CONFLICT_REPLACE, getSurveyContentValues(faultySurvey))
      insert("job", SQLiteDatabase.CONFLICT_REPLACE, getJobContentValues(jobId2, faultySurvey))
      insert("task", SQLiteDatabase.CONFLICT_REPLACE, getTaskContentValues(taskId2, jobId2))
      insert("condition", SQLiteDatabase.CONFLICT_REPLACE, getConditionContentValues(taskId2))

      close()
    }

    helper.runMigrationsAndValidate(testDatabase, 126, true, *migrations)

    with(getMigratedRoomDatabase(migrations)) {
      val surveys = surveyDao().getAll().first()
      assertEquals(1, surveys.size)
      assertEquals(correctSurvey, surveys[0].surveyEntity.id)

      val deletedSurvey = surveyDao().findSurveyById(faultySurvey)
      assertEquals(null, deletedSurvey)

      close()
    }
  }

  private fun getMigratedRoomDatabase(migrations: Array<Migration>): LocalDatabase =
    Room.databaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        LocalDatabase::class.java,
        testDatabase,
      )
      .allowMainThreadQueries()
      .addMigrations(*migrations)
      .build()
}
