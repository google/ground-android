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

package org.groundplatform.android.persistence.remote.firebase.schema

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.collections.immutable.toPersistentMap
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style as StyleModel
import org.groundplatform.android.persistence.remote.DataStoreException
import org.groundplatform.android.persistence.remote.firebase.protobuf.parseFrom
import org.groundplatform.android.proto.Job as JobProto

/** Converts between Firestore documents and [Job] instances. */
internal object JobConverter {

  @Throws(DataStoreException::class)
  fun toJob(doc: DocumentSnapshot): Job {
    if (!doc.exists()) throw DataStoreException("Missing job")
    val jobProto = JobProto::class.parseFrom(doc)
    val taskMap = jobProto.tasksList.associate { it.id to TaskConverter.toTask(it) }
    val strategy =
      if (taskMap.values.map { it.isAddLoiTask }.none()) {
        Job.DataCollectionStrategy.PREDEFINED
      } else {
        Job.DataCollectionStrategy.MIXED
      }
    return Job(
      id = jobProto.id.ifEmpty { doc.id },
      style = StyleModel(jobProto.style.color),
      name = jobProto.name,
      strategy = strategy,
      tasks = taskMap.toPersistentMap(),
    )
  }
}
