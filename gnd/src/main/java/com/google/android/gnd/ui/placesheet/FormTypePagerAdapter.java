/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.ui.placesheet;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.google.android.gnd.model.Form;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

public class FormTypePagerAdapter extends FragmentPagerAdapter {
  private List<Form> forms;

  @Inject
  public FormTypePagerAdapter(FragmentManager fm) {
    super(fm);
    this.forms = Arrays.asList(
      Form.newBuilder().putTitle("pt", "Form One").build(),
      Form.newBuilder().putTitle("pt", "Form Two").build()
    );
  }

  public void setForms(List<Form> forms) {
    this.forms = forms;
  }

  @Override
  public int getCount() {
    return forms.size();
  }

  @Override
  public Fragment getItem(int position) {
    return RecordListFragment.newInstance(position);
  }

  @Override
  public CharSequence getPageTitle(int position) {
    // TODO: i18n.
    return forms.get(position).getTitleOrDefault("pt", "Form " + position);
  }
}
