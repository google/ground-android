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

import androidx.annotation.VisibleForTesting
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.protobuf.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.groundplatform.android.data.remote.firebase.base.FluentCollectionReference
import org.groundplatform.android.data.remote.firebase.schema.LoiConverter.toLoi
import org.groundplatform.android.proto.AuditInfo
import org.groundplatform.android.proto.LocationOfInterest as LocationOfInterestProto
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import timber.log.Timber

/**
 * Path of field on LOI documents used to differentiate LOIs defined by the organizer vs by data
 * collectors.
 */
const val SOURCE_FIELD = LocationOfInterestProto.SOURCE_FIELD_NUMBER.toString()
/** Path of field on LOI documents representing the creator of the LOI. */
const val OWNER_FIELD = LocationOfInterestProto.OWNER_ID_FIELD_NUMBER.toString()
/** Path of field on LOI documents representing the last modified server timestamp. */
@VisibleForTesting
internal val LAST_MODIFIED_SERVER_SECONDS: FieldPath =
  FieldPath.of(
    LocationOfInterestProto.LAST_MODIFIED_FIELD_NUMBER.toString(),
    AuditInfo.SERVER_TIMESTAMP_FIELD_NUMBER.toString(),
    Timestamp.SECONDS_FIELD_NUMBER.toString(),
  )

/**
 * Documents per query. Deliberately small since geometry complexity varies widely and is unknown
 * before fetching. Any limit bounds memory, and the cost of being small is just more round trips.
 */
@VisibleForTesting internal const val PAGE_SIZE = 250

internal sealed interface LoiQueryScope {
  val source: LocationOfInterestProto.Source
  val ownerUserId: String?

  data object Predefined : LoiQueryScope {
    // Use !=false rather than ==true to not break legacy dev surveys.
    // TODO: Switch to whereEqualTo(true) once legacy dev surveys deleted or migrated.
    // Issue URL: https://github.com/google/ground-android/issues/2375
    override val source = LocationOfInterestProto.Source.IMPORTED
    override val ownerUserId: String? = null
  }

  data object Shared : LoiQueryScope {
    override val source = LocationOfInterestProto.Source.FIELD_DATA
    override val ownerUserId: String? = null
  }

  data class UserDefined(override val ownerUserId: String) : LoiQueryScope {
    override val source = LocationOfInterestProto.Source.FIELD_DATA
  }
}

class LoiCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  fun loi(id: String) = LoiDocumentReference(reference().document(id))

  /** Emits the survey's LOIs in [scope], one page at a time. */
  internal fun fetch(
    survey: Survey,
    scope: LoiQueryScope,
    fromTimestamp: Long?,
  ): Flow<List<LocationOfInterest>> = fetchLois(survey, query(scope), fromTimestamp)

  /** Returns how many LOIs [fetch] would emit for the same [scope]. */
  internal suspend fun count(scope: LoiQueryScope): Long =
    query(scope).count().get(AggregateSource.SERVER).await().count

  private fun query(scope: LoiQueryScope): Query {
    val query = reference().whereEqualTo(SOURCE_FIELD, scope.source.number)
    return scope.ownerUserId?.let { query.whereEqualTo(OWNER_FIELD, it) } ?: query
  }

  /**
   * Emits the LOIs matching [query], a page at a time. Pages are fetched lazily, so a collector
   * that saves each page before asking for the next never holds more than one page in memory.
   */
  private fun fetchLois(
    survey: Survey,
    query: Query,
    fromTimestamp: Long?,
  ): Flow<List<LocationOfInterest>> = flow {
    val orderedQuery =
      if (fromTimestamp == null) {
        query.orderBy(FieldPath.documentId()).limit(PAGE_SIZE.toLong())
      } else {
        query
          .whereGreaterThanOrEqualTo(LAST_MODIFIED_SERVER_SECONDS, fromTimestamp / 1000)
          .orderBy(LAST_MODIFIED_SERVER_SECONDS)
          .limit(PAGE_SIZE.toLong())
      }

    var startAfter: DocumentSnapshot? = null
    var hasMore: Boolean

    do {
      val pagedQuery = orderedQuery.let {
        if (startAfter == null) it else it.startAfter(startAfter)
      }
      val documents = pagedQuery.get().await().documents.takeIf { it.isNotEmpty() } ?: break

      emit(documents.mapNotNull { it.toLoiOrNull(survey) })

      // Counted in documents fetched, not LOIs emitted: an unreadable document is dropped by the
      // conversion above but still takes up a place in the page.
      hasMore = documents.size == PAGE_SIZE
      startAfter = documents.last()
    } while (hasMore)
  }

  private fun DocumentSnapshot.toLoiOrNull(survey: Survey): LocationOfInterest? =
    toLoi(survey, this)
      .onFailure { Timber.e(it, "Invalid LOI $id in remote survey ${survey.id}") }
      .getOrNull()
}
