package com.google.android.gnd.ui.basemapselector;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;

import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.ui.map.Extent;
import com.google.android.gnd.workers.FileDownloadWorkManager;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;

import static java8.util.stream.StreamSupport.stream;

/**
 * This view model is responsible for managing state for the {@link BasemapSelectorFragment}.
 * Together, they constitute a basemap selector that users can interact with to select portions of a
 * basemap for offline viewing. Among other things, this view model is responsible for receiving
 * requests to download basemap files and for scheduling those requests with an {@link
 * com.google.android.gnd.workers.FileDownloadWorker}.
 */
public class BasemapSelectorViewModel extends ViewModel {
  private static final String TAG = BasemapSelectorViewModel.class.getName();

  private final Flowable<ImmutableSet<Tile>> tilesStream;
  private final LiveData<ImmutableSet<Tile>> tiles;
  private final HashSet<Extent> extents = new HashSet<>();
  private final PublishSubject<HashSet<Extent>> extentsSubject = PublishSubject.create();
  private LiveData<HashSet<Extent>> selectedExtents;
  private final DataRepository dataRepository;
  private final FileDownloadWorkManager downloadWorkManager;

  @Inject
  BasemapSelectorViewModel(
      DataRepository dataRepository, FileDownloadWorkManager downloadWorkManager) {
    this.dataRepository = dataRepository;
    this.downloadWorkManager = downloadWorkManager;

    this.tilesStream = this.dataRepository.getTilesOnceAndStream();

    this.tiles = LiveDataReactiveStreams.fromPublisher(tilesStream);

    this.selectedExtents =
        LiveDataReactiveStreams.fromPublisher(
            extentsSubject.toFlowable(BackpressureStrategy.LATEST));
  }

  public void updateExtentSelections(Extent extent) {
    switch (extent.getState()) {
      case PENDING_DOWNLOAD:
        this.extents.add(extent);
        break;
      case PENDING_REMOVAL:
        this.extents.add(extent);
        break;
      default:
        // TODO: Remove temporary hack to remove selected extents.
        // For whatever reason, my equals override on extents is not working for hashset removal.
        // For now, we just remove the possible states that are in the set.
        this.extents.remove(extent.toBuilder().setState(Extent.State.PENDING_DOWNLOAD).build());
        this.extents.remove(extent.toBuilder().setState(Extent.State.PENDING_REMOVAL).build());
    }

    extentsSubject.onNext(extents);
  }

  private void applyExtentChange(Extent extent) {
    switch (extent.getState()) {
      case PENDING_REMOVAL:
        downloadWorkManager
            .enqueueRemovalWorker(extent.getId())
            .subscribe(() -> Log.d(TAG, "Removal worker queued"));
        break;
      case PENDING_DOWNLOAD:
        downloadWorkManager
            .enqueueFileDownloadWorker(extent.getId())
            .subscribe(() -> Log.d(TAG, "Download worker queued"));
        break;
      default:
        // Do nothing.
    }
  }

  public void applyExtentChanges() {
    stream(this.extents).forEach(this::applyExtentChange);
    extents.clear();
  }

  public LiveData<HashSet<Extent>> getSelectedExtents() {
    return this.selectedExtents;
  }

  public LiveData<ImmutableSet<Tile>> getTiles() {
    return tiles;
  }
}
