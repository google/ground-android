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

package com.google.android.ground.persistence.remote.firestore.schema

import com.google.android.ground.model.Constants
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.persistence.remote.firestore.base.FluentCollectionReference
import com.google.android.ground.rx.annotations.Cold
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import io.reactivex.Single

private const val ACL_FIELD = "acl"
private val VALID_ROLES =
  listOf(
    Constants.OWNER,
    Constants.SURVEY_ORGANIZER,
    Constants.DATA_COLLECTOR,
    Constants.VIEWER_ROLE
  )

class SurveysCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  fun survey(id: String) = SurveyDocumentReference(reference().document(id))

  fun getReadable(user: User): @Cold Single<List<Survey>> =
    runQuery(reference().whereIn(FieldPath.of(ACL_FIELD, user.email), VALID_ROLES)) {
      doc: DocumentSnapshot ->
      SurveyConverter.toSurvey(doc)
    }
}
