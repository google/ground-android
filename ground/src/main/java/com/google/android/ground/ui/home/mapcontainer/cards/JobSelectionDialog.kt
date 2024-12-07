package com.google.android.ground.ui.home.mapcontainer.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.android.ground.R

@Composable
fun JobSelectionDialog(
  selectedJobId: String,
  jobs: List<MapUiData.AddLoiUiData>,
  onJobSelection: (MapUiData.AddLoiUiData) -> Unit,
  onConfirmRequest: (MapUiData.AddLoiUiData) -> Unit,
  onDismissRequest: () -> Unit,
) {
  AlertDialog(
    containerColor = MaterialTheme.colorScheme.surface,
    onDismissRequest = onDismissRequest,
    title = {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(text = stringResource(R.string.add_site), fontSize = 5.em)
      }
    },
    text = {
      Column {
        Spacer(Modifier.height(16.dp))
        jobs.forEach { JobSelectionRow(it, onJobSelection, it.job.id === selectedJobId) }
      }
    },
    confirmButton = {
      Button(
        onClick = { jobs.find { it.job.id == selectedJobId }?.let { onConfirmRequest(it) } },
        contentPadding = PaddingValues(25.dp, 0.dp),
        enabled = selectedJobId != "",
      ) {
        Text(stringResource(R.string.begin))
      }
    },
    dismissButton = {
      OutlinedButton(onClick = { onDismissRequest() }) {
        Text(text = stringResource(R.string.cancel))
      }
    },
  )
}

@Composable
fun JobSelectionRow(
  job: MapUiData.AddLoiUiData,
  onJobSelection: (MapUiData.AddLoiUiData) -> Unit,
  selected: Boolean,
) {
  Row(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onJobSelection(job) }) {
    Text(job.job.name ?: stringResource(R.string.unnamed_job), fontSize = 18.sp)
    if (selected) {
      Icon(
        modifier = Modifier.size(25.dp),
        painter = painterResource(id = R.drawable.baseline_check_24),
        contentDescription = "selected",
        tint = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
