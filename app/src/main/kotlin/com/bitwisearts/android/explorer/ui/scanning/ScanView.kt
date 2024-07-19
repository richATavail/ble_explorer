package com.bitwisearts.android.explorer.ui.scanning

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.explorer.ExplorerApp
import com.bitwisearts.android.explorer.R
import com.bitwisearts.android.explorer.ble.ExplorerScan
import com.bitwisearts.android.explorer.ui.components.AdvertisementView
import kotlinx.coroutines.launch

/**
 * The primary [Composable] view for scanning for BLE devices and displaying
 * their advertisements.
 */
@Composable
fun ScannerView (
	navController: NavController,
	lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
	viewModel: ScannerViewModel = viewModel())
{
	// If `lifecycleOwner` changes, dispose and reset the effect
	DisposableEffect(lifecycleOwner) {
		// Create an observer that triggers our remembered callbacks
		// for sending analytics events
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_STOP) {
				viewModel.scanRequest.found.clear()
				viewModel.viewModelScope.launch {
					ExplorerApp.app.bleScanManager.requestCancelScan()
				}
			}
		}

		// Add the observer to the lifecycle
		lifecycleOwner.lifecycle.addObserver(observer)

		// When the effect leaves the Composition, remove the observer
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
		}
	}
	Column(modifier = Modifier.fillMaxSize())
	{
		Row(
			modifier = Modifier
				.padding(all = 20.dp)
				.fillMaxWidth())
		{
			val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
			val bluetoothEnabled by
				viewModel.bluetoothEnabled.collectAsStateWithLifecycle()
			Text(
				text = stringResource(R.string.ble_devices),
				fontSize = 30.sp,
				modifier = Modifier.padding(horizontal = 18.dp))
			if (isScanning && bluetoothEnabled)
			{
				CircularProgressIndicator()
			}
			else
			{
				viewModel.cancelScan()
				Button(
					enabled = bluetoothEnabled,
					onClick = {
						viewModel.scanRequest.found.clear()
						viewModel.startScan()
					}
				)
				{
					Text(text = stringResource(R.string.scan))
				}
			}
		}
		Advertisements(navController, viewModel.scanRequest.found)
	}
}

/**
 * The [Composable] that lists all the [Advertisement]s that have been
 * discovered via a BLE Scan.
 *
 * @param navController
 *   The [NavController] used for navigation.
 * @param advertisements
 *   The list of [Advertisement]s to add to the screen.
 */
@Composable
fun Advertisements(
	navController: NavController,
	advertisements: List<Advertisement>)
{
	LazyColumn(modifier = Modifier.padding(start = 18.dp))
	{
		items(advertisements) {
			AdvertisementView(navController, it)
		}
	}
}

/**
 * The [ViewModel] for the [ScannerView].
 *
 * @author Richard Arriaga.
 */
class ScannerViewModel: ViewModel()
{
	/** The [ExplorerScan] used for scanning for BLE devices. */
	val scanRequest = ExplorerScan(
		4000,
		false,
		ExplorerApp.app.bleScanManager,
		viewModelScope)
	{
		Log.e("ScanView", it.displayString)
	}

	/**
	 * `true` indicates the app is performing a BLE scan; `false` otherwise.
	 */
	val isScanning get() = ExplorerApp.app.bleScanManager.isScanning

	/** `true` indicates Bluetooth is enabled; `false` otherwise. */
	val bluetoothEnabled get() = ExplorerApp.app.bleScanManager.isBleIsEnabled

	/** Start a BLE scan. */
	fun startScan ()
	{
		ExplorerApp.app.bleScanManager.requestScan(scanRequest)
	}

	fun cancelScan ()
	{
		viewModelScope.launch {
			ExplorerApp.app.bleScanManager.requestCancelScan()
		}
	}

	override fun onCleared()
	{
		Log.w(
			"ScannerViewModel",
			"+++++++++ Has Been Cleared!! ++++++++")
	}
}