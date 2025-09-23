package com.bitwisearts.android.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.connection.BleConnection.ConnectionPriority
import com.bitwisearts.android.ble.connection.BleConnection.PhysicalLayer
import com.bitwisearts.android.ble.connection.BleConnectionState.DISCONNECTED
import com.bitwisearts.android.ble.connection.ConnectionState
import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicChangeNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A Bluetooth Low Energy (BLE) peripheral.
 *
 * @author Richard Arriaga
 *
 * @property macAddress
 *   This [BleDevice]'s [Mac Address][Advertisement.address].
 * @property advertisement
 *   An [Advertisement] sent by this [BleDevice] or `null` if no [Advertisement]
 *   received.
 */
open class BleDevice constructor(
	val macAddress: String,
	bluetoothManager: BluetoothManager,
	context: Context,
	ioScope: CoroutineScope,
	defaultScope: CoroutineScope,
	var advertisement: Advertisement? = null)
{


	/** Helper for creating a identifying label for logging purposes. */
	val logLabel: String get() =
		"${advertisement?.deviceName ?: ""} (${macAddress})"

	/**
	 * The [BleConnection] to this [BleDevice] or `null` if not connected.
	 */
	var connection: BleConnection
		private set

	/**
	 * A [Job] that monitors the [connectionState] of the current
	 * [connection] if any.
	 */
	private var connectionMonitor: Job

	/**
	 * The [MutableStateFlow] containing the current [ConnectionState] of this
	 * [BleConnection].
	 */
	private val _connectionState: MutableStateFlow<ConnectionState> =
		MutableStateFlow(DISCONNECTED)

	/** The current [ConnectionState] of this [BleConnection]. */
	val connectionState = _connectionState.asStateFlow()

	/**
	 * Handle a [CharacteristicChangeNotification] received for this device.
	 *
	 * @param notification
	 *   The [CharacteristicChangeNotification] that was received.
	 */
	open fun processNotification (notification: CharacteristicChangeNotification)
	{
		// Do nothing by default
	}

	/**
	 * The [List] of [Characteristic]s that should be monitored for
	 * notifications. Override in subclasses to provide the list of
	 * [Characteristic]s to monitor.
	 */
	open val notifyCharacteristics: List<Characteristic> = emptyList()

	open fun afterServicesDiscovered(activeConnection: BleConnection)
	{
		// Do nothing by default
	}

	/**
	 * Create a [BleConnection] to this [BleDevice]. If a connection already
	 * exists and is connected, no new connection will be created.
	 *
	 * @param bluetoothManager
	 *   The [BluetoothManager] used to create the connection.
	 * @param context
	 *   The [Context] used to create the connection.
	 * @param ioScope
	 *   The [CoroutineScope] to use for IO operations.
	 * @param defaultScope
	 *   The [CoroutineScope] to use for default operations.
	 */
	init {
		BleConnection(
			device = this,
			bluetoothManager = bluetoothManager,
			context,
			ioScope = ioScope,
			defaultScope = defaultScope,
			afterServicesDiscovered = ::afterServicesDiscovered
		).also {
			connection = it
			connectionMonitor = defaultScope.launch {
				it.connectionState.collect { state ->
					_connectionState.value = state
				}
			}
		}
	}

	/**
	 * Connect to this [BleDevice] if not already connected.
	 *
	 * @param autoConnect
	 *   The [BluetoothDevice.connectGatt] autoConnect parameter indicating
	 *   whether to directly connect to the remote device (false) or to
	 *   automatically connect as soon as the remote device becomes available
	 *   (true).
	 * @param timeoutMillis
	 *   The time in milliseconds to wait for the connection to be established
	 *   before failing and cancelling the connection attempt.
	 * @param prioritySetting
	 *   The [BleConnection.ConnectionPriority] setting for the connection
	 *   priority.
	 * @param phy
	 *   The [BleConnection.PhysicalLayer] setting that determines the PHY
	 *   used for this connection.
	 * @param timeoutAction
	 *   The lambda that is executed if the connection attempt times out.
	 */
	suspend fun connect(
		autoConnect: Boolean = false,
		timeoutMillis: Long = 6_000L,
		prioritySetting: ConnectionPriority = ConnectionPriority.BALANCED,
		phy: PhysicalLayer = PhysicalLayer.PHY_1M,
		timeoutAction: suspend () -> Unit
	) {
		connection.connect(
			autoConnect = autoConnect,
			timeoutMillis = timeoutMillis,
			prioritySetting = prioritySetting,
			phy = phy,
			timeoutAction = timeoutAction
		)
	}

	/**
	 * Disconnect from this [BleDevice] if connected.
	 */
	fun disconnect()
	{
		connection.fullyCloseConnection()
	}
}