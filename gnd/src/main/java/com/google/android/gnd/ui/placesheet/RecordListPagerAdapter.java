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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.google.android.gnd.model.Form;

import java.util.List;

public class RecordListPagerAdapter extends FragmentPagerAdapter {
  private Context context;
  private List<Form> forms;

  public RecordListPagerAdapter(FragmentManager fm, Context context, List<Form> forms) {
    super(fm);
    this.context = context;
    this.forms = forms;
  }

  @Override
  public int getCount() {
    return forms.size();
  }

  @Override
  public Fragment getItem(int position) {
    RecordListFragment fragment = new RecordListFragment();
    Bundle args = new Bundle();
    args.putInt(RecordListFragment.FORM_NO, position);
    return fragment;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    // TODO: i18n.
    return forms.get(position).getTitleOrDefault("pt", "Form " + position);
  }
}
