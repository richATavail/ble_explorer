package com.bitwisearts.android.explorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.bitwisearts.android.explorer.BottomNavRoute
import com.bitwisearts.android.explorer.ui.theme.BleExplorerTheme

/**
 * This is the [BottomAppBar] that controls navigation.
 *
 * @param navController
 *   The [NavController] used to control the navigation of the application via
 *   the [BottomAppBar].
 */
@Composable
fun BottomNavigation(navController: NavController) {
	BottomAppBar {
		androidx.compose.foundation.layout.Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceEvenly
		) {
			BottomNavRoute.entries.forEach { item ->
				IconButton(onClick = {
					navController.navigate(item.route) {
						navController.graph.startDestinationRoute?.let { route ->
							popUpTo(route) {
								saveState = true
							}
						}
						launchSingleTop = true
						restoreState = true
					}
				}) {
					Icon(
						painter = painterResource(id = item.icon),
						contentDescription = stringResource(id = item.title)
					)
				}
			}
		}
	}
}

@Preview
@Composable
private fun BottomNavigationPreview() {
	BleExplorerTheme {
		BottomNavigation(navController = rememberNavController())
	}
}