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

package com.google.android.gnd.ui.tos;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.repository.TermsOfServiceRepository;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidTest;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class TermsOfServiceViewModelTest extends BaseHiltTest {

  @BindValue @Mock Navigator mockNavigator;
  @BindValue @Mock TermsOfServiceRepository mockRepository;

  @Inject TermsOfServiceViewModel viewModel;

  @Test
  public void testOnButtonClicked() {
    viewModel.onButtonClicked();
    Mockito.verify(mockRepository, Mockito.times(1)).setTermsOfServiceAccepted(true);
    Mockito.verify(mockNavigator, Mockito.times(1))
        .navigate(HomeScreenFragmentDirections.showHomeScreen());
  }

  @Test
  public void testTermsOfServiceText() {
    viewModel.setTermsOfServiceText("Terms Text");
    assertThat(viewModel.getTermsOfServiceText()).isEqualTo("Terms Text");
  }
}
