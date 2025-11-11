package org.groundplatform.android.data.local.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration_124_125 = object : Migration(124, 125) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("""
            CREATE TABLE IF NOT EXISTS survey_temp (
                id TEXT NOT NULL,
                title TEXT,
                description TEXT,
                acl TEXT,
                data_sharing_terms BLOB,
                general_access INTEGER NOT NULL DEFAULT 0,
                data_visibility INTEGER,
                PRIMARY KEY(id)
            )
        """.trimIndent())

    db.execSQL("""
            INSERT INTO survey_temp (id, title, description, acl, data_sharing_terms, general_access, data_visibility)
            SELECT id, title, description, acl, data_sharing_terms, 
                   COALESCE(general_access, 0), data_visibility
            FROM survey
        """.trimIndent())

    db.execSQL("DROP TABLE survey")

    db.execSQL("ALTER TABLE survey_temp RENAME TO survey")
  }
}
