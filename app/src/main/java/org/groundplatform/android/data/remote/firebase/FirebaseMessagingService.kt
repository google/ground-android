/*
 * Copyright 2023 Google LLC
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

package org.groundplatform.android.data.remote.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.sync.SurveySyncService
import org.groundplatform.android.di.coroutines.ApplicationScope
import timber.log.Timber

const val TOPIC_PREFIX = "/topics/"

private const val LOI_ID_KEY = "loiId"
private const val DELETED_KEY = "deleted"

/**
 * Listens to messages from Firebase Cloud Messaging, and enqueuing re-sync of survey metadata when
 * receiving.
 */
@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

  @Inject lateinit var surveySyncService: SurveySyncService
  @Inject lateinit var localLoiStore: LocalLocationOfInterestStore
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope

  /**
   * Processes new messages, enqueuing a worker to sync the survey with the id specified in the
   * message topic.
   */
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    val surveyId = remoteMessage.from?.removePrefix(TOPIC_PREFIX)
    if (surveyId.isNullOrEmpty()) {
      Timber.w("Invalid topic: ${remoteMessage.from}")
      return
    }
    Timber.v("Message received from topic ${remoteMessage.from}")

    // Dropping it here spares the sync the full read it would take to notice the deletion.
    remoteMessage.data[LOI_ID_KEY]
      ?.takeIf { remoteMessage.data[DELETED_KEY].toBoolean() }
      ?.let { externalScope.launch { localLoiStore.safeDeleteLocalLoi(it) } }

    surveySyncService.enqueueSync(surveyId)
  }

  override fun onNewToken(token: String) {
    // no-op: The server doesn't target single devices or device groups, so there's no need to
    // re-register the app with the server when the token changes.
  }
}
