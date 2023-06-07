package com.bitwisearts.android.explorer


import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bitwisearts.android.explorer.ui.connected.DeviceView
import com.bitwisearts.android.explorer.ui.connected.DevicesView
import com.bitwisearts.android.explorer.ui.scanning.ScannerView

sealed class BottomNavRoutes constructor(
	val title: Int,
	val icon: Int,
	val route: String)
{
	object ScannerRoute: BottomNavRoutes(
		R.string.ble_scan,
		R.drawable.white_ble_searching,
		"ble_scanner")

	object DevicesRoute: BottomNavRoutes(
		R.string.connected_devices,
		R.drawable.white_ble_connected,
		"ble_connected_devices")
}

object DeviceRoute
{
	const val macParam = "mac_address"

	const val route = "device/{$macParam}"

	fun navigate (
		navController: NavController,
		macAddress: String)
	{
		navController.navigate("device/$macAddress")
	}
}

/**
 * The [Composable] [NavHost] that manages navigation.
 * 
 * @param modifier
 *   The [Modifier] to be applied to the layout.
 * @param navController
 *   The [NavHostController] for the [NavHost].
 * @param startDestination
 *   The route for the staring [Composable] destination.
 */
@Composable
fun AppNavigationGraph(
	modifier: Modifier = Modifier,
	navController: NavHostController = rememberNavController(),
	startDestination: String = BottomNavRoutes.ScannerRoute.route
) {
	NavHost(
		modifier = modifier,
		navController = navController,
		startDestination = startDestination
	) {
		composable(BottomNavRoutes.ScannerRoute.route)
		{
			ScannerView(navController)
		}
		composable(BottomNavRoutes.DevicesRoute.route)
		{
			DevicesView(navController)
		}
		composable(DeviceRoute.route)
		{
			DeviceView(
				it.arguments?.getString(DeviceRoute.macParam) ?: "")
		}
	}
}
