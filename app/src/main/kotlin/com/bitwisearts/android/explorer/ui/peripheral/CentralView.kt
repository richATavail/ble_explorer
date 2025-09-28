package com.bitwisearts.android.explorer.ui.peripheral

import android.bluetooth.BluetoothGattService
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.connection.BleConnectionState.CONNECTED
import com.bitwisearts.android.ble.connection.BleConnectionState.DISCONNECTED
import com.bitwisearts.android.ble.connection.BleDeviceManager
import com.bitwisearts.android.ble.connection.ConnectionState
import com.bitwisearts.android.explorer.ExplorerApp
import com.bitwisearts.android.explorer.R
import com.bitwisearts.android.explorer.ble.ExplorerScan
import com.bitwisearts.android.explorer.ble.peripheral.SampleBleDevice
import com.bitwisearts.android.explorer.ble.peripheral.SamplePeripheralScan
import com.bitwisearts.android.explorer.ui.scanning.Advertisements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The Composable that displays the user interface for the Central role.
 *
 * @param viewModel
 *   The [CentralViewModel] that provides the data for this Composable.
 * @param modifier
 *   The [Modifier] to be applied to this Composable.
 *
 * @author Richard Arriaga
 */
@Composable
fun CentralView(
	modifier: Modifier = Modifier,
	lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
	viewModel: CentralViewModel = viewModel()
) {
	val showScanView by viewModel.showScanView.collectAsStateWithLifecycle()
	if (showScanView)
	{
		CentralScanView(
			modifier = modifier.padding(9.dp),
			lifecycleOwner = lifecycleOwner,
			viewModel = viewModel
		)
	}
	else
	{
		CentralDeviceView(
			modifier = modifier.padding(9.dp),
			lifecycleOwner = lifecycleOwner,
			viewModel = viewModel
		)
	}
}

@Composable
private fun CentralScanView(
	modifier: Modifier = Modifier,
	lifecycleOwner: LifecycleOwner,
	viewModel: CentralViewModel
) {
	BackHandler {  }
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
	Column(modifier = modifier.fillMaxSize())
	{
		Row(
			modifier = Modifier
				.padding(all = 20.dp)
				.fillMaxWidth())
		{
			val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
			val bluetoothEnabled by
			viewModel.bluetoothEnabled.collectAsStateWithLifecycle()
			Column {
				Text(
					text = stringResource(R.string.sample_peripherals),
					fontSize = 30.sp,
					modifier = Modifier.padding(horizontal = 18.dp)
				)
				Text("Use the scan button to find a peripheral. Tap on " +
					"the advertisement to expand it then type connect to " +
					"connect to the peripheral. Make sure the peripheral is " +
					"running on another device first..",)
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
		}
		Advertisements(
			buttonLabel = stringResource(id = R.string.connect),
			viewModel.scanRequest.found
		) {
			viewModel.toggleDeviceView()
			viewModel.setSelectedDevice()
			viewModel.connect()
		}
	}
}

@Composable
fun CentralDeviceView(
	modifier: Modifier = Modifier,
	lifecycleOwner: LifecycleOwner,
	viewModel: CentralViewModel
) {
	val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
	DisposableEffect(lifecycleOwner)
	{
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_STOP) {
				selectedDevice?.connection?.fullyCloseConnection()
			}
		}

		// Add the observer to the lifecycle
		lifecycleOwner.lifecycle.addObserver(observer)

		// When the effect leaves the Composition, remove the observer
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
		}
	}
	if (selectedDevice == null)
	{
		Text(
			text = stringResource(R.string.no_device),
			fontSize = 30.sp,
			modifier = Modifier.padding(all = 20.dp)
		)
		return
	}
	val device = selectedDevice!!
	Column(modifier.verticalScroll(rememberScrollState()))
	{
		val connectionState by viewModel.connectionState
			.collectAsStateWithLifecycle()
		Row {
			Text(
				text = stringResource(R.string.connect),
				fontWeight = FontWeight.Bold
			)
			Text(
				modifier = modifier.padding(start = 8.dp),
				text = connectionState.label)
		}
		val receivedMessage by device.receivedMessage
			.collectAsStateWithLifecycle()

		if(connectionState == CONNECTED)
		{
			// Display received messages from the peripheral
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 8.dp)
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						text = "Received Notification",
						style = MaterialTheme.typography.titleMedium
					)

					Text(
						text = receivedMessage?.message ?: "-",
						style = MaterialTheme.typography.bodyMedium
					)
				}
			}

			// Send message to peripheral via Write Characteristic
			var writeMessage by remember { mutableStateOf("Hello from Central!") }
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 8.dp)
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						text = "Send Message",
						style = MaterialTheme.typography.titleMedium
					)

					OutlinedTextField(
						value = writeMessage,
						onValueChange = { writeMessage = it },
						label = { Text("Write message") },
						modifier = Modifier.fillMaxWidth()
					)

					Button(
						onClick = {
							viewModel.viewModelScope.launch {
								device.writeSampleWriteCharacteristic(
									writeMessage)
							}
						},
						modifier = Modifier.align(Alignment.End)
					) {
						Text("Send")
					}
				}
			}

			// Read characteristic from peripheral
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 8.dp)
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						text = "Read Characteristic",
						style = MaterialTheme.typography.titleMedium
					)

					val readValue by device.readMessage
						.collectAsStateWithLifecycle()

					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text =
								readValue.ifEmpty { "Press Read to get value" },
							style = MaterialTheme.typography.bodyMedium,
							modifier = Modifier.weight(1f)
						)

						Button(
							onClick = {
								viewModel.viewModelScope.launch {
									device.readSampleReadCharacteristic()
								}
							}
						) {
							Text("Read")
						}
					}
				}
			}
		}

		if(connectionState == CONNECTED || connectionState == DISCONNECTED)
		{
			Button(
				onClick =
				{
					if (connectionState == CONNECTED)
					{
						viewModel.disconnect()
						viewModel.toggleScanView()
						viewModel.clearSelectedDevice()
						return@Button
					}
					else
					{
						viewModel.connect()
					}
				}
			) {
				val buttonTextId =
					if (connectionState == CONNECTED) R.string.disconnect
					else R.string.connect
				Text(text = stringResource(id = buttonTextId))
			}
		}
	}
}

/**
 * The [ViewModel]
 *
 * @author Richard Arriaga
 */
class CentralViewModel: ViewModel()
{
	override fun onCleared()
	{
		device.disconnect()
		Log.w(
			"CentralViewModel",
			"+++++++++ Has Been Cleared!! ++++++++")
	}

	/** `true` indicates Bluetooth is enabled; `false` otherwise. */
	val bluetoothEnabled get() = ExplorerApp.app.bleScanManager.isBleIsEnabled

	private val _showScanView = MutableStateFlow(true)
	/**
	 * `true` indicates the Scan View should be shown; `false` indicates the
	 * connected device view should be shown.
	 */
	val showScanView get() = _showScanView.asStateFlow()

	/** Toggle the view to show the scan view. */
	fun toggleScanView ()
	{
		_showScanView.value = true
	}

	/** Toggle the view to show the connected device view. */
	fun toggleDeviceView ()
	{
		_showScanView.value = false
	}

	////////////////////////////////////////////////////////////////////////////
	//                             Device Scan						          //
	////////////////////////////////////////////////////////////////////////////

	/** The [ExplorerScan] used for scanning for BLE devices. */
	val scanRequest = SamplePeripheralScan(
		4000,
		false,
		ExplorerApp.app.bleScanManager,
		viewModelScope
	) {
		Log.e("ScanView", it.displayString)
	}

	/**
	 * `true` indicates the app is performing a BLE scan; `false` otherwise.
	 */
	val isScanning get() = ExplorerApp.app.bleScanManager.isScanning

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

	////////////////////////////////////////////////////////////////////////////
	//                           Device Connection						      //
	////////////////////////////////////////////////////////////////////////////

	/**
	 * The [mac address][BleDevice.macAddress] of the presently selected device
	 * or an empty string if no  device selected. It is expected that this is
	 * actually populated with a mac address if we have gotten to this screen.
	 */
	val selectedAddress get() = BleDeviceManager.selectedAddress

	/**
	 * The [Advertisement] of the presently selected device or `null` if no
	 * device selected. It is expected that this is actually not `null` if we have gotten to this
	 * screen.
	 */
	val selectedAdvertisement: Advertisement? get() =
		BleDeviceManager.selectedAdvertisement

	private val _selectedDevice: MutableStateFlow<SampleBleDevice?> =
		MutableStateFlow(null)

	/**
	 * The presently selected [BleDevice] or `null` if no device selected. It is
	 * expected that this is actually not `null` if we have gotten to this
	 * screen.
	 */
	val selectedDevice: StateFlow<SampleBleDevice?> =
		_selectedDevice.asStateFlow()

	/**
	 * Set the presently selected [BleDevice] based on the
	 * [selectedAddress] and [selectedAdvertisement].
	 */
	fun setSelectedDevice ()
	{
		_selectedDevice.value =
			SampleBleDevice(macAddress = selectedAddress.value,
				bluetoothManager = ExplorerApp.app.bleScanManager.bluetoothManager,
				context = ExplorerApp.app.baseContext,
				ioScope = viewModelScope,
				defaultScope = viewModelScope,
				advertisement = selectedAdvertisement
			).apply {
				BleDeviceManager.devices[macAddress] = this
			}
	}

	/** Clear the presently selected device. */
	fun clearSelectedDevice ()
	{
		_selectedDevice.value = null
	}

	private val device get() = selectedDevice.value!!

	/** The current [ConnectionState] of this [device]. */
	val connectionState get() = device.connectionState

	/**
	 * The [StateFlow] containing the map of [BluetoothGattService.getUuid] to
	 * the corresponding [BluetoothGattService].
	 */
	val services get() = device.connection.gattServices

	/** [Connect][BleConnection.connect] to the [device] over BLE. */
	fun connect()
	{
		viewModelScope.launch {
			device.connect(
				autoConnect = false,
				prioritySetting = BleConnection.ConnectionPriority.BALANCED,
				phy = BleConnection.PhysicalLayer.PHY_2M,
				timeoutAction = {
					Log.d(
						"DeviceViewModel",
						"~~~~ Device Failed to Connect ~~~~")
				}
			)
		}
	}

	/**
	 * Disconnect [disconnect][BleConnection.fullyCloseConnection] from the
	 * [device].
	 */
	fun disconnect ()
	{
		viewModelScope.launch {
			device.disconnect()
		}
	}
}