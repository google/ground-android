/*
 * Copyright 2021 Google LLC
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
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;
import java.io.File;
import java.lang.ref.WeakReference;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * View model for the OfflineAreaViewerFragment. Manges offline area deletions and calculates the
 * storage size of an area on the user's device.
 */
public class OfflineBaseMapViewerViewModel extends AbstractViewModel {

  @Hot(replays = true)
  private final BehaviorProcessor<OfflineBaseMapViewerFragmentArgs> argsProcessor;

  @Hot private final PublishProcessor<Nil> removeClickProcessor;

  private final OfflineBaseMapRepository offlineBaseMapRepository;
  private final WeakReference<Context> context;
  public LiveData<Double> areaStorageSize;
  public LiveData<String> areaName;
  private final LiveData<OfflineBaseMap> offlineArea;

  @Inject
  public OfflineBaseMapViewerViewModel(
      OfflineBaseMapRepository offlineBaseMapRepository, @ApplicationContext Context context) {
    this.argsProcessor = BehaviorProcessor.create();
    this.removeClickProcessor = PublishProcessor.create();
    this.offlineBaseMapRepository = offlineBaseMapRepository;
    this.context = new WeakReference<>(context);
    @Hot
    Flowable<OfflineBaseMap> offlineAreaInternal =
        this.argsProcessor.switchMap(
            args ->
                this.offlineBaseMapRepository
                    .getOfflineArea(args.getOfflineAreaId())
                    .toFlowable()
                    .doOnError(
                        throwable ->
                            Timber.e(
                                throwable, "Couldn't render area: %s", args.getOfflineAreaId())));
    this.areaName =
        LiveDataReactiveStreams.fromPublisher(offlineAreaInternal.map(OfflineBaseMap::getName));
    this.areaStorageSize =
        LiveDataReactiveStreams.fromPublisher(
            offlineAreaInternal
                .flatMap(
                    offlineBaseMapRepository::getIntersectingDownloadedTileSourcesOnceAndStream)
                .map(this::tileSourcesToTotalStorageSize));
    this.offlineArea = LiveDataReactiveStreams.fromPublisher(offlineAreaInternal);
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

  /**
   * Removes the offline area associated with this viewmodel from the device by removing all tile
   * sources that are not included in other areas and removing the area from the db.
   *
   * <p>Terminates the upstream click processor so that the resulting stream completes.
   */
  @Hot(terminates = true)
  public Completable onRemoveArea() {
    return this.removeClickProcessor.flatMapCompletable(
        __ -> {
          if (this.offlineArea.getValue() == null) {
            return Completable.error(new Throwable("Could not remove nonexistent area."));
          }

          this.removeClickProcessor.onComplete();
          return offlineBaseMapRepository.deleteArea(this.offlineArea.getValue().getId());
        });
  }

  /** Returns the offline area associated with this view model. */
  public LiveData<OfflineBaseMap> getOfflineArea() {
    return offlineArea;
  }

  /** Gets a single offline area by the id passed to the OfflineAreaViewerFragment's arguments. */
  public void loadOfflineArea(OfflineBaseMapViewerFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }

  /** Deletes the area associated with this viewmodel. */
  public void removeArea() {
    Timber.d("Removing offline area %s", this.offlineArea.getValue());
    this.removeClickProcessor.onNext(Nil.NIL);
  }
}
