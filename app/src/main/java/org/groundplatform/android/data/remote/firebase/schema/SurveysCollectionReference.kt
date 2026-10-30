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

package org.groundplatform.android.data.remote.firebase.schema

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.snapshots
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.groundplatform.android.data.remote.firebase.base.FluentCollectionReference
import org.groundplatform.android.proto.Role
import org.groundplatform.android.proto.Survey as SurveyProto
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.User

/**
 * How long to ignore an empty cache snapshot before treating the survey list as empty.
 *
 * Firestore emits that snapshot when the first listen times out (about 10 seconds) and then retries
 * the server read. Waiting longer than the retry keeps the list in a loading state instead of
 * flashing the empty / sign-out UI. A settled server snapshot is delivered immediately.
 */
internal const val SURVEY_LIST_LOAD_TIMEOUT_MILLIS = 30_000L

private const val ACL_FIELD = SurveyProto.ACL_FIELD_NUMBER.toString()
private const val GENERAL_ACCESS_FIELD = SurveyProto.GENERAL_ACCESS_FIELD_NUMBER.toString()
private const val STATE = SurveyProto.STATE_FIELD_NUMBER.toString()
private val ALLOWED_ROLES =
  listOf(Role.SURVEY_ORGANIZER.ordinal, Role.DATA_COLLECTOR.ordinal, Role.VIEWER.ordinal)
private val ALLOWED_STATES = listOf(SurveyProto.State.READY_VALUE)

class SurveysCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  fun survey(id: String) = SurveyDocumentReference(reference().document(id))

  fun getReadable(user: User): Flow<List<Survey>> {
    val references =
      reference()
        .whereIn(STATE, ALLOWED_STATES)
        .whereIn(FieldPath.of(ACL_FIELD, user.email), ALLOWED_ROLES)

    return references.snapshots().toSettledSurveys { snapshot ->
      snapshot.documents
        .mapNotNull { SurveyConverter.toSurvey(it) }
        .filter { survey ->
          survey.generalAccess == Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED ||
            survey.generalAccess == Survey.GeneralAccess.RESTRICTED ||
            survey.generalAccess == Survey.GeneralAccess.UNLISTED
        }
    }
  }

  fun getPublicReadable(): Flow<List<Survey>> {
    val references =
      reference()
        .whereEqualTo(GENERAL_ACCESS_FIELD, SurveyProto.GeneralAccess.PUBLIC_VALUE)
        .whereIn(STATE, listOf(SurveyProto.State.READY_VALUE))
    return references.snapshots().toSettledSurveys { snapshot ->
      snapshot.documents.mapNotNull(SurveyConverter::toSurvey)
    }
  }
}

/**
 * Suppresses the empty cache snapshot emitted when the first server attempt times out.
 *
 * The listener stays open and the successful retry is emitted. If no settled snapshot arrives
 * before [SURVEY_LIST_LOAD_TIMEOUT_MILLIS], an empty list is emitted so the UI can offer sign-out,
 * and later server snapshots are still forwarded. The timeout is cancelled once a snapshot settles
 * or the upstream flow ends, so collection is not held open for the rest of the timeout.
 */
private fun Flow<QuerySnapshot>.toSettledSurveys(
  transform: (QuerySnapshot) -> List<Survey>
): Flow<List<Survey>> = channelFlow {
  val settled = AtomicBoolean(false)
  val delayJob = launch {
    delay(SURVEY_LIST_LOAD_TIMEOUT_MILLIS)
    if (settled.compareAndSet(false, true)) {
      send(emptyList())
    }
  }
  try {
    collect { snapshot ->
      if (!snapshot.isTransientEmptyCache() || settled.get()) {
        settled.set(true)
        delayJob.cancel()
        send(transform(snapshot))
      }
    }
  } finally {
    delayJob.cancel()
  }
}

/** Empty local cache delivered while Firestore retries a timed-out server listen. */
private fun QuerySnapshot.isTransientEmptyCache(): Boolean = isEmpty && metadata.isFromCache
