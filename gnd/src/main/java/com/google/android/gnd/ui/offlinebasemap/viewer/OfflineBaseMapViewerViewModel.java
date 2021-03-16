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

package com.google.android.gnd.ui.offlinebasemap.viewer;

import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.basemap.OfflineBaseMap;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.repository.OfflineBaseMapRepository;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Flowable;
import io.reactivex.subjects.SingleSubject;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * View model for the OfflineAreaViewerFragment. Manges offline area deletions and calculates the
 * storage size of an area on the user's device.
 */
public class OfflineBaseMapViewerViewModel extends AbstractViewModel {

  @Hot(replays = true)
  private final SingleSubject<OfflineBaseMapViewerFragmentArgs> fragmentArgs =
      SingleSubject.create();

  @Hot private final SingleSubject<Nil> removeAreaClicks = SingleSubject.create();

  private final WeakReference<Context> context;
  public LiveData<Double> areaStorageSize;
  public LiveData<String> areaName;
  private final LiveData<OfflineBaseMap> offlineArea;
  @Inject Navigator navigator;
  @Nullable private String offlineAreaId;

  @Inject
  public OfflineBaseMapViewerViewModel(
      OfflineBaseMapRepository offlineBaseMapRepository,
      @ApplicationContext Context context,
      Navigator navigator) {
    this.context = new WeakReference<>(context);
    this.navigator = navigator;
    @Hot
    // We only need to convert this single to a flowable in order to use it with LiveData.
    // It still only contains a single offline area returned by getOfflineArea.
    Flowable<OfflineBaseMap> offlineAreaItemAsFlowable =
        this.fragmentArgs
            .map(OfflineBaseMapViewerFragmentArgs::getOfflineAreaId)
            .flatMap(offlineBaseMapRepository::getOfflineArea)
            .doOnError(throwable -> Timber.e(throwable, "Couldn't render area %s", offlineAreaId))
            .toFlowable();
    this.areaName =
        LiveDataReactiveStreams.fromPublisher(
            offlineAreaItemAsFlowable.map(OfflineBaseMap::getName));
    this.areaStorageSize =
        LiveDataReactiveStreams.fromPublisher(
            offlineAreaItemAsFlowable
                .flatMap(
                    offlineBaseMapRepository::getIntersectingDownloadedTileSourcesOnceAndStream)
                .map(this::tileSourcesToTotalStorageSize));
    this.offlineArea = LiveDataReactiveStreams.fromPublisher(offlineAreaItemAsFlowable);
    disposeOnClear(
        removeAreaClicks
            .map(__ -> Objects.requireNonNull(this.offlineArea.getValue()).getId())
            .flatMapCompletable(offlineBaseMapRepository::deleteArea)
            .doOnError(throwable -> Timber.e(throwable, "Couldn't remove area: %s", offlineAreaId))
            .subscribe(navigator::navigateUp));
  }

  private Double tileSourcesToTotalStorageSize(ImmutableSet<TileSource> tileSources) {
    return stream(tileSources).map(this::tileSourceStorageSize).reduce((x, y) -> x + y).orElse(0.0);
  }

  private double tileSourceStorageSize(TileSource tileSource) {
    Context context1 = context.get();
    if (context1 == null) {
      return 0.0;
    } else {
      File tileFile = new File(context1.getFilesDir(), tileSource.getPath());
      return (double) tileFile.length() / (1024 * 1024);
    }
  }

  /** Returns the offline area associated with this view model. */
  public LiveData<OfflineBaseMap> getOfflineArea() {
    return offlineArea;
  }

  /** Gets a single offline area by the id passed to the OfflineAreaViewerFragment's arguments. */
  public void loadOfflineArea(OfflineBaseMapViewerFragmentArgs args) {
    this.fragmentArgs.onSuccess(args);
    this.offlineAreaId = args.getOfflineAreaId();
  }

  /** Deletes the area associated with this viewmodel. */
  public void removeArea() {
    Timber.d("Removing offline area %s", this.offlineArea.getValue());
    this.removeAreaClicks.onSuccess(Nil.NIL);
  }
}
