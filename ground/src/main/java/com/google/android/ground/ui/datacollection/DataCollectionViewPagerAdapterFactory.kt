package com.google.android.ground.ui.datacollection

import androidx.fragment.app.Fragment
import com.google.android.ground.model.task.Task
import com.google.common.collect.ImmutableList
import dagger.assisted.AssistedFactory

@AssistedFactory
interface DataCollectionViewPagerAdapterFactory {
  fun create(
    fragment: Fragment,
    tasks: ImmutableList<Task>,
    dataCollectionViewModel: DataCollectionViewModel
  ): DataCollectionViewPagerAdapter
}
