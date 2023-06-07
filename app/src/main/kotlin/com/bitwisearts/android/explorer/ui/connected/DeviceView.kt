package com.bitwisearts.android.explorer.ui.connected

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.connection.BleDeviceManager
import com.bitwisearts.android.ble.connection.ConnectionState
import com.bitwisearts.android.ble.gatt.attribute.AttributePermission
import com.bitwisearts.android.ble.gatt.attribute.BleCharacteristicProperty
import com.bitwisearts.android.ble.gatt.attribute.UnrecognizedService
import com.bitwisearts.android.ble.gatt.attribute.attributePermissions
import com.bitwisearts.android.ble.gatt.attribute.bleCharacteristicProperties
import com.bitwisearts.android.ble.gatt.attribute.common.CommonCharacteristic
import com.bitwisearts.android.ble.gatt.attribute.common.CommonService
import com.bitwisearts.android.explorer.ExplorerApp
import com.bitwisearts.android.explorer.R
import com.bitwisearts.android.explorer.ui.components.AdvertisementExpanded
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The primary [Composable] view showing a particular device for the given Mac
 * Address stored in [BleDeviceManager.devices].
 */
@Composable
fun DeviceView (
	macAddress: String,
	lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
	viewModel: DeviceViewModel = viewModel())
{
	DisposableEffect(lifecycleOwner)
	{
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_STOP) {
				viewModel.connection.fullyCloseConnection()
			}
		}

		// Add the observer to the lifecycle
		lifecycleOwner.lifecycle.addObserver(observer)

		// When the effect leaves the Composition, remove the observer
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
		}
	}
	// TODO do something if bluetooth is turned off...
	val bluetoothEnabled =
		viewModel.bluetoothEnabled.collectAsState().value
	val connectionState = viewModel.connectionState.collectAsState().value
	val advertisement: Advertisement? by
		remember { mutableStateOf(viewModel.selectedAdvertisement)}
	Column(
		modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState()))
	{
		advertisement?.let {
			AdvertisementExpanded(
				it.address,
				it.deviceName,
				it.rssi,
				it.txPower,
				it.serviceUUIDs,
				it.scanRecordBytes,
				it.advertisementData)
			Text(text = connectionState.label)
			Button(onClick =
			{
				viewModel.connect()
			}) {
				Text(text = stringResource(id = R.string.connect))
			}
		}?: Text(text = "Still gotta build this!!! Show $macAddress")
		val services = viewModel.services.collectAsState().value
		services.forEach { (k, v) ->
			Log.d("DeviceView", "Adding Service $k")
			ServiceView(v)
		}
	}
}

/**
 * A view of a [BluetoothGattCharacteristic].
 *
 * @param uuid
 *   The [BluetoothGattCharacteristic.getUuid].
 * @param charName
 *   A String name applied of the characteristic.
 * @param properties
 *   The set of [BleCharacteristicProperty] for the
 *   [BluetoothGattCharacteristic].
 * @param permissions
 *   The set of [AttributePermission]s of the [BluetoothGattCharacteristic].
 */
@Composable
fun CharacteristicView(
	uuid: UUID,
	charName: String,
	properties: Set<BleCharacteristicProperty>,
	permissions: Set<AttributePermission>)
{
	Column(modifier = Modifier.padding(9.dp))
	{
		Row(Modifier.fillMaxWidth().padding(bottom = 7.dp))
		{
			Text(
				text = charName,
				modifier = Modifier.padding(end = 5.dp),
				fontWeight = FontWeight.Bold
			)
			SelectionContainer {
				Text(text = uuid.toString())
			}
		}
		Row(Modifier.fillMaxWidth().padding(bottom = 7.dp))
		{
			Text(
				text = stringResource(id = R.string.properties),
				modifier = Modifier.padding(end = 5.dp),
				fontWeight = FontWeight.Bold,
				fontSize = 10.sp)
			Text(
				text = properties.joinToString(", ") { it.description },
				fontStyle = FontStyle.Italic,
				fontSize = 10.sp)
		}
		Row(Modifier.fillMaxWidth().padding(bottom = 12.dp))
		{
			Text(
				text = stringResource(id = R.string.permissions),
				modifier = Modifier.padding(end = 5.dp),
				fontWeight = FontWeight.Bold,
				fontSize = 10.sp)
			Text(
				text = permissions.joinToString(", ") { it.description },
				fontStyle = FontStyle.Italic,
				fontSize = 10.sp)
		}
	}
}

/**
 * A view of the provided [BluetoothGattService].
 */
@Composable
fun ServiceView(service: BluetoothGattService)
{
	// TODO totally can do this better!
	val unrecognized = stringResource(id = R.string.unrecognized)
	val s = CommonService[service.uuid] ?:
		UnrecognizedService(
			service.uuid,
			"$unrecognized ${stringResource(id = R.string.service)}",
			service.characteristics)
	Column(modifier = Modifier.padding(9.dp))
	{
		Row(Modifier.fillMaxWidth().padding(bottom = 7.dp))
		{
			Text(
				text = s.name,
				modifier = Modifier.padding(end = 6.dp),
				fontWeight = FontWeight.Bold)
			SelectionContainer {
				Text(text = s.uuid.toString())
			}
		}
		s.characteristics.forEach {
			Row(Modifier.fillMaxWidth().padding(start = 1.dp))
			{
				service.getCharacteristic(it.uuid)?.let { bgc ->
					val knownName = CommonCharacteristic[it.uuid]?.name ?:
						"$unrecognized ${stringResource(id = R.string.characteristic)}"
					CharacteristicView(
						uuid = bgc.uuid,
						charName = knownName,
						properties = bgc.bleCharacteristicProperties,
						permissions = bgc.attributePermissions)
					}
			}
		}
	}
}

/**
 * The [ViewModel] for the [DeviceView].
 *
 * @author Richard Arriaga.
 */
class DeviceViewModel: ViewModel()
{
	/** `true` indicates Bluetooth is enabled; `false` otherwise. */
	val bluetoothEnabled get() = ExplorerApp.app.bleScanManager.isBleIsEnabled

	/**
	 * The [mac address][BleDevice.macAddress] of the presently selected device
	 * or an empty string if no  device selected. It is expected that this is
	 * actually populated with a mac address if we have gotten to this screen.
	 */
	val selectedAddress get() = BleDeviceManager.selectedAddress

	/**
	 * The [Advertisement] of the presently selected device or `null` if no
	 * device selected. It is expected that this is actually not `null` if we
	 * have gotten to this screen.
	 */
	val selectedAdvertisement: Advertisement? get() =
		BleDeviceManager.selectedAdvertisement

	/** The target [BleDevice] to connect to. */
	private val device: BleDevice by lazy {
		BleDevice(selectedAddress.value, selectedAdvertisement).apply {
			BleDeviceManager.devices[macAddress] = this
		}
	}

	/**
	 * The [BleConnection] for the [device].
	 */
	val connection: BleConnection =
		BleConnection(
			device,
			ExplorerApp.app.bleScanManager.bluetoothManager,
			ExplorerApp.app.baseContext)
		{
			Log.d("DeviceViewModel", "~~~~ Device Connected ~~~~")
		}

	/** The current [ConnectionState] of this [connection]. */
	val connectionState get() = connection.connectionState

	/**
	 * The [StateFlow] containing the map of [BluetoothGattService.getUuid] to
	 * the corresponding [BluetoothGattService].
	 */
	val services get() = connection.gattServices

	/** [Connect][BleConnection.connect] to the [device] over BLE. */
	fun connect ()
	{
		viewModelScope.launch {
			connection.connect()
		}
	}

	override fun onCleared()
	{
		Log.w(
			"DeviceViewModel",
			"+++++++++ Has Been Cleared!! ++++++++")
	}
}