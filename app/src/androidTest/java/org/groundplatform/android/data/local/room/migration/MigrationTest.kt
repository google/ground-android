package org.groundplatform.android.data.local.room.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.data.local.room.LocalDatabase
import org.groundplatform.android.model.Survey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
  private val testDatabase = "test.db"

  @get:Rule
  val helper = MigrationTestHelper(
    instrumentation = InstrumentationRegistry.getInstrumentation(),
    assetsFolder = LocalDatabase::class.java.canonicalName!!,
    openFactory = FrameworkSQLiteOpenHelperFactory()
  )
  private val testSurvey = Survey(
    id = "1",
    title = "Test Survey",
    description = "Description",
    jobMap = mapOf(),
    generalAccess = org.groundplatform.android.proto.Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED,
  )

  @Test
  @Throws(IOException::class)
  fun migrate124To125() = runBlocking {
    helper.createDatabase(testDatabase, 124).apply {
      insert(
        table = "survey",
        conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE,
        values = ContentValues().apply {
          put("id", testSurvey.id)
          put("title", testSurvey.title)
          put("description", testSurvey.description)
          put("general_access", testSurvey.generalAccess.ordinal)
        }
      )
      close()
    }

    helper.runMigrationsAndValidate(testDatabase, 125, true, Migration_124_125)

    with(getMigratedRoomDatabase(arrayOf(Migration_124_125))) {
      val migratedSurvey = surveyDao().getAll().first()[0].surveyEntity
      assertEquals(testSurvey.id, migratedSurvey.id)
      assertEquals(testSurvey.title, migratedSurvey.title)
      assertEquals(testSurvey.description, migratedSurvey.description)
      assertEquals(testSurvey.generalAccess.ordinal, migratedSurvey.generalAccess)
      close()
    }
  }

  private fun getMigratedRoomDatabase(migrations: Array<Migration>): LocalDatabase {
    return Room.databaseBuilder(
      InstrumentationRegistry.getInstrumentation().targetContext,
      LocalDatabase::class.java,
      testDatabase
    )
      .allowMainThreadQueries()
      .addMigrations(*migrations)
      .build()
  }

}