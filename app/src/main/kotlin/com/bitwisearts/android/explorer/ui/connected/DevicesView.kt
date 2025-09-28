package com.bitwisearts.android.explorer.ui.connected

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitwisearts.android.ble.connection.BleDeviceManager
import com.bitwisearts.android.explorer.ui.components.AdvertisementCollapsed

/**
 * The primary [Composable] view showing devices stored in
 * [BleDeviceManager.devices].
 */
@Composable
fun DevicesView (
	navController: NavController,
	viewModel: DevicesViewModel = viewModel()
) {
	Column {
		Text("Advertisements", fontWeight = FontWeight.Bold)
		BleDeviceManager.advertisements.values.forEach {
			AdvertisementCollapsed(
				it.address, it.deviceName
			)
		}
		Text("Devices", fontWeight = FontWeight.Bold)
		BleDeviceManager.devices.values.forEach {
			Text(it.macAddress)
		}
	}
}

/**
 * The [ViewModel] for the [DevicesView].
 *
 * @author Richard Arriaga.
 */
class DevicesViewModel: ViewModel()
{
	override fun onCleared()
	{
		Log.w(
			"DevicesView",
			"+++++++++ Has Been Cleared!! ++++++++")
	}
}