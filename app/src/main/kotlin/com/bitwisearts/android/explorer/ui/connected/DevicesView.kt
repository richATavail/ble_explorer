package com.bitwisearts.android.explorer.ui.connected

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitwisearts.android.ble.connection.BleDeviceManager

/**
 * The primary [Composable] view showing devices stored in
 * [BleDeviceManager.devices].
 */
@Composable
fun DevicesView (
	navController: NavController,
	viewModel: DevicesViewModel = viewModel()
)
{
	Text(text = "Still gotta build this!!!")
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