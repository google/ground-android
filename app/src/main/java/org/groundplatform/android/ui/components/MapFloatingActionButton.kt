package org.groundplatform.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun MapFloatingActionButton(
  modifier: Modifier = Modifier,
  type: MapFloatingActionButtonType,
  onClick: () -> Unit,
) {
  FloatingActionButton(
    modifier = modifier.padding(16.dp),
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    onClick = onClick,
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(type.iconRes),
      contentDescription = null,
      tint = colorResource(type.iconTintRes),
    )
  }
}

sealed class MapFloatingActionButtonType(
  open val iconRes: Int,
  open val iconTintRes: Int = R.color.md_theme_onSurfaceVariant,
) {
  data class OpenNavDrawer(override val iconRes: Int = R.drawable.baseline_menu_24) :
    MapFloatingActionButtonType(iconRes)

  data class MapType(override val iconRes: Int = R.drawable.map_layers) :
    MapFloatingActionButtonType(iconRes)

  // TODO: Consider adding another icon for representing "GPS disabled" state.
  // Issue URL: https://github.com/google/ground-android/issues/1789
  data class LocationLocked(
    override val iconRes: Int = R.drawable.ic_gps_lock,
    override val iconTintRes: Int = R.color.md_theme_primary,
  ) : MapFloatingActionButtonType(iconRes, iconTintRes)

  data class LocationNotLocked(override val iconRes: Int = R.drawable.ic_gps_lock_not_fixed) :
    MapFloatingActionButtonType(iconRes)
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun MapFloatingActionButtonPreview() {
  AppTheme {
    MapFloatingActionButton(type = MapFloatingActionButtonType.LocationLocked(), onClick = {})
  }
}
