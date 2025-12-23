/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.multiplechoice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.common.Constants
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

const val MULTIPLE_CHOICE_ITEM_TEST_TAG = "multiple choice item test tag"
const val OTHER_INPUT_TEXT_TEST_TAG = "other input test tag"
const val SELECT_MULTIPLE_RADIO_TEST_TAG = "select multiple radio test tag"
const val SELECT_MULTIPLE_CHECKBOX_TEST_TAG = "select multiple checkbox test tag"

/**
 * A composable function that displays a single item in a multiple-choice list.
 *
 * This composable provides a visually consistent and interactive way to present an option within a
 * list of choices where the user can select one or more items. It typically includes a text label
 * and a selectable indicator (e.g., a checkbox).
 */
@Composable
fun MultipleChoiceItemView(
  item: MultipleChoiceItem,
  modifier: Modifier = Modifier,
  isLastIndex: Boolean = false,
  toggleItem: (item: MultipleChoiceItem) -> Unit = {},
  otherValueChanged: (text: String) -> Unit = {},
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(item.isSelected) {
    if (item.isOtherOption) {
      if (item.isSelected) {
        focusRequester.requestFocus()
      } else {
        keyboardController?.hide()
        focusManager.clearFocus()
      }
    }
  }

  Column(modifier = Modifier.testTag(MULTIPLE_CHOICE_ITEM_TEST_TAG)) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      when (item.cardinality) {
        MultipleChoice.Cardinality.SELECT_ONE -> {
          RadioButton(
            modifier = Modifier.testTag(SELECT_MULTIPLE_RADIO_TEST_TAG),
            selected = item.isSelected,
            onClick = { toggleItem(item) },
          )
        }

        MultipleChoice.Cardinality.SELECT_MULTIPLE -> {
          Checkbox(
            modifier = Modifier.testTag(SELECT_MULTIPLE_CHECKBOX_TEST_TAG),
            checked = item.isSelected,
            onCheckedChange = { toggleItem(item) },
          )
        }
      }

      ClickableText(
        text = item.toTextLabel(),
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        onClick = { toggleItem(item) },
      )
    }

    if (item.isOtherOption) {
      Row(modifier = modifier.padding(horizontal = 48.dp)) {
        OtherTextField(modifier, item, focusRequester, otherValueChanged)
      }
    }

    if (!isLastIndex) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@Composable
private fun OtherTextField(
  modifier: Modifier,
  item: MultipleChoiceItem,
  focusRequester: FocusRequester,
  otherValueChanged: (text: String) -> Unit,
) {
  OutlinedTextField(
    supportingText = {
      Row(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = modifier.weight(1f))
        Text(
          "${item.otherText.length} / ${Constants.TEXT_DATA_CHAR_LIMIT}",
          textAlign = TextAlign.End,
        )
      }
    },
    isError = item.otherText.length > Constants.TEXT_DATA_CHAR_LIMIT,
    value = item.otherText,
    textStyle = MaterialTheme.typography.bodyLarge,
    onValueChange = { otherValueChanged(it) },
    modifier = Modifier.testTag(OTHER_INPUT_TEXT_TEST_TAG).focusRequester(focusRequester),
  )
}

@Composable
private fun MultipleChoiceItem.toTextLabel() =
  AnnotatedString(if (isOtherOption) stringResource(id = R.string.other) else option.label)

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SelectOneListItemPreview() {
  AppTheme {
    MultipleChoiceItemView(
      item =
        MultipleChoiceItem(
          Option(id = "id", code = "code", label = "Option 1"),
          cardinality = MultipleChoice.Cardinality.SELECT_ONE,
          isSelected = false,
        )
    )
  }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SelectMultipleListItemPreview() {
  AppTheme {
    MultipleChoiceItemView(
      item =
        MultipleChoiceItem(
          Option(id = "id", code = "code", label = "Option 2"),
          cardinality = MultipleChoice.Cardinality.SELECT_MULTIPLE,
          isSelected = false,
        )
    )
  }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SelectOneOtherListItemPreview() {
  AppTheme {
    MultipleChoiceItemView(
      item =
        MultipleChoiceItem(
          Option(id = "id", code = "code", label = "Option 3"),
          cardinality = MultipleChoice.Cardinality.SELECT_ONE,
          isSelected = true,
          isOtherOption = true,
          otherText = "Other text",
        )
    )
  }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun SelectMultipleOtherListItemPreview() {
  AppTheme {
    MultipleChoiceItemView(
      item =
        MultipleChoiceItem(
          Option(id = "id", code = "code", label = "Option 4"),
          cardinality = MultipleChoice.Cardinality.SELECT_MULTIPLE,
          isSelected = true,
          isOtherOption = true,
          otherText = "Other text",
        )
    )
  }
}
