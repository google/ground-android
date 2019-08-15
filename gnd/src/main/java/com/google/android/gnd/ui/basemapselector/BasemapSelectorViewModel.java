package com.google.android.gnd.ui.basemapselector;

import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

/**
 * This view model is responsible for managing state for the {@link BasemapSelectorFragment}.
 * Together, they constitute a basemap selector that users can interact with to select portions of a
 * basemap for offline viewing. Among other things, this view model is responsible for receiving
 * requests to download basemap files and for scheduling those requests with an {@link
 * com.google.android.gnd.workers.FileDownloadWorker}.
 */
public class BasemapSelectorViewModel extends ViewModel {
  @Inject
  BasemapSelectorViewModel() {}

  // TODO: Implement view model.
}
