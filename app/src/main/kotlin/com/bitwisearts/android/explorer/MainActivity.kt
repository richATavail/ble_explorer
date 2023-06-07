package com.bitwisearts.android.explorer

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.rememberNavController
import com.bitwisearts.android.explorer.ui.components.BottomNavigation
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitwisearts.android.explorer.ui.theme.Ble_explorerTheme

/**
 * The sole [Activity][ComponentActivity] that contains this application's view.
 *
 * @author Richard Arriaga
 */
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity()
{
	/**
	 * The [BroadcastReceiver] for watching changes to
	 * [Bluetooth][BluetoothAdapter.ACTION_STATE_CHANGED].
	 */
	private val bleStatusReceiver = object: BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			val state = intent.getIntExtra(
				BluetoothAdapter.EXTRA_STATE,
				BluetoothAdapter.ERROR)
			val message = when(state)
			{
				BluetoothAdapter.STATE_OFF ->
				{
					ExplorerApp.app.bleScanManager.notifyBleEnabled(false)
					"turned off bluetooth!"
				}
				BluetoothAdapter.STATE_TURNING_OFF ->
				{
					ExplorerApp.app.bleScanManager.notifyBleEnabled(false)
					"bluetooth turning off!"
				}
				BluetoothAdapter.STATE_ON ->
				{
					ExplorerApp.app.bleScanManager.notifyBleEnabled(true)
					"turned on bluetooth"
				}
				BluetoothAdapter.STATE_TURNING_ON ->
				{
					ExplorerApp.app.bleScanManager.notifyBleEnabled(true)
					"bluetooth is turning on"
				}
				else ->
					"Something happened with bluetooth that can't be figured out"
			}
			Log.d("MainActivity", "BLE: $message")
			Toast.makeText(
				this@MainActivity,
				message,
				Toast.LENGTH_LONG).show()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			val mainViewModel = viewModel<MainViewModel>()

			// Check to see if we have the appropriate permissions.
			val permissionsToRequest = mainViewModel.permissionsToRequest
			var permissionsAsked by remember { mutableStateOf(false) }
			val permissionsRequestLauncher = rememberLauncherForActivityResult(
				ActivityResultContracts.RequestMultiplePermissions())
			{
				mainViewModel.permissions.forEachIndexed { i, permission ->
					mainViewModel.processPermissionResult(
						permission, it[permission.permission] == true)
					if (i == mainViewModel.permissions.size - 1)
					{
						// TODO this doesn't quite line up the way I want if
						//  permissions are denied. I need the dialogs to pop up
						//  below at the right times. Perhaps it is better to
						//  setup a screen flow for permissions at app start?
						permissionsAsked = true
					}
				}
			}
			SideEffect {
				permissionsRequestLauncher.launch(mainViewModel.permissionNames)
			}
			// TODO get permission dialogs in order! Not quite working right.
//			if (permissionsAsked)
//			{
//				permissionsToRequest.forEach {
//					it.PermissionDialog(
//						isPermanentlyDeclined =
//						!shouldShowRequestPermissionRationale(it.permission),
//						onDismiss = {
//							permissionsToRequest.removeFirst()
//						},
//						gotoSettings = this@MainActivity::gotoAppSettings,
//						requestPermission = {
//							permissionsToRequest.removeFirst()
//							permissionsRequestLauncher
//								.launch(arrayOf(it.permission))
//						})
//				}
//			}
//			// Check to see if we have the appropriate permissions.
//			ConditionallyRequestPermission(Manifest.permission.BLUETOOTH_SCAN)
//			{
//				if (!it)
//				{
//					Toast.makeText(
//						ExplorerApp.app,
//						"Did not grant permission to use ble scan.",
//						Toast.LENGTH_SHORT
//					).show()
//				}
//			}
//			ConditionallyRequestPermission(Manifest.permission.BLUETOOTH_CONNECT)
//			{
//				if (!it)
//				{
//					Toast.makeText(
//						ExplorerApp.app,
//						"Did not grant permission to use ble connect.",
//						Toast.LENGTH_SHORT
//					).show()
//				}
//			}
			val navController = rememberNavController()
			Ble_explorerTheme {
				Scaffold(
					bottomBar = { BottomNavigation(navController = navController) }
				) {
					AppNavigationGraph(
						modifier = Modifier.padding(it),
						navController = navController)
				}
			}
		}
	}

	override fun onResume()
	{
		super.onResume()
		registerReceiver(
			bleStatusReceiver,
			IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
	}

	override fun onPause()
	{
		super.onPause()
		unregisterReceiver(bleStatusReceiver)
	}

	/**
	 * Open the device settings and navigate to the settings page for this
	 * [explorer application][ExplorerApp].
	 */
	fun gotoAppSettings ()
	{
		Intent(
			Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
			Uri.fromParts("package", packageName, null)).apply {
			startActivity(this)
		}
	}
}

///**
// * Conditionally request an app [permission][Manifest.permission].
// *
// * @param permission
// *   The String representation of the permission to request.
// * @param resultHandler
// *   Accepts `true` if the permission is granted; `false` if not.
// */
//@Composable
//fun ConditionallyRequestPermission (
//	permission: String,
//	resultHandler: (Boolean) -> Unit)
//{
//	val permissionFlag =
//		ContextCompat.checkSelfPermission(ExplorerApp.app, permission)
//	if (permissionFlag != PackageManager.PERMISSION_GRANTED)
//	{
//		val launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> = rememberLauncherForActivityResult(
//			ActivityResultContracts.RequestMultiplePermissions(), resultHandler)
//		SideEffect {
//			launcher.launch(arrayOf(permission))
//		}
//	}
//	else
//	{
//		resultHandler(true)
//	}
//}