package com.bitwisearts.android.explorer.ui.connected

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.advertisement.AdvertisingDataType
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.connection.BleConnectionState
import com.bitwisearts.android.ble.connection.BleDeviceManager
import com.bitwisearts.android.ble.connection.ConnectionState
import com.bitwisearts.android.ble.gatt.attribute.AttributePermission
import com.bitwisearts.android.ble.gatt.attribute.BleCharacteristicProperty
import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.UnrecognizedService
import com.bitwisearts.android.ble.gatt.attribute.attributePermissions
import com.bitwisearts.android.ble.gatt.attribute.bleCharacteristicProperties
import com.bitwisearts.android.ble.gatt.attribute.common.CommonCharacteristic
import com.bitwisearts.android.ble.gatt.attribute.common.CommonService
import com.bitwisearts.android.ble.gatt.attribute.common.HeartRateMeasurement
import com.bitwisearts.android.ble.request.ReadRequestResult
import com.bitwisearts.android.explorer.ExplorerApp
import com.bitwisearts.android.explorer.R
import com.bitwisearts.android.explorer.ble.device.HeartRateBleDevice
import com.bitwisearts.android.explorer.ui.components.AdvertisementExpanded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
	val bluetoothEnabled by
	viewModel.bluetoothEnabled.collectAsStateWithLifecycle()
	val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
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
					if(connectionState == BleConnectionState.CONNECTED)
					{
						viewModel.disconnect()
						return@Button
					} else {
						viewModel.connect()
					}
				}) {
				val buttonTextId =
					if (connectionState == BleConnectionState.CONNECTED) {
						R.string.disconnect
					} else {
						R.string.connect
					}
				Text(text = stringResource(id = buttonTextId))
			}
		}?: Text(text = "Still gotta build this!!! Show $macAddress")
		val services by viewModel.services.collectAsStateWithLifecycle()
		val servicesExpanded = remember { mutableStateOf(false) }
		DisposableEffect(connectionState) {
			servicesExpanded.value =
				connectionState == BleConnectionState.CONNECTED
			onDispose { }
		}
		Column {
			Row(
				modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
				verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
			) {
				Text(
					text = "Services",
					fontWeight = FontWeight.Bold,
					fontSize = 18.sp,
					modifier = Modifier.weight(1f)
				)
				IconButton(
					onClick = { servicesExpanded.value = !servicesExpanded.value }
				) {
					Icon(
						imageVector = if (servicesExpanded.value)
							Icons.Default.KeyboardArrowUp
						else
							Icons.Default.KeyboardArrowDown,
						contentDescription =
							if (servicesExpanded.value) "Collapse" else "Expand"
					)
				}
			}

			if (servicesExpanded.value) {
				services.forEach { (k, v) ->
					Log.d("DeviceView", "Adding Service $k")
					ServiceView(
						v,
						viewModel.connection,
						viewModel.connection.device)
				}
			}
		}
	}
}

/**
 * A view of the provided [BluetoothGattService].
 *
 * @param service
 *   The [BluetoothGattService] to view.
 * @param bleConnection
 *  The [BleConnection] to use for any operations on the service's
 *  [BluetoothGattCharacteristic]s.
 * @param bleDevice
 *  The [BleDevice] to which the service belongs.
 */
@Composable
fun ServiceView(
	service: BluetoothGattService,
	bleConnection: BleConnection,
	bleDevice: BleDevice
) {
	// TODO totally can do this better!
	val unrecognized = stringResource(id = R.string.unrecognized)
	val s = CommonService[service.uuid] ?:
		UnrecognizedService(
			service.uuid,
			"$unrecognized ${stringResource(id = R.string.service)}",
			service.characteristics
		)
	Column(modifier = Modifier.padding(9.dp))
	{
		Row(Modifier.fillMaxWidth().padding(bottom = 4.dp))
		{
			Text(
				text = s.name,
				fontSize = 18.sp,
				modifier = Modifier.padding(end = 6.dp),
				fontWeight = FontWeight.Bold)
		}
		Row(Modifier.fillMaxWidth().padding(bottom = 7.dp))
		{
			SelectionContainer {
				Text(
					fontSize = 18.sp,
					text = s.uuid.toString()
				)
			}
		}
		s.characteristics.forEach {
			Row(Modifier.fillMaxWidth().padding(start = 1.dp))
			{
				service.getCharacteristic(it.uuid)?.let { bgc ->
					val knownName = CommonCharacteristic[it.uuid]?.name ?:
						"$unrecognized ${stringResource(id = R.string.characteristic)}"
					CharacteristicView(
						characteristic = it,
						charName = knownName,
						properties = bgc.bleCharacteristicProperties,
						permissions = bgc.attributePermissions,
						bleConnection = bleConnection,
						bleDevice = bleDevice
					)
				}
			}
		}
	}
}

/**
 * A view of a [BluetoothGattCharacteristic].
 *
 * @param characteristic
 *   The [Characteristic] to view.
 * @param charName
 *   A String name applied of the characteristic.
 * @param properties
 *   The set of [BleCharacteristicProperty] for the
 *   [BluetoothGattCharacteristic].
 * @param permissions
 *   The set of [AttributePermission]s of the [BluetoothGattCharacteristic].
 * @param bleConnection
 *   The [BleConnection] to use for any operations on the characteristic.
 * @param bleDevice
 *   The [BleDevice] to which the characteristic belongs.
 */
@Composable
fun CharacteristicView(
	characteristic: Characteristic,
	charName: String,
	properties: Set<BleCharacteristicProperty>,
	permissions: Set<AttributePermission>,
	bleConnection: BleConnection,
	bleDevice: BleDevice
) {
	Column(modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp))
	{
		Row(Modifier.fillMaxWidth().padding(bottom = 3.dp))
		{
			Text(
				text = charName,
				fontWeight = FontWeight.Bold
			)
		}
		Row(Modifier.fillMaxWidth().padding(bottom = 7.dp))
		{
			SelectionContainer {
				Text(text = characteristic.uuid.toString())
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
		if(properties.contains(BleCharacteristicProperty.READ))
		{
			ReadCharacteristicView(
				characteristic = characteristic,
				bleConnection = bleConnection,
			)
		}
		else if (bleDevice is HeartRateBleDevice &&
			characteristic.uuid == HeartRateMeasurement.uuid)
		{
			NotifyCharacteristicView(
				characteristic = characteristic,
				bleConnection = bleConnection,
				notifyFlow = bleDevice.heartRate
			)
		}
	}
}

/**
 * A view that allows reading from the given [Characteristic].
 *
 * @param characteristic
 *   The [Characteristic] to read from.
 * @param bleConnection
 *   The [BleConnection] to use to read the characteristic.
 */
@Composable
fun ColumnScope.ReadCharacteristicView(
	characteristic: Characteristic,
	bleConnection: BleConnection)
{
	val connectionState by bleConnection.connectionState
		.collectAsStateWithLifecycle()
	var readResult: ReadRequestResult? by remember {
		mutableStateOf(null)
	}
	val scope = rememberCoroutineScope()
	Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
		Button(
			enabled = connectionState == BleConnectionState.CONNECTED,
			onClick = {
				scope.launch(Dispatchers.IO) {
					readResult = bleConnection.readCharacteristic(characteristic)
				}
		}) {
			Text(text = stringResource(id = R.string.read))
		}
	}
	readResult?.let { result ->
		Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
			val lastReadValue =
				when(result) {
					is ReadRequestResult.ReadFailure ->
						result.toString()
					is ReadRequestResult.ReadSuccess ->
						characteristic.stringifyValue(result.data)
				}
			Text(text = lastReadValue)
		}
	}
}

/**
 * A view that allows reading from the given [Characteristic].
 *
 * @param characteristic
 *   The [Characteristic] to subscribe to notifications from.
 * @param bleConnection
 *   The [BleConnection] to use to read the characteristic.
 */
@Composable
fun ColumnScope.NotifyCharacteristicView(
	characteristic: Characteristic,
	bleConnection: BleConnection,
	notifyFlow: StateFlow<ByteArray>)
{
	val notifyValue by notifyFlow.collectAsStateWithLifecycle()
	Row(Modifier.fillMaxWidth()) {
		Text(
			text = stringResource(id = R.string.notify),
			modifier = Modifier.padding(end = 5.dp),
			fontWeight = FontWeight.Bold,
			fontSize = 10.sp)
		Text(
			text = characteristic.stringifyValue(notifyValue),
			fontSize = 10.sp)
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
		val heartService =
			selectedAdvertisement?.advertisementData?.firstOrNull {
				it.type == AdvertisingDataType.COMPLETE_16_SERVICE_UUID
					&& HeartRateBleDevice.isHeartRateDevice(it.data)
			}
		if (heartService != null)
		{
			Log.d("DeviceViewModel", "It's a Heart Rate Monitor!")
			HeartRateBleDevice(selectedAddress.value, selectedAdvertisement)
		}
		else
		{
			BleDevice(selectedAddress.value, selectedAdvertisement)
		}.apply {
			BleDeviceManager.devices[macAddress] = this
		}
	}

	/**
	 * The [BleConnection] for the [device].
	 */
	val connection: BleConnection =
		BleConnection(
			device = device,
			bluetoothManager = ExplorerApp.app.bleScanManager.bluetoothManager,
			context = ExplorerApp.app.baseContext,
			ioScope = viewModelScope,
			defaultScope = viewModelScope)
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
			connection.connect {
				Log.d(
					"DeviceViewModel",
					"~~~~ Device Failed to Connect ~~~~")

			}
		}
	}

	/**
	 * Disconnect [disconnect][BleConnection.fullyCloseConnection] from the
	 * [device].
	 */
	fun disconnect ()
	{
		viewModelScope.launch {
			connection.fullyCloseConnection()
		}
	}

	override fun onCleared()
	{
		connection.fullyCloseConnection()
		Log.w(
			"DeviceViewModel",
			"+++++++++ Has Been Cleared!! ++++++++")
	}
}