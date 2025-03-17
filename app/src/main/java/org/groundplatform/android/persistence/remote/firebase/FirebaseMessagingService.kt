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

package org.groundplatform.android.persistence.remote.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.persistence.sync.SurveySyncService
import timber.log.Timber

const val TOPIC_PREFIX = "/topics/"

/**
 * Listens to messages from Firebase Cloud Messaging, and enqueuing re-sync of survey metadata when
 * receiving.
 */
@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

  @Inject lateinit var surveySyncService: SurveySyncService

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
    surveySyncService.enqueueSync(surveyId)
  }

  override fun onNewToken(token: String) {
    // no-op: The server doesn't target single devices or device groups, so there's no need to
    // re-register the app with the server when the token changes.
  }
}
