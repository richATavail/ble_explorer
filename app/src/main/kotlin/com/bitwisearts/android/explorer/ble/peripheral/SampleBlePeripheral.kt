package com.bitwisearts.android.explorer.ble.peripheral

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.gatt.attribute.common.ClientCharacteristicConfiguration
import com.bitwisearts.android.explorer.ExplorerApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

/**
 * A comprehensive BLE (Bluetooth Low Energy) peripheral implementation for
 * Android. This is used to make testing the BLE central functionality easier.
 *
 * This class provides a complete BLE peripheral that can:
 * - Advertise itself with a custom service UUID and device name
 * - Act as a [BluetoothGattServer] with read, write, and notify characteristics
 * - Handle multiple client connections
 * - Send notifications to subscribed clients
 *
 * @property context
 *   The Android context used for Bluetooth operations and permission checks
 * @property bluetoothManager
 *   The [BluetoothManager] for managing Bluetooth operations. If not provided,
 *   it will be obtained from the context.
 * @property deviceName
 *   The name that will be advertised to BLE clients. Defaults to
 *   "BLE Peripheral".
 */
@Suppress("MissingPermission")
class SampleBlePeripheral(
	private val context: Context = ExplorerApp.app,
	private val bluetoothManager: BluetoothManager =
		context.getSystemService(Context.BLUETOOTH_SERVICE)
			as BluetoothManager,
	private val deviceName: String = "Mine",
)
{
	companion object
	{
		/** Log tag used for debugging and error messages */
		private const val TAG = "BLEPeripheral"
	}

	/**
	 * The MTU (Maximum Transmission Unit) size for BLE communication. Defaults
	 * to [BleConnection.ADJUSTED_MAX_MTU_SIZE].
	 */
	private var mtu = BleConnection.ADJUSTED_MAX_MTU_SIZE

	/** Bluetooth adapter for Bluetooth functionality */
	private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

	/** BLE advertiser for broadcasting the peripheral */
	private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
		bluetoothAdapter.bluetoothLeAdvertiser

	/** GATT server that handles client connections and requests */
	private var gattServer: BluetoothGattServer? = null

	/**
	 * The value returned when clients read from the read characteristic
	 */
	private var readCharacteristicValue: ByteArray = byteArrayOf(0)

	private val _writeValueFlow = MutableStateFlow(byteArrayOf())
	/** The value received when clients write to the write characteristic */
	val writeValueFlow: StateFlow<ByteArray> = _writeValueFlow.asStateFlow()

	/** Set of currently connected BLE devices */
	private val connectedDevices = mutableSetOf<BluetoothDevice>()

	/** Set of devices that have subscribed to notifications */
	private val notificationSubscribers = mutableSetOf<BluetoothDevice>()

	/** Accumulator for assembling multi-part write messages */
	private var writeMessageAccumulator: MessageDeserializer? = null

	init
	{
		when
		{
			!bluetoothAdapter.isEnabled ->
			{
				Log.e(TAG, "Bluetooth not enabled")
			}

			!context.packageManager.hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE) ->
			{
				Log.e(TAG, "BLE not supported")
			}

			else ->
			{
				if (bluetoothLeAdvertiser == null)
				{
					Log.e(TAG, "BLE advertising not supported")
				}
			}
		}
	}

	/**
	 * Sets the value that will be returned when clients read from the read
	 * characteristic.
	 *
	 * @param value
	 *   The integer value to set for the read characteristic
	 */
	fun setReadCharacteristicValue(value: Int)
	{
		this.readCharacteristicValue = serializeUnsignedInt(value)
		Log.d(TAG, "Set read characteristic value to $value")
	}

	/**
	 * Starts the BLE peripheral by setting up the GATT server and beginning advertisement.
	 *
	 * This method:
	 * 1. Checks for required Bluetooth permissions
	 * 2. Sets up the GATT server with read, write, and notify characteristics
	 * 3. Starts BLE advertising with the configured service UUID and device name
	 *
	 * @return
	 *   `true` if the peripheral started successfully, false otherwise
	 * @throws SecurityException if required Bluetooth permissions are not granted
	 */
	fun startPeripheral(): Boolean
	{
		Log.i(TAG, "Starting BLE Peripheral")
		if (!checkPermissions())
		{
			Log.e(TAG, "Missing required permissions")
			return false
		}

		// Make sure we close any existing server first to avoid conflicts
		closeGattServer()

		if (!setupGattServer())
		{
			Log.e(TAG, "Failed to setup GATT server")
			return false
		}

		// Stop any existing advertising first
		stopAdvertising()

		if (!startAdvertising())
		{
			Log.e(TAG, "Failed to start advertising")
			return false
		}

		Log.i(TAG, "BLE Peripheral started successfully")
		return true
	}

	/**
	 * Stops the BLE peripheral by ceasing advertisement and closing the GATT server.
	 *
	 * This method:
	 * - Stops BLE advertising
	 * - Closes the GATT server
	 * - Clears all connected devices and notification subscribers
	 * - Releases Bluetooth resources
	 *
	 * It's safe to call this method multiple times.
	 */
	fun stopPeripheral()
	{
		stopAdvertising()
		closeGattServer()
		Log.i(TAG, "BLE Peripheral stopped")
	}

	/**
	 * Checks if all required Bluetooth permissions are granted.
	 *
	 * Required permissions:
	 * - BLUETOOTH
	 * - BLUETOOTH_ADMIN
	 * - BLUETOOTH_ADVERTISE (Android 12+)
	 * - BLUETOOTH_CONNECT (Android 12+)
	 *
	 * @return true if all permissions are granted, false otherwise
	 */
	private fun checkPermissions(): Boolean
	{
		val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		{
			arrayOf(
				Manifest.permission.BLUETOOTH,
				Manifest.permission.BLUETOOTH_ADMIN,
				Manifest.permission.BLUETOOTH_ADVERTISE,
				Manifest.permission.BLUETOOTH_CONNECT
			)
		}
		else
		{
			arrayOf(
				Manifest.permission.BLUETOOTH,
				Manifest.permission.BLUETOOTH_ADMIN
			)
		}

		for (permission in permissions)
		{
			if (ActivityCompat.checkSelfPermission(context, permission) !=
				PackageManager.PERMISSION_GRANTED
			)
			{
				Log.e(TAG, "Missing permission: $permission")
				return false
			}
		}
		return true
	}

	/**
	 * Sets up the GATT server with the primary service and three characteristics.
	 *
	 * Creates:
	 * - Primary service with [SampleBleService.uuid]
	 * - Read characteristic with [SampleReadCharacteristic.uuid]
	 * (READ permission)
	 * - Write characteristic with [SampleWriteCharacteristic.uuid]
	 * (WRITE permission)
	 * - Notify characteristic with [SampleNotifyCharacteristic.uuid]
	 * (NOTIFY property)
	 * - Client Characteristic Configuration descriptor for notifications
	 *
	 * @return
	 *   `true` if GATT server setup was successful, false otherwise
	 * @throws SecurityException if Bluetooth permissions are insufficient
	 */
	private fun setupGattServer(): Boolean
	{
		try
		{
			gattServer = bluetoothManager
				.openGattServer(context, gattServerCallback)
				?: return false

			val service =
				BluetoothGattService(
					SampleBleService.uuid,
					BluetoothGattService.SERVICE_TYPE_PRIMARY
				)

			// Read characteristic
			val readCharacteristic = BluetoothGattCharacteristic(
				SampleReadCharacteristic.uuid,
				BluetoothGattCharacteristic.PROPERTY_READ,
				BluetoothGattCharacteristic.PERMISSION_READ
			)

			// Write characteristic
			val writeCharacteristic = BluetoothGattCharacteristic(
				SampleWriteCharacteristic.uuid,
				BluetoothGattCharacteristic.PROPERTY_WRITE,
				BluetoothGattCharacteristic.PERMISSION_WRITE
			)

			// Notify characteristic - needs both NOTIFY property and READ
			// property
			val notifyCharacteristic = BluetoothGattCharacteristic(
				SampleNotifyCharacteristic.uuid,
				BluetoothGattCharacteristic.PROPERTY_NOTIFY or
					BluetoothGattCharacteristic.PROPERTY_READ,
				BluetoothGattCharacteristic.PERMISSION_READ
			)

			// Add descriptor for notifications with proper permissions
			val configDescriptor = BluetoothGattDescriptor(
				ClientCharacteristicConfiguration.uuid,
				BluetoothGattDescriptor.PERMISSION_READ or
					BluetoothGattDescriptor.PERMISSION_WRITE
			)

			// Set initial value for descriptor (important to prevent some
			// client errors)
			configDescriptor.value =
				BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

			notifyCharacteristic.addDescriptor(configDescriptor)

			service.addCharacteristic(readCharacteristic)
			service.addCharacteristic(writeCharacteristic)
			service.addCharacteristic(notifyCharacteristic)

			val success = gattServer!!.addService(service)
			Log.d(TAG, "Adding GATT service: $success")
			return success
		}
		catch (e: Exception)
		{
			Log.e(
				TAG,
				"Exception setting up GATT server: ${e.message}",
				e
			)
			return false
		}
	}

	/**
	 * Starts BLE advertising to make this peripheral discoverable by BLE clients.
	 *
	 * Advertising configuration:
	 * - Mode: ADVERTISE_MODE_LOW_LATENCY (highest chance of connection)
	 * - TX Power: ADVERTISE_TX_POWER_MEDIUM
	 * - Connectable: true
	 * - Includes device name and service UUID
	 *
	 * @return
	 *   `true` if advertising started successfully, false otherwise
	 * @throws SecurityException if Bluetooth permissions are insufficient
	 */
	private fun startAdvertising(): Boolean
	{
		try
		{
			val advertiseSettings = AdvertiseSettings.Builder()
				.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
				.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
				.setConnectable(true)
				.build()

			// Create advertise data with minimal required information
			val advertiseData = AdvertiseData.Builder()
				.setIncludeDeviceName(true)
				.setIncludeTxPowerLevel(false)
				.addServiceUuid(SampleBleService.asParcelUuid)
				.build()

			// Create scan response data with additional information
			val scanResponse = AdvertiseData.Builder()
				.setIncludeTxPowerLevel(true)
				.build()

			bluetoothAdapter.name = deviceName

			return bluetoothLeAdvertiser?.let { advertiser ->
				advertiser.startAdvertising(
					advertiseSettings,
					advertiseData,
					scanResponse,  // Add scan response data
					advertiseCallback
				)
				true
			} ?: run {
				Log.e(TAG, "BluetoothLeAdvertiser is null")
				false
			}
		}
		catch (e: Exception)
		{
			Log.e(TAG, "Exception starting advertising: ${e.message}", e)
			return false
		}
	}

	/**
	 * Stops BLE advertising, making this peripheral no longer discoverable.
	 *
	 * @throws SecurityException if Bluetooth permissions are insufficient
	 */
	private fun stopAdvertising()
	{
		try
		{
			bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
		}
		catch (e: Exception)
		{
			Log.e(TAG, "Exception stopping advertising: ${e.message}")
		}
	}

	@Suppress("MissingPermission")
	private fun closeGattServer()
	{
		try {
			gattServer?.close()
			gattServer = null
			connectedDevices.clear()
			notificationSubscribers.clear()
		} catch (e: Exception) {
			Log.e(TAG, "Exception closing GATT server: ${e.message}")
		}
	}

	/**
	 * [Mutex] used to ensure that notifications are sent one at a time.
	 */
	private val notificationMutex = Mutex()

	/**
	 * Sends a notification message to all devices subscribed to the notify
	 * characteristic.
	 *
	 * The message will be sent to all devices that have:
	 * 1. Connected to this peripheral
	 * 2. Enabled notifications by writing to the Client Characteristic
	 * Configuration descriptor
	 *
	 * @param text
	 *   The string message to send to subscribed clients
	 * @throws SecurityException if Bluetooth permissions are insufficient
	 */
	@Suppress("DEPRECATION")
	suspend fun sendNotification(text: String)
	{
		notificationMutex.withLock {
			if (notificationSubscribers.isEmpty())
			{
				Log.w(TAG, "No devices subscribed for notifications")
				return
			}

			try
			{
				val service = gattServer?.getService(SampleBleService.uuid)
					?: return
				val characteristic =
					service.getCharacteristic(SampleNotifyCharacteristic.uuid)
						?: return
				val message = Message(text)
				val payload = message.serialize()
				val request = NotifyRequest(mtu, payload)
				var chunkSent = true
				while (request.hasNext())
				{
					val chunk =
						if(chunkSent) request.next() else request.resendBytes()
					if (chunk == null)
					{
						Log.e(
							TAG,
							"Max resend attempts reached, aborting " +
								"notification:\n'$text'")
						return
					}
					characteristic.value = chunk
					if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU)
					{
						for (device in notificationSubscribers)
						{
							val success = gattServer?.notifyCharacteristicChanged(
								device, characteristic, false
							) ?: false

							if (success)
							{
								Log.d(
									TAG,
									"Notification sent to ${device.address}: " +
										text
								)
							}
							else
							{
								chunkSent = false
								Log.e(
									TAG,
									"Failed to send notification to " +
										"${device.address}"
								)
							}
						}
					}
					else
					{
						for (device in notificationSubscribers)
						{
							// For API level > TIRAMISU, notifyCharacteristicChanged returns int status code
							val statusCode = gattServer?.notifyCharacteristicChanged(
								device,
								characteristic,
								false,
								chunk
							) ?: BluetoothGatt.GATT_FAILURE

							if (statusCode == BluetoothGatt.GATT_SUCCESS)
							{
								Log.d(
									TAG,
									"Notification sent to ${device.address}: " +
										text
								)
							}
							else
							{
								Log.e(
									TAG,
									"Failed to send notification to " +
										"${device.address}, status: $statusCode"
								)
							}
						}
					}
				}
			}
			catch (e: Exception)
			{
				Log.e(
					TAG,
					"Exception sending notification: ${e.message}",
					e
				)
			}
		}
	}

	/**
	 * Callback handler for BLE advertising events.
	 *
	 * Handles success and failure events when starting BLE advertising.
	 */
	private val advertiseCallback = object : AdvertiseCallback()
	{
		/**
		 * Called when BLE advertising starts successfully.
		 *
		 * @param settingsInEffect
		 *   The actual advertising settings that took effect
		 */
		override fun onStartSuccess(settingsInEffect: AdvertiseSettings?)
		{
			Log.i(TAG,
				"BLE Advertising started successfully with settings: " +
					"$settingsInEffect")
		}

		/**
		 * Called when BLE advertising fails to start.
		 *
		 * @param errorCode The error code indicating the reason for failure
		 *   1: ADVERTISE_FAILED_DATA_TOO_LARGE
		 *   2: ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
		 *   3: ADVERTISE_FAILED_ALREADY_STARTED
		 *   4: ADVERTISE_FAILED_INTERNAL_ERROR
		 *   5: ADVERTISE_FAILED_FEATURE_UNSUPPORTED
		 */
		override fun onStartFailure(errorCode: Int)
		{
			val errorMessage = when (errorCode) {
				ADVERTISE_FAILED_DATA_TOO_LARGE ->
					"ADVERTISE_FAILED_DATA_TOO_LARGE"
				ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
					"ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
				ADVERTISE_FAILED_ALREADY_STARTED ->
					"ADVERTISE_FAILED_ALREADY_STARTED"
				ADVERTISE_FAILED_INTERNAL_ERROR ->
					"ADVERTISE_FAILED_INTERNAL_ERROR"
				ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
					"ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
				else -> "Unknown error code: $errorCode"
			}
			Log.e(TAG, "BLE Advertising failed: $errorMessage")
		}
	}

	/**
	 * Callback handler for GATT server events.
	 *
	 * Handles all client interactions including connections, characteristic
	 * reads/writes,
	 * and descriptor writes for notification subscriptions.
	 */
	private val gattServerCallback = object : BluetoothGattServerCallback()
	{
		/**
		 * Called when a client device connects to or disconnects from this GATT
		 * server.
		 *
		 * @param device The remote device whose connection state changed
		 * @param status The status of the connection change operation
		 * @param newState The new connection state (CONNECTED or DISCONNECTED)
		 */
		override fun onConnectionStateChange(
			device: BluetoothDevice?,
			status: Int,
			newState: Int
		) {
			super.onConnectionStateChange(device, status, newState)

			device?.let {
				try
				{
					val statusMessage =
						when (status)
						{
							BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS"
							BluetoothGatt.GATT_FAILURE -> "GATT_FAILURE"
							0x85 -> "GATT_INSUFFICIENT_AUTHENTICATION"
							0x86 -> "GATT_INSUFFICIENT_ENCRYPTION"
							0x87 -> "GATT_INVALID_ATTRIBUTE_LENGTH"
							0x80 -> "GATT_INVALID_OFFSET"
							else -> "Status code: $status"
						}

					Log.d(TAG,
						"Connection state change for ${it.address}, " +
							"status: $statusMessage")

					when (newState)
					{
						BluetoothProfile.STATE_CONNECTING ->
						{
							Log.i(TAG,
								"Device connecting: ${it.address}")
							stopAdvertising()
						}
						BluetoothProfile.STATE_CONNECTED ->
						{
							Log.i(TAG,
								"Device connected: ${it.address}")
							connectedDevices.add(it)
						}

						BluetoothProfile.STATE_DISCONNECTED ->
						{
							Log.i(TAG,
								"Device disconnected: ${it.address}")
							connectedDevices.remove(it)
							notificationSubscribers.remove(it)
							
							// Restart advertising if the GATT server is still
							// active
							if (gattServer != null && connectedDevices.isEmpty())
							{
								Log.i(TAG,
									"Restarting advertising after " +
										"disconnection")
								startAdvertising()
							}
						}
					}
				}
				catch (e: SecurityException)
				{
					Log.e(TAG,
						"Security exception in connection state " +
							"change: ${e.message}")
				}
			}
		}

		/**
		 * Called when a client requests to read from a characteristic.
		 *
		 * For the read characteristic, returns the value set via [setReadCharacteristicValue].
		 * For other characteristics, returns a GATT_FAILURE response.
		 *
		 * @param device The remote device that requested the read
		 * @param requestId Unique identifier for this request
		 * @param offset Starting offset for the read operation
		 * @param characteristic The characteristic being read
		 */
		override fun onCharacteristicReadRequest(
			device: BluetoothDevice?,
			requestId: Int,
			offset: Int,
			characteristic: BluetoothGattCharacteristic?
		) {
			super.onCharacteristicReadRequest(
				device, requestId, offset, characteristic)

			try
			{
				when (characteristic?.uuid)
				{
					SampleReadCharacteristic.uuid ->
					{
						val response = readCharacteristicValue
						gattServer?.sendResponse(
							device,
							requestId,
							BluetoothGatt.GATT_SUCCESS,
							offset,
							response
						)
						Log.d(TAG,
							"Read request from ${device?.address}: " +
								String(readCharacteristicValue)
						)
					}
					SampleNotifyCharacteristic.uuid ->
					{
						// Allow reading the notify characteristic
						val notifyValue = "Notification value".toByteArray()
						gattServer?.sendResponse(
							device,
							requestId,
							BluetoothGatt.GATT_SUCCESS,
							offset,
							notifyValue
						)
						Log.d(TAG,
							"Read request for notify characteristic " +
								"from ${device?.address}")
					}
					else ->
					{
						gattServer?.sendResponse(
							device,
							requestId,
							BluetoothGatt.GATT_FAILURE,
							offset,
							null
						)
						Log.d(TAG,
							"Read request for unknown characteristic " +
								"${characteristic?.uuid} from ${device?.address}")
					}
				}
			}
			catch (e: Exception)
			{
				Log.e(TAG,
					"Exception in read request: ${e.message}")
				gattServer?.sendResponse(
					device,
					requestId,
					BluetoothGatt.GATT_FAILURE,
					offset,
					null
				)
			}
		}

		/**
		 * Called when a client requests to write to a characteristic.
		 *
		 * For the write characteristic, accepts the data and notifies the callback.
		 * For other characteristics, returns a GATT_FAILURE response.
		 *
		 * @param device
		 *   The remote device that requested the write
		 * @param requestId
		 *   Unique identifier for this request
		 * @param characteristic
		 *   The characteristic being written to
		 * @param preparedWrite
		 *   Whether this is a prepared write operation
		 * @param responseNeeded
		 *   Whether the client expects a response
		 * @param offset
		 *   Starting offset for the write operation
		 * @param value
		 *   The data being written
		 */
		override fun onCharacteristicWriteRequest(
			device: BluetoothDevice?,
			requestId: Int,
			characteristic: BluetoothGattCharacteristic?,
			preparedWrite: Boolean,
			responseNeeded: Boolean,
			offset: Int,
			value: ByteArray?
		) {
			super.onCharacteristicWriteRequest(
				device,
				requestId,
				characteristic,
				preparedWrite,
				responseNeeded,
				offset,
				value
			)

			try
			{
				when (characteristic?.uuid)
				{
					SampleWriteCharacteristic.uuid ->
					{
						val receivedValue = value ?: return
						var accumulator = writeMessageAccumulator?.apply {
							additionalPayload(value)
						} ?: MessageDeserializer(value)
						Log.d(
							TAG,
							"Write request from ${device?.address}"
						)
						if (accumulator.hasAllBytes)
						{
							_writeValueFlow.value = receivedValue
							accumulator
						}

						if (responseNeeded)
						{
							gattServer?.sendResponse(
								device,
								requestId,
								BluetoothGatt.GATT_SUCCESS,
								offset,
								null
							)
						}
					}

					else ->
					{
						if (responseNeeded)
						{
							gattServer?.sendResponse(
								device,
								requestId,
								BluetoothGatt.GATT_FAILURE,
								offset,
								null
							)
						}
						Log.d(TAG,
							"Write request for unknown characteristic " +
								"${characteristic?.uuid} from ${device?.address}")
					}
				}
			}
			catch (e: Exception)
			{
				Log.e(TAG,
					"Exception in write request: ${e.message}")
				if (responseNeeded)
				{
					gattServer?.sendResponse(
						device,
						requestId,
						BluetoothGatt.GATT_FAILURE,
						offset,
						null
					)
				}
			}
		}

		/**
		 * Called when a client writes to a characteristic descriptor.
		 *
		 * Primarily handles Client Characteristic Configuration descriptor writes
		 * to enable/disable notifications. Updates the notification subscribers list
		 * and notifies the callback of subscription changes.
		 *
		 * @param device The remote device that requested the descriptor write
		 * @param requestId Unique identifier for this request
		 * @param descriptor The descriptor being written to
		 * @param preparedWrite Whether this is a prepared write operation
		 * @param responseNeeded Whether the client expects a response
		 * @param offset Starting offset for the write operation
		 * @param value The data being written to the descriptor
		 */
		override fun onDescriptorWriteRequest(
			device: BluetoothDevice?,
			requestId: Int,
			descriptor: BluetoothGattDescriptor?,
			preparedWrite: Boolean,
			responseNeeded: Boolean,
			offset: Int,
			value: ByteArray?
		) {
			super.onDescriptorWriteRequest(
				device,
				requestId,
				descriptor,
				preparedWrite,
				responseNeeded,
				offset,
				value
			)
			Log.d(TAG,
				"Descriptor write request from ${device?.address} for " +
					"${descriptor?.uuid}, value: ${value?.contentToString()}")
			try
			{
				if (descriptor?.uuid == ClientCharacteristicConfiguration.uuid)
				{
					device?.let {
						val isNotificationEnabled =
							value != null && value.contentEquals(
								BluetoothGattDescriptor
									.ENABLE_NOTIFICATION_VALUE)

						if (isNotificationEnabled)
						{
							notificationSubscribers.add(it)
							Log.d(TAG,
								"Device ${it.address} subscribed to " +
									"notifications")
						}
						else
						{
							notificationSubscribers.remove(it)
							Log.d(TAG,
								"Device ${it.address} unsubscribed from " +
									"notifications")
						}

						// Store the value in the descriptor
						descriptor.value = value
					}

					if (responseNeeded)
					{
						gattServer?.sendResponse(
							device,
							requestId,
							BluetoothGatt.GATT_SUCCESS,
							offset,
							null
						)
					}
				}
				else
				{
					if (responseNeeded)
					{
						gattServer?.sendResponse(
							device,
							requestId,
							BluetoothGatt.GATT_FAILURE,
							offset,
							null
						)
					}
					Log.d(TAG,
						"Write request for unknown descriptor " +
							"${descriptor?.uuid} from ${device?.address}")
				}
			}
			catch (e: Exception)
			{
				Log.e(TAG,
					"Exception in descriptor write request: ${e.message}")
				if (responseNeeded)
				{
					gattServer?.sendResponse(
						device,
						requestId,
						BluetoothGatt.GATT_FAILURE,
						offset,
						null
					)
				}
			}
		}

		override fun onDescriptorReadRequest(
			device: BluetoothDevice?,
			requestId: Int,
			offset: Int,
			descriptor: BluetoothGattDescriptor?
		) {
			super.onDescriptorReadRequest(device, requestId, offset, descriptor)

			try
			{
				if (descriptor?.uuid == ClientCharacteristicConfiguration.uuid) {
					val value = descriptor.value
						?: BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
					gattServer?.sendResponse(
						device,
						requestId,
						BluetoothGatt.GATT_SUCCESS,
						offset,
						value
					)
					Log.d(TAG,
						"Read request for CCCD descriptor " +
							"from ${device?.address}")
				}
				else
				{
					gattServer?.sendResponse(
						device,
						requestId,
						BluetoothGatt.GATT_FAILURE,
						offset,
						null
					)
					Log.d(TAG,
						"Read request for unknown descriptor " +
							"${descriptor?.uuid} from ${device?.address}")
				}
			}
			catch (e: Exception)
			{
				Log.e(TAG,
					"Exception in descriptor read request: ${e.message}")
				gattServer?.sendResponse(
					device,
					requestId,
					BluetoothGatt.GATT_FAILURE,
					offset,
					null
				)
			}
		}

		override fun onMtuChanged(device: BluetoothDevice?, mtu: Int)
		{
			super.onMtuChanged(device, mtu)

			this@SampleBlePeripheral.mtu =
				min(this@SampleBlePeripheral.mtu, mtu)
			Log.d(TAG,
				"MTU size changed to " +
					"${this@SampleBlePeripheral.mtu} for device ${device?.address}")
		}

		override fun onServiceAdded(status: Int, service: BluetoothGattService?)
		{
			super.onServiceAdded(status, service)
			val statusString =
				if (status == BluetoothGatt.GATT_SUCCESS) "GATT_SUCCESS"
				else "GATT_FAILURE (code: $status)"
			Log.d(TAG,
				"Service ${service?.uuid} added with status: $statusString")
		}
	}
}