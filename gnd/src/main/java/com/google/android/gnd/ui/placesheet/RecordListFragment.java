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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RecordListFragment extends Fragment {
  private static final String FORM_NO = "formNo";

  private LinearLayout layout;

  static RecordListFragment newInstance(int formId) {
    RecordListFragment fragment = new RecordListFragment();
    Bundle args = new Bundle();
    args.putInt(FORM_NO, formId);
    fragment.setArguments(args);
    return fragment;
  }

  //  private DataSheetPresenter dataSheetPresenter;
  //  private Form form;
  int getFormId() {
    return getArguments().getInt(FORM_NO);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    //    DataSheetPresenter dataSheetPresenter = ((MainActivity) context).getMainPresenter()
    //        .getDataSheetPresenter();
    //    form = (Form) args.get(FORM);

  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    layout = new LinearLayout(getActivity());
    layout.setOrientation(LinearLayout.VERTICAL);
    TextView text = new TextView(getActivity());
    text.setTextSize(28);
    text.setText("Page " + getFormId());
    layout.addView(text);
    return layout;
  }
}
