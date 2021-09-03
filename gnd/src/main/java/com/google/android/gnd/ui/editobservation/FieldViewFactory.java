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

package com.google.android.gnd.ui.editobservation;

import static com.google.android.gnd.ui.util.ViewUtil.assignGeneratedId;

import android.widget.LinearLayout;
import androidx.annotation.LayoutRes;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import com.google.android.gnd.BR;
import com.google.android.gnd.R;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.ui.common.ViewModelFactory;
import javax.inject.Inject;

/** Inflates a new view and generates a view model for a given {@link Field.Type}. */
public class FieldViewFactory {

  @Inject Fragment fragment;
  @Inject ViewModelFactory viewModelFactory;

  @Inject
  FieldViewFactory() {}

  private static Class<? extends AbstractFieldViewModel> getViewModelClass(Field.Type fieldType) {
    switch (fieldType) {
      case TEXT_FIELD:
        return TextFieldViewModel.class;
      case MULTIPLE_CHOICE:
        return MultipleChoiceFieldViewModel.class;
      case PHOTO:
        return PhotoFieldViewModel.class;
      case NUMBER:
        return NumberFieldViewModel.class;
      case DATE:
        return DateFieldViewModel.class;
      case TIME:
        return TimeFieldViewModel.class;
      default:
        throw new IllegalArgumentException("Unsupported field type: " + fieldType);
    }
  }

  @LayoutRes
  private static int getLayoutId(Field.Type fieldType) {
    switch (fieldType) {
      case TEXT_FIELD:
        return R.layout.text_input_field;
      case MULTIPLE_CHOICE:
        return R.layout.multiple_choice_input_field;
      case PHOTO:
        return R.layout.photo_input_field;
      case NUMBER:
        return R.layout.number_input_field;
      case DATE:
        return R.layout.date_input_field;
      case TIME:
        return R.layout.time_input_field;
      default:
        throw new IllegalArgumentException("Unsupported field type: " + fieldType);
    }
  }

  /**
   * Inflates the view, generates a new view model and binds to the {@link ViewDataBinding}.
   *
   * @param fieldType Type of the field
   * @param root Parent layout
   * @return {@link ViewDataBinding}
   */
  ViewDataBinding addFieldView(Field.Type fieldType, LinearLayout root) {
    ViewDataBinding binding =
        DataBindingUtil.inflate(fragment.getLayoutInflater(), getLayoutId(fieldType), root, true);
    binding.setLifecycleOwner(fragment);
    binding.setVariable(BR.viewModel, viewModelFactory.create(getViewModelClass(fieldType)));
    assignGeneratedId(binding.getRoot());
    return binding;
  }
}
