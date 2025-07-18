package com.bitwisearts.android.ble.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.connection.BleConnection.Companion.DEFAULT_GATT_MAX_MTU_SIZE
import com.bitwisearts.android.ble.connection.BleConnection.Companion.DEFAULT_GATT_MIN_MTU_SIZE
import com.bitwisearts.android.ble.connection.BleConnection.Companion.HEADER_ATT_SIZE
import com.bitwisearts.android.ble.connection.BleConnection.Companion.HEADER_L2CAP_SIZE
import com.bitwisearts.android.ble.connection.BleConnectionState.CONNECTED
import com.bitwisearts.android.ble.connection.BleConnectionState.CONNECTING
import com.bitwisearts.android.ble.connection.BleConnectionState.CONNECTION_FAILED
import com.bitwisearts.android.ble.connection.BleConnectionState.DISCONNECTED
import com.bitwisearts.android.ble.connection.BleConnectionState.DISCONNECTING
import com.bitwisearts.android.ble.connection.BleConnectionState.DISCONNECT_REQUESTED
import com.bitwisearts.android.ble.connection.BleConnectionState.DISCOVERING_SERVICES
import com.bitwisearts.android.ble.connection.BleConnectionState.MTU_NEGOTIATION
import com.bitwisearts.android.ble.gatt.GattNoAttribute
import com.bitwisearts.android.ble.gatt.GattNoConnection
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.KnownGattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicChangeNotification
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicId
import com.bitwisearts.android.ble.gatt.attribute.DescriptorId
import com.bitwisearts.android.ble.gatt.attribute.bleCharacteristicProperties
import com.bitwisearts.android.ble.request.BleRequest
import com.bitwisearts.android.ble.request.CharacteristicReadRequest
import com.bitwisearts.android.ble.request.CharacteristicWriteRequest
import com.bitwisearts.android.ble.request.DescriptorReadRequest
import com.bitwisearts.android.ble.request.DescriptorWriteRequest
import com.bitwisearts.android.ble.request.EnableNotifyCharacteristicRequest
import com.bitwisearts.android.ble.utility.asHex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.math.min

/**
 * The [BluetoothGattCallback] that represents an active connection to a
 * [BleDevice].
 *
 * The connection workflow is dictated by [BleConnectionState]
 * ([connectionState]) of this connection. Connecting the device for the first
 * time will result in this transition of states:
 *
 * 1. [BleConnectionState.DISCONNECTED]
 * 2. [BleConnectionState.MTU_NEGOTIATION] - Set after transition to actual
 *   connected state, coinciding with the call to
 *   [negotiate the MTU][BluetoothGatt.requestMtu].
 * 3. [BleConnectionState.DISCOVERING_SERVICES] - Set immediately after
 *   [MTU negotiation][BluetoothGatt.requestMtu] is resolved and
 *   [BluetoothGatt.discoverServices] is called.
 * 4. [BleConnectionState.CONNECTED] - Set after
 *   [BluetoothGatt.discoverServices] completes.
 *
 * @author Richard Arriaga
 *
 * @property device
 *   The [BleDevice] this [BleConnection] is connecting to.
 * @property bluetoothManager
 *   The Android [BluetoothManager] made available to the running application.
 * @property context
 *   The underlying [Context] used to establish the connection using
 *   [BluetoothDevice.connectGatt].
 * @property ioScope
 *   The [CoroutineScope] used to launch IO coroutines used to interact with
 *   the device.
 * @property defaultScope
 *   The [CoroutineScope] used to launch the coroutines used to process the
 *   results of a request.
 * @property afterServicesDiscovered
 *   A custom lambda that
 */
@SuppressLint("MissingPermission")
open class BleConnection constructor(
	val device: BleDevice,
	private val bluetoothManager: BluetoothManager,
	private val context: Context,
	private val ioScope: CoroutineScope =
		CoroutineScope(SupervisorJob() + Dispatchers.IO),
	private val defaultScope: CoroutineScope =
		CoroutineScope(SupervisorJob() + Dispatchers.Default),
	private val afterServicesDiscovered: suspend (BleConnection) -> Unit
): BluetoothGattCallback()
{
	/** The [Mutex] for synchronizing BLE related actions. */
	private val mutex = Mutex()

	/**
	 * The negotiated MTU for transmitted data to/from the [BleDevice] over this
	 * connection.
	 */
	var mtu = DEFAULT_GATT_MAX_MTU_SIZE
		private set

	/**
	 * The [MutableStateFlow] containing the current [ConnectionState] of this
	 * [BleConnection].
	 */
	private val _connectionState: MutableStateFlow<ConnectionState> =
		MutableStateFlow(DISCONNECTED)

	/** The current [ConnectionState] of this [BleConnection]. */
	val connectionState = _connectionState.asStateFlow()

	/**
	 * The [BluetoothGatt] that represents a connection to the [BleDevice] or
	 * `null` if no connection is active.
	 */
	var gatt: BluetoothGatt? = null

	/**
	 * Disconnect the BLE connection to the [device]. This will retain the 
	 * [gatt] and reuse it to reconnect to the [device] if a reconnect request
	 * is made.
	 */
	@SuppressLint("MissingPermission")
	fun disconnect()
	{
		ioScope.launch(Dispatchers.IO) {
			mutex.withLock {
				if (!(_connectionState.value).isConnected)
				{
					return@launch
				}
				_connectionState.value = DISCONNECT_REQUESTED
				gatt?.disconnect()
			}
		}
	}

	/**
	 * Fully close the [gatt] connection; this makes the [gatt] no longer 
	 * usable.
	 */
	fun fullyCloseConnection()
	{
		ioScope.launch {
			mutex.withLock {
				_connectionState.value = DISCONNECT_REQUESTED
				if (!(_connectionState.value).isConnected)
				{
					return@launch
				}
				gatt?.apply {
					disconnect()
					close()
					handlerThread?.quitSafely()
					handlerThread = null
				}
			}
		}
	}

	private var timeoutJob: Job? = null

	private var handlerThread: HandlerThread? = null

	/**
	 * Connect to the [device] if not already connected.
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
		autoConnect: Boolean = true,
		timeoutMillis: Long = 6_000L,
		prioritySetting: ConnectionPriority = ConnectionPriority.BALANCED,
		phy: PhysicalLayer = PhysicalLayer.PHY_2M,
		timeoutAction: suspend () -> Unit)
	{
		if (_connectionState.value == CONNECTED)
		{
			return
		}
		this.prioritySetting = prioritySetting
		_connectionState.value = CONNECTING
		mutex.withLock {
			// If gatt present attempt a reconnect, otherwise create a new
			// connection.
			handlerThread?.quitSafely()
			val tempThread = HandlerThread("BleConnectionHandlerThread").apply {
				handlerThread = this
				start()
			}
			val handler = Handler(tempThread.looper)
			gatt?.connect() ?: bluetoothManager.adapter
				.getRemoteDevice(device.macAddress)
				.connectGatt(
					context,
					autoConnect,
					this,
					BluetoothDevice.TRANSPORT_LE,
					phy.code,
					handler
				)
			timeoutJob?.cancel()
			timeoutJob = ioScope.launch {
				delay(timeoutMillis)
				if (isActive && _connectionState.value != CONNECTED)
				{
					_connectionState.value = CONNECTION_FAILED
					timeoutAction()
					gatt?.close()
					Log.i("BleConnection","Connection attempt locally timed out.")
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////
	//                             BLE Requests                               //
	////////////////////////////////////////////////////////////////////////////
	/** The buffered [Channel] of [BleRequest]s for this [BleConnection]. */
	private var bleRequestChannel = Channel<BleRequest<*, *>>(Channel.BUFFERED)

	/**
	 * The last incomplete [CharacteristicReadRequest] or `null` if none
	 * outstanding.
	 */
	private var lastCharacterReadRequest: CharacteristicReadRequest? = null

	/**
	 * The last incomplete [DescriptorReadRequest] or `null` if none
	 * outstanding.
	 */
	private var lastDescriptorReadRequest: DescriptorReadRequest? = null

	/**
	 * The last incomplete [CharacteristicWriteRequest] or `null` if none
	 * outstanding.
	 */
	private var lastCharacterWriteRequest: CharacteristicWriteRequest? = null

	/**
	 * The last incomplete [DescriptorWriteRequest] or `null` if none
	 * outstanding.
	 */
	private var lastDescriptorWriteRequest: DescriptorWriteRequest? = null

	/**
	 * Fail the given [BleRequest] due to the not-connected state.
	 *
	 * @param bleRequest
	 *   The [BleRequest] to fail.
	 * @param state
	 *   The [connectionState] this [BleConnection] is in that caused the
	 *   request to fail.
	 */
	private fun failRequestNoConnection (
		bleRequest: BleRequest<*, *>, state: ConnectionState
	)
	{
		Log.i(
			"BleConnection",
			"Attempted $bleRequest, but connection status is $state")
		when (bleRequest)
		{
			is CharacteristicReadRequest ->
				bleRequest.complete(null, GattNoConnection)
			is DescriptorReadRequest ->
				bleRequest.complete(null, GattNoConnection)
			is CharacteristicWriteRequest ->
				bleRequest.gattResponseHandler(GattNoConnection)
			is DescriptorWriteRequest ->
				bleRequest.gattResponseHandler(GattNoConnection)
			is EnableNotifyCharacteristicRequest ->
				bleRequest.resultHandler(false, null)
		}
	}

	/**
	 * Submit the provided [BleRequest] to the device connected using this
	 * [BleConnection].
	 *
	 * @param bleRequest
	 *   The [BleRequest] to submit.
	 */
	suspend fun submitBleRequest(bleRequest: BleRequest<*, *>)
	{
		val state = connectionState.value
		if (state == CONNECTED)
		{
			bleRequestChannel.send(bleRequest)
		}
		else
		{
			failRequestNoConnection(bleRequest, state)
		}
	}

	/**
	 * Process the provided [BleRequest].
	 *
	 * @param bleRequest
	 *   The request to be processed.
	 */
	private suspend fun processBleRequest(bleRequest: BleRequest<*, *>)
	{
		val state = connectionState.value
		if (state != CONNECTED)
		{
			failRequestNoConnection(bleRequest, state)
			return
		}
		when (bleRequest)
		{
			is CharacteristicReadRequest ->
			{
				mutex.withLock {
					gatt?.let {
						val char = characteristic(bleRequest.identifier)
						if (char == null)
						{
							bleRequest.complete(
								null, GattNoAttribute(bleRequest.identifier))
							return
						}
						lastCharacterReadRequest = bleRequest
						bleRequest.request(it, char)
					}
				}
			}
			is DescriptorReadRequest ->
			{
				mutex.withLock {
					gatt?.let {
						val desc = descriptor(bleRequest.identifier)
						if (desc == null)
						{
							bleRequest.complete(null, GattNoConnection)
							return
						}
						lastDescriptorReadRequest = bleRequest
						bleRequest.request(it, desc)
					}
				}
			}
			is CharacteristicWriteRequest ->
			{
				mutex.withLock {
					gatt?.let {
						val char = characteristic(bleRequest.identifier)
						if (char == null)
						{
							bleRequest.gattResponseHandler(GattNoConnection)
							return
						}
						lastCharacterWriteRequest = bleRequest
						bleRequest.request(it, char)
					}
				}
			}
			is DescriptorWriteRequest ->
			{
				mutex.withLock {
					gatt?.let {
						val desc = descriptor(bleRequest.identifier)
						if (desc == null)
						{
							bleRequest.gattResponseHandler(GattNoConnection)
							return
						}
						lastDescriptorWriteRequest = bleRequest
						bleRequest.request(it, desc)
					}
				}
			}
			is EnableNotifyCharacteristicRequest ->
			{
				mutex.withLock {
					gatt?.let {
						val char = characteristic(bleRequest.identifier)
						if (char == null)
						{
							Log.w(
								"BleConnection",
								"Attempted to enable notify on " +
									"characteristic, ${bleRequest.identifier}, " +
									"but characteristic not found.")
							bleRequest.resultHandler(false, null)
							return
						}

						if (char.bleCharacteristicProperties
							.any { p -> p.supportsNotify })
						{
							val enabled = it.setCharacteristicNotification(
								char, true)
							Log.i(
								"BleConnection",
								"Enable notify on characteristic," +
									"${bleRequest.identifier}, success: $enabled")
							bleRequest.resultHandler(enabled, null)
						}
						else
						{
							Log.w(
								"BleConnection",
								"Attempted to enable notify on " +
									"characteristic, ${bleRequest.identifier}, " +
									"but characteristic does not support notify.")
							bleRequest.resultHandler(false, null)
						}
					} ?: run {
						Log.w(
							"BleConnection",
							"Attempted to enable notify on " +
								"characteristic, ${bleRequest.identifier}, but " +
								"no BLE connection is available."
						)
						bleRequest.resultHandler(false, null)
					}
					processNextRequest()
				}
			}
		}
	}

	/** Process the next received [BleRequest] from the [bleRequestChannel]. */
	private fun processNextRequest()
	{
		ioScope.launch {
			val req = bleRequestChannel.receive()
			processBleRequest(req)
		}
	}

	////////////////////////////////////////////////////////////////////////////
	//                    BluetoothGattCallback Overrides                     //
	////////////////////////////////////////////////////////////////////////////
	/**
	 * The [ConnectionPriority] setting for the connection priority. Default is
	 * balanced.
	 */
	private var prioritySetting: ConnectionPriority =
		ConnectionPriority.BALANCED

	/**
	 * The [PhysicalLayer] setting that determines the PHY used for this
	 * connection. Default is 1M.
	 */
	private var phy: PhysicalLayer = PhysicalLayer.PHY_1M

	/**
	 * The [MutableStateFlow] containing the map of
	 * [BluetoothGattService.getUuid] to the corresponding
	 * [BluetoothGattService].
	 */
	private val _gattServiceMap:
		MutableStateFlow<Map<UUID, BluetoothGattService>> =
			MutableStateFlow(mapOf())

	/**
	 * The [StateFlow] containing the map of [BluetoothGattService.getUuid] to
	 * the corresponding [BluetoothGattService].
	 */
	val gattServices: StateFlow<Map<UUID, BluetoothGattService>> get() =
		_gattServiceMap.asStateFlow()

	/**
	 * Answer the [BluetoothGattService] for the associated
	 * [id][BluetoothGattService.getUuid].
	 *
	 * @param id
	 *   The id of the [BluetoothGattService] to retrieve.
	 * @return
	 *   The associated [BluetoothGattService] or `null` if not found.
	 */
	fun service (id: UUID): BluetoothGattService? = _gattServiceMap.value[id]

	/**
	 * The [MutableStateFlow] containing the map of [CharacteristicId] to the
	 * corresponding [BluetoothGattCharacteristic].
	 */
	private val _gattCharacteristicMap:
		MutableStateFlow<Map<CharacteristicId, BluetoothGattCharacteristic>> =
			MutableStateFlow(mapOf())

	/**
	 * The [StateFlow] containing the map of [CharacteristicId] to the
	 * corresponding [BluetoothGattCharacteristic].
	 */
	val gattCharacteristics:
		StateFlow<Map<CharacteristicId, BluetoothGattCharacteristic>> get() =
			_gattCharacteristicMap.asStateFlow()

	/**
	 * Answer the [BluetoothGattCharacteristic] for the associated
	 * [CharacteristicId].
	 *
	 * @param id
	 *   The id of the [CharacteristicId] to retrieve.
	 * @return
	 *   The associated [BluetoothGattCharacteristic] or `null` if not found.
	 */
	fun characteristic (id: CharacteristicId): BluetoothGattCharacteristic? =
		_gattCharacteristicMap.value[id]

	/**
	 * The [MutableStateFlow] containing the map of [DescriptorId] to the
	 * corresponding [BluetoothGattDescriptor].
	 */
	private val _gattDescriptorMap:
		MutableStateFlow<Map<DescriptorId, BluetoothGattDescriptor>> =
			MutableStateFlow(mapOf())

	/**
	 * The [StateFlow] containing the map of [DescriptorId] to the corresponding
	 * [BluetoothGattDescriptor].
	 */
	val gattDescriptors:
		StateFlow<Map<DescriptorId, BluetoothGattDescriptor>> get() =
			_gattDescriptorMap.asStateFlow()

	/**
	 * Answer the [BluetoothGattDescriptor] for the associated [DescriptorId].
	 *
	 * @param id
	 *   The id of the [DescriptorId] to retrieve.
	 * @return
	 *   The associated [BluetoothGattDescriptor] or `null` if not found.
	 */
	fun descriptor (id: DescriptorId): BluetoothGattDescriptor? =
		_gattDescriptorMap.value[id]
	
	override fun onConnectionStateChange(
		gatt: BluetoothGatt, status: Int, newState: Int)
	{
		val gattStatusCode = KnownGattStatusCode[status]
		val newConnectionState = BleConnectionState[newState]
		val previousState = _connectionState.value
		Log.i(
			"BLE Connection State Change",
			"${device.logLabel}: " +
				"New State: $newConnectionState ($newState) " +
				"Status: ${gattStatusCode.display}\n" +
				"Previous State: $previousState")
		if (newConnectionState == previousState)
		{
			return
		}
		when (newConnectionState)
		{
			CONNECTED ->
			{
				if (gattStatusCode == KnownGattStatusCode.SUCCESS)
				{
					timeoutJob?.cancel()
					timeoutJob = null
					if (_connectionState.value != DISCONNECT_REQUESTED)
					{
						Log.i("BLE Connected","Triggering MTU negotiation")
						ioScope.launch {
							delay(200)
							_connectionState.value = MTU_NEGOTIATION
							gatt.requestMtu(DEFAULT_GATT_MAX_MTU_SIZE)
						}
					}
				}
				else
				{
					Log.e(
						"BLE Connection Fail",
						"${device.logLabel}: ${gattStatusCode.display}")
					_connectionState.value = CONNECTION_FAILED
				}
			}
			CONNECTING ->
			{
				Log.i(
					"BLE Connecting",
					"${device.logLabel}: ${gattStatusCode.display}")
				_connectionState.value = CONNECTING
			}
			DISCONNECTED ->
			{
				Log.i(
					"BLE Disconnected",
					"${device.logLabel}: ${gattStatusCode.display}")
				if (_connectionState.value == DISCONNECT_REQUESTED)
				{
					// We asked for the disconnect so we need to proactively
					// clean up the GATT.
					gatt.close()
				}
				_connectionState.value = DISCONNECTED
			}
			DISCONNECTING ->
			{
				Log.i(
					"BLE Disconnecting",
					"${device.logLabel}: ${gattStatusCode.display}")
				_connectionState.value = DISCONNECTING
			}
			// Discovering services has a fictitious status code that should
			// not be known to Android, thus if it is actually received, it
			// is invalid. The same goes with the Invalid connection state
			else ->
			{
				Log.e(
					"BLE_Connection_State",
					"State: $newConnectionState ($newState) " +
						"Status: ${gattStatusCode.display}")
				_connectionState.value = InvalidConnectionState(status)
				gatt.close()
			}
		}
	}

	override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int)
	{
		Log.d(
			"MTU_CHANGED",
			"MTU Value: $mtu (${KnownGattStatusCode[status]})")
		this.mtu =
			if (status == KnownGattStatusCode.SUCCESS.code)
				min(mtu - HEADER_ATT_SIZE, ADJUSTED_MAX_MTU_SIZE)
			else ADJUSTED_MIN_MTU_SIZE

		// We don't bother trying to make a next request if the connection
		// times out.
		if (status != KnownGattStatusCode.CONNECTION_TIMEOUT.code)
		{
			_connectionState.value = DISCOVERING_SERVICES
			ioScope.launch {
				gatt.discoverServices()
			}
		}
	}
	override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int)
	{
		ioScope.launch {
			if (status == KnownGattStatusCode.SUCCESS.code &&
				_connectionState.value != DISCONNECT_REQUESTED)
			{
				mutex.withLock {
					// The GATT services are fully populated and ready for use.
					this@BleConnection.gatt = gatt
					val priorityResult = gatt.requestConnectionPriority(prioritySetting.code)
					Log.d(
						"BLE_Connection_Priority",
						"Set connection priority to $prioritySetting: $priorityResult")
					val serviceMap = mutableMapOf<UUID, BluetoothGattService>()
					val charMap = 
						mutableMapOf<CharacteristicId, BluetoothGattCharacteristic>()
					val descrMap =
						mutableMapOf<DescriptorId, BluetoothGattDescriptor>()

					// Walk the GATT services and extract them along with their
					// characteristics along with their descriptors.
					gatt.services.forEach { s ->
						serviceMap[s.uuid] = s
						s.characteristics.forEach { c ->
							charMap[CharacteristicId(s.uuid, c.uuid)] = c
							c.descriptors.forEach { d ->
								descrMap[DescriptorId(s.uuid, c.uuid, d.uuid)] = d
							}
						}
					}
					_gattServiceMap.value = serviceMap
					_gattCharacteristicMap.value = charMap
					_gattDescriptorMap.value = descrMap
					if (_connectionState.value == DISCOVERING_SERVICES)
					{
						val prioritySetting =
							gatt.requestConnectionPriority(prioritySetting.code)
						// TODO make sure we transition to being fully connected
						//  only after all requested notifications are enabled.
						device.notifyCharacteristics.forEach {
							submitBleRequest(
								EnableNotifyCharacteristicRequest(
									it.characteristicId,
									this@BleConnection,
									ioScope
								) { success, status ->
									if (!success)
									{
										Log.w(
											"Enable Notify Failed",
											"${device.logLabel}: " +
												"Characteristic: $it " +
												if (status != null)
												{
													"Status: ${status.display}"
												}
												else
												{
													"Characteristic not found"
												}
										)
									}
								}
							)
						}
						// This connection is now ready for general use.
						_connectionState.value = CONNECTED
						afterServicesDiscovered(this@BleConnection)
					}
				}
			}
			else
			{
				Log.e(
					"Fail BLE Services Discovery",
					"${device.logLabel}: " +
						KnownGattStatusCode[status].display)
			}
		}
	}

	@Deprecated("For API 31 Use " +
		"onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic, ByteArray)")
	override fun onCharacteristicChanged(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic)
	{
		// Read as soon as possible to avoid race condition with a subsequent
		// change
		val received = characteristic.value?.clone() ?: ByteArray(0)
		Log.d(
			"Characteristic Changed",
			"${characteristic.uuid}: ${received.asHex}")
		ioScope.launch {
			device.processNotification(
				CharacteristicChangeNotification(characteristic, received))
		}
	}

	override fun onCharacteristicChanged(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic,
		value: ByteArray)
	{
		Log.d(
			"Characteristic Changed API 33+",
			"${characteristic.uuid}: ${value.asHex}")
		ioScope.launch {
			device.processNotification(
				CharacteristicChangeNotification(characteristic, value))
		}
	}
	@Deprecated("Deprecated in Java: value read can race")
	override fun onCharacteristicRead(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic,
		status: Int)
	{
		// Note, due to potential for race conditions, if another read of this is issued and completed before this is read, the value
		// returned from the original request will have been overwritten. It is
		// important to grab the value out of the characteristic as soon as
		// possible on this thread.
		val value = characteristic.value?.clone() ?: ByteArray(0)
		defaultScope.launch {
			processCharacteristicReadRequest(characteristic, value, status)
		}
	}
	override fun onCharacteristicRead(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic,
		value: ByteArray,
		status: Int)
	{
		defaultScope.launch {
			processCharacteristicReadRequest(characteristic, value, status)
		}
	}

	override fun onCharacteristicWrite(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic,
		status: Int)
	{
		defaultScope.launch {
			mutex.withLock {
				lastCharacterWriteRequest?.let {
					val result = KnownGattStatusCode[status]
					if (result != KnownGattStatusCode.SUCCESS) {
						if(!it.resendLastPayload(gatt, characteristic)) {
							if(it.gattResponseHandler(result)) {
								processNextRequest()
							}
						}
					} else {
						if (it.isComplete)
						{
							lastCharacterWriteRequest = null
							if(it.gattResponseHandler(result)) {
								processNextRequest()
							}
						}
						else
						{
							it.request(gatt, characteristic)
						}
					}
				}

			}
		}
	}

	@Deprecated("Deprecated in Java: value read can race")
	override fun onDescriptorRead(
		gatt: BluetoothGatt,
		descriptor: BluetoothGattDescriptor,
		status: Int)
	{
		// Note, due to potential for race conditions, if another read of this
		// descriptor is issued and completed before this is read, the value
		// returned from the original request will have been overwritten. It is
		// important to grab the value out of the descriptor as soon as possible
		// on this thread.
		val value = descriptor.value?.clone() ?: ByteArray(0)
		defaultScope.launch {
			processDescriptorReadRequest(descriptor, value, status)
		}
	}
	override fun onDescriptorRead(
		gatt: BluetoothGatt,
		descriptor: BluetoothGattDescriptor,
		status: Int,
		value: ByteArray)
	{
		defaultScope.launch {
			processDescriptorReadRequest(descriptor, value, status)
		}
	}

	override fun onDescriptorWrite(
		gatt: BluetoothGatt,
		descriptor: BluetoothGattDescriptor,
		status: Int)
	{
		defaultScope.launch {
			mutex.withLock {
				lastDescriptorWriteRequest?.let {
					val result = KnownGattStatusCode[status]
					Log.d(
						"BLE_Descriptor_Write",
						"${descriptor.uuid}: $result")

					if (result != KnownGattStatusCode.SUCCESS) {
						// Handle write failure
						Log.w(
							"BLE_Descriptor_Write_Failed",
							"${descriptor.uuid}: $result")
						if (!it.gattResponseHandler(result)) {
							// If handler returns false, don't process next request
							return@launch
						}
					} else if (!it.isComplete) {
						// If there are more bytes to write, continue the write operation
						it.request(gatt, descriptor)
						return@launch
					} else {
						// Success and complete
						it.gattResponseHandler(result)
					}
				}
				lastDescriptorWriteRequest = null
				processNextRequest()
			}
		}
	}

	/**
	 * Process the result of a [CharacteristicReadRequest].
	 *
	 * @param characteristic
	 *   The [BluetoothGattCharacteristic] read from.
	 * @param value
	 *   The [ByteArray] value read or `null` if no value read.
	 * @param status
	 *   The [GattStatusCode.code] that describes the result of the request.
	 */
	private suspend fun processCharacteristicReadRequest (
		characteristic: BluetoothGattCharacteristic,
		value: ByteArray?,
		status: Int)
	{
		mutex.withLock {
			lastCharacterReadRequest?.let {
				val result = KnownGattStatusCode[status]
				Log.d(
					"BLE_Characteristic_Read",
					"${characteristic.uuid}: ($result) " +
						(value?.asHex + " No Value Read"))
				it.complete(value, result)
			}
			lastCharacterReadRequest = null
			processNextRequest()
		}
	}

	/**
	 * Process the result of a [DescriptorReadRequest].
	 *
	 * @param descriptor
	 *   The [BluetoothGattDescriptor] read from.
	 * @param value
	 *   The [ByteArray] value read or `null` if no value read.
	 * @param status
	 *   The [GattStatusCode.code] that describes the result of the request.
	 */
	private suspend fun processDescriptorReadRequest (
		descriptor: BluetoothGattDescriptor,
		value: ByteArray?,
		status: Int)
	{
		mutex.withLock {
			lastDescriptorReadRequest?.let {
				val result = KnownGattStatusCode[status]
				Log.d(
					"BLE_Characteristic_Read",
					"${descriptor.uuid}: ($result) " +
						(value?.asHex + " No Value Read"))
				it.complete(value, result)
			}
			lastDescriptorReadRequest = null
			processNextRequest()
		}
	}

	companion object
	{
		/**
		 * The L2CAP Header (Logical Link Control and Adaptation Protocol) is
		 * the number of bytes that must be accounted for in the MTU; this is
		 * the first 4 bytes of the transmitted payload.
		 */
		private const val HEADER_L2CAP_SIZE: Int = 4

		/**
		 * The ATT Header represents the number of bytes that must be accounted
		 * for which includes the operation code (1 byte) and the attribute
		 * handle (2 bytes).
		 */
		private const val HEADER_ATT_SIZE: Int = 3

		/**
		 * The total number of bytes that must be attributed to the BLE header
		 * for a which includes the L2CAP header (first 4 bytes) and the ATT
		 * header (3 bytes).
		 */
		private const val TOTAL_HEADER_BLE_SIZE: Int =
			HEADER_L2CAP_SIZE + HEADER_ATT_SIZE

		/**
		 * The purported maximum maximum transmission unit (MTU) for Android. It
		 * is used to negotiate the MTU for the connected device.
		 *
		 * Though this is the official maximum, many devices per Bluetooth 4.2
		 * with the Data Length Extension (DLE), we can effectively transmit up
		 * to 251 bytes total.
		 *
		 * Note that the MTU includes the ATT header (3 bytes) and L2CAP Header
		 * (4 bytes), so the actual maximum payload is
		 *
		 * ```kotlin
		 * DEFAULT_GATT_MAX_MTU_SIZE - HEADER_L2CAP_SIZE - HEADER_ATT_SIZE
		 * ```
		 */
		private const val DEFAULT_GATT_MAX_MTU_SIZE = 517

		/**
		 * The purported minimum maximum transmission unit is 27. It is reduced
		 * by 3 bytes to accommodate the ATT header: OP-Code (1 byte) and
		 * Attribute Handle (2 bytes) and 4 bytes . Used if MTU negotiation fails.
		 */
		private const val DEFAULT_GATT_MIN_MTU_SIZE = 27

		/**
		 * The maximum number of bytes that can be sent in a single transmission
		 * accounting for the [HEADER_L2CAP_SIZE] and [HEADER_ATT_SIZE] starting
		 * with [DEFAULT_GATT_MAX_MTU_SIZE] as the base MTU.
		 */
		const val ADJUSTED_MAX_MTU_SIZE: Int =
			DEFAULT_GATT_MAX_MTU_SIZE - TOTAL_HEADER_BLE_SIZE

		/**
		 * The minimum number of bytes that can be sent in a single transmission
		 * accounting for the [HEADER_L2CAP_SIZE] and [HEADER_ATT_SIZE] starting
		 * with [DEFAULT_GATT_MIN_MTU_SIZE] as the base MTU.
		 */
		const val ADJUSTED_MIN_MTU_SIZE: Int =
			DEFAULT_GATT_MIN_MTU_SIZE - TOTAL_HEADER_BLE_SIZE
	}

	/**
	 * The connection priority to use when requesting a change in the connection
	 * priority using [BluetoothGatt.requestConnectionPriority].
	 */
	enum class ConnectionPriority(val code: Int)
	{
		/** Balanced connection priority. */
		BALANCED(BluetoothGatt.CONNECTION_PRIORITY_BALANCED),

		/** High connection priority. Consumes more power. */
		HIGH(BluetoothGatt.CONNECTION_PRIORITY_HIGH),

		/** Low power connection priority. Slower, consumes less power. */
		LOW_POWER(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER)
	}

	/**
	 * The physical layer to use when requesting a change in the physical layer
	 * using [BluetoothGatt.setPreferredPhy].
	 */
	enum class PhysicalLayer(val code: Int)
	{
		/** Basic 1M PHY, standard BLE data rate. */
		PHY_1M(BluetoothDevice.PHY_LE_1M),

		/** 2M PHY, faster data rate introduced in Bluetooth 5.0. */
		PHY_2M(BluetoothDevice.PHY_LE_2M),

		/** Coded PHY for extended range, also from Bluetooth 5.0. */
		PHY_CODED(BluetoothDevice.PHY_LE_CODED)
	}
}
