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
package com.google.android.ground.ui.offlinebasemap.selector

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.R
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.persistence.uuid.OfflineUuidGenerator
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.rx.Event
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.common.collect.ImmutableList
import io.reactivex.Flowable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject
import timber.log.Timber

class OfflineAreaSelectorViewModel
@Inject
internal constructor(
  private val offlineAreaRepository: OfflineAreaRepository,
  private val offlineUuidGenerator: OfflineUuidGenerator,
  private val resources: Resources
) : AbstractViewModel() {
  enum class DownloadMessage {
    STARTED,
    FAILURE
  }

  private val downloadClicks: @Hot FlowableProcessor<OfflineArea> = PublishProcessor.create()
  private val remoteTileRequests: @Hot FlowableProcessor<Nil> = PublishProcessor.create()
  val downloadMessages: LiveData<Event<DownloadMessage>>
  val remoteTileSets: Flowable<ImmutableList<TileSet>>
  private var viewport: LatLngBounds? = null

  init {
    downloadMessages =
      LiveDataReactiveStreams.fromPublisher(
        downloadClicks.switchMapSingle { baseMap: OfflineArea ->
          offlineAreaRepository
            .addOfflineAreaAndEnqueue(baseMap)
            .toSingleDefault(DownloadMessage.STARTED)
            .onErrorReturn { e: Throwable -> onEnqueueError(e) }
            .map { Event.create(it) }
        }
      )
    remoteTileSets = remoteTileRequests.switchMapSingle { offlineAreaRepository.tileSets }
  }

  private fun onEnqueueError(e: Throwable): DownloadMessage {
    Timber.e("Failed to add area and queue downloads: %s", e.message)
    return DownloadMessage.FAILURE
  }

  fun setViewport(viewport: LatLngBounds?) {
    this.viewport = viewport
  }

  fun onDownloadClick() {
    viewport?.let {
      downloadClicks.onNext(
        OfflineArea(
          offlineUuidGenerator.generateUuid(),
          OfflineArea.State.PENDING,
          it,
          resources.getString(R.string.unnamed_area)
        )
      )
    }
  }

  fun requestRemoteTileSets() = remoteTileRequests.onNext(Nil.NIL)
}
