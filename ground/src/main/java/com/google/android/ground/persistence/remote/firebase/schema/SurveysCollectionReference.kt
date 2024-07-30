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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.persistence.remote.firebase.base.FluentCollectionReference
import com.google.android.ground.proto.Role
import com.google.android.ground.proto.Survey as SurveyProto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val ACL_FIELD = SurveyProto.ACL_FIELD_NUMBER.toString()

class SurveysCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  fun survey(id: String) = SurveyDocumentReference(reference().document(id))

  fun getReadable(user: User): Flow<List<Survey>> =
    reference()
      .whereIn(
        FieldPath.of(ACL_FIELD, user.email),
        listOf(Role.SURVEY_ORGANIZER, Role.DATA_COLLECTOR, Role.VIEWER).map { it.ordinal },
      )
      .snapshots()
      .map { it.documents.map { doc -> doc.let { SurveyConverter.toSurvey(doc) } } }
}
