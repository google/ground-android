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
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.basemap.tile.TileSet;
import com.google.android.gnd.repository.OfflineAreaRepository;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
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
public class OfflineAreaViewerViewModel extends AbstractViewModel {

  @Hot(replays = true)
  private final PublishSubject<OfflineAreaViewerFragmentArgs> fragmentArgs =
      PublishSubject.create();

  @Hot private final PublishSubject<Nil> removeAreaClicks = PublishSubject.create();

  private final WeakReference<Context> context;
  private final LiveData<OfflineArea> offlineArea;
  public LiveData<Double> areaStorageSize;
  public LiveData<String> areaName;
  @Inject Navigator navigator;
  @Nullable private String offlineAreaId;

  @Inject
  public OfflineAreaViewerViewModel(
      OfflineAreaRepository offlineAreaRepository,
      @ApplicationContext Context context,
      Navigator navigator) {
    this.context = new WeakReference<>(context);
    this.navigator = navigator;
    @Hot
    // We only need to convert this single to a flowable in order to use it with LiveData.
    // It still only contains a single offline area returned by getOfflineArea.
    Flowable<OfflineArea> offlineAreaItemAsFlowable =
        this.fragmentArgs
            .map(OfflineAreaViewerFragmentArgs::getOfflineAreaId)
            .flatMapSingle(offlineAreaRepository::getOfflineArea)
            .doOnError(throwable -> Timber.e(throwable, "Couldn't render area %s", offlineAreaId))
            .toFlowable(BackpressureStrategy.LATEST);
    this.areaName =
        LiveDataReactiveStreams.fromPublisher(offlineAreaItemAsFlowable.map(OfflineArea::getName));
    this.areaStorageSize =
        LiveDataReactiveStreams.fromPublisher(
            offlineAreaItemAsFlowable
                .flatMap(offlineAreaRepository::getIntersectingDownloadedTileSetsOnceAndStream)
                .map(this::tileSetsToTotalStorageSize));
    this.offlineArea = LiveDataReactiveStreams.fromPublisher(offlineAreaItemAsFlowable);
    disposeOnClear(
        removeAreaClicks
            .map(__ -> Objects.requireNonNull(this.offlineArea.getValue()).getId())
            .flatMapCompletable(offlineAreaRepository::deleteOfflineArea)
            .doOnError(throwable -> Timber.e(throwable, "Couldn't remove area: %s", offlineAreaId))
            .subscribe(navigator::navigateUp));
  }

  private Double tileSetsToTotalStorageSize(ImmutableSet<TileSet> tileSets) {
    return stream(tileSets).map(this::tileSetStorageSize).reduce((x, y) -> x + y).orElse(0.0);
  }

  private double tileSetStorageSize(TileSet tileSet) {
    Context context1 = context.get();
    if (context1 == null) {
      return 0.0;
    } else {
      File tileFile = new File(context1.getFilesDir(), tileSet.getPath());
      return (double) tileFile.length() / (1024 * 1024);
    }
  }

  /** Returns the offline area associated with this view model. */
  public LiveData<OfflineArea> getOfflineArea() {
    return offlineArea;
  }

  /** Gets a single offline area by the id passed to the OfflineAreaViewerFragment's arguments. */
  public void loadOfflineArea(OfflineAreaViewerFragmentArgs args) {
    this.fragmentArgs.onNext(args);
    this.offlineAreaId = args.getOfflineAreaId();
  }

  /** Deletes the area associated with this viewmodel. */
  public void removeArea() {
    Timber.d("Removing offline area %s", this.offlineArea.getValue());
    this.removeAreaClicks.onNext(Nil.NIL);
  }
}
