package com.bitwisearts.android.explorer


import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bitwisearts.android.explorer.ble.peripheral.SampleBlePeripheral
import com.bitwisearts.android.explorer.ui.connected.DeviceView
import com.bitwisearts.android.explorer.ui.connected.DevicesView
import com.bitwisearts.android.explorer.ui.peripheral.PeripheralView
import com.bitwisearts.android.explorer.ui.scanning.ScannerView

/**
 * The routes for the bottom navigation items.
 *
 * @property title
 *   The string resource ID for the title of the route.
 * @property icon
 *   The drawable resource ID for the icon of the route.
 * @property route
 *   The string route for the navigation graph.
 *
 * @author Richard Arriaga
 */
enum class BottomNavRoute(
	val title: Int,
	val icon: Int,
	val route: String)
{
	/**
	 * The [BottomNavRoute] representing  the home page route.
	 */
	HomeRoute(
		R.string.app_name,
		R.drawable.white_home,
		"home"),

	/**
	 * The [BottomNavRoute] representing the scanner page route.
	 */
	ScannerRoute(
		R.string.ble_scan,
		R.drawable.white_ble_searching,
		"ble_scanner"),

	/**
	 * The [BottomNavRoute] representing the connected devices page route.
	 */
	DevicesRoute(
		R.string.connected_devices,
		R.drawable.white_ble_connected,
		"ble_connected_devices"),

	/**
	 * The [BottomNavRoute] representing the peripheral mode page route. This
	 * is where a device can advertise itself as a [SampleBlePeripheral]. It
	 * provides utilities for controlling the [SampleBlePeripheral].
	 */
	PeripheralAdvertiseRoute(
		R.string.peripheral_mode,
		R.drawable.white_ble_peripheral,
		"ble_peripheral"),

	/**
	 * The [BottomNavRoute] representing the central mode page route. This
	 * is where a device can act as a central device to connect to
	 * [SampleBlePeripheral]s and interact with them.
	 */
	PeripheralCentralRoute(
		R.string.central_mode,
		R.drawable.white_ble_central,
		"ble_central")
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
	startDestination: String = BottomNavRoute.HomeRoute.route
) {
	NavHost(
		modifier = modifier,
		navController = navController,
		startDestination = startDestination
	) {
		composable(BottomNavRoute.HomeRoute.route)
		{
			val context = LocalContext.current
			Column(
				modifier = Modifier.padding(20.dp).fillMaxWidth()
			) {
				Row {
					Text("Home!")
				}
				Row {
					Button(
						onClick = {
								val intent = Intent().apply {
									action =
										ACTION_APPLICATION_DETAILS_SETTINGS
									data =
										Uri.fromParts(
											"package",
											context.packageName,
											null
										)
									addFlags(FLAG_ACTIVITY_NEW_TASK)
								}
								context.startActivity(intent)

						}
					) {
						Text("Welcome to the BLE Explorer App!")
					}
				}
			}
		}
		composable(BottomNavRoute.ScannerRoute.route)
		{
			ScannerView(navController)
		}
		composable(BottomNavRoute.DevicesRoute.route)
		{
			DevicesView(navController)
		}
		composable(DeviceRoute.route)
		{
			DeviceView(
				it.arguments?.getString(DeviceRoute.macParam) ?: "")
		}
		composable(BottomNavRoute.PeripheralAdvertiseRoute.route)
		{
			PeripheralView()
		}
		composable(BottomNavRoute.PeripheralCentralRoute.route)
		{
			// TODO
		}
	}
}
