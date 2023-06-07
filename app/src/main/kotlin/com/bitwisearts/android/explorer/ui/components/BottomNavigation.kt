package com.bitwisearts.android.explorer.ui.components

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bitwisearts.android.explorer.BottomNavRoutes

/**
 * This is the [BottomAppBar] that controls navigation.
 *
 * @param navController
 *   The [NavController] used to control the navigation of the application via
 *   the [BottomAppBar].
 */
@Composable
fun BottomNavigation(navController: NavController) {
	val items = listOf(
		BottomNavRoutes.ScannerRoute,
		BottomNavRoutes.DevicesRoute)
	BottomAppBar {
		items.forEach { item ->
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
					contentDescription = stringResource(id = item.title))
			}
		}
	}
}