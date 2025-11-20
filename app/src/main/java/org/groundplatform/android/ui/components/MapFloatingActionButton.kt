package org.groundplatform.android.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun MapFloatingActionButton(
  modifier: Modifier = Modifier,
  @DrawableRes iconRes: Int,
  iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  onClick: () -> Unit,
) {
  FloatingActionButton(
    modifier = modifier.padding(16.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    onClick = onClick,
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(iconRes),
      contentDescription = null,
      tint = iconTint,
    )
  }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun MapFloatingActionButtonPreview() {
  AppTheme { MapFloatingActionButton(iconRes = R.drawable.ic_gps_lock_not_fixed, onClick = {}) }
}
