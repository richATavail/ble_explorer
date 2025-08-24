package com.bitwisearts.android.explorer.ble.peripheral

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

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
class BlePeripheral(
	private val context: Context,
	private val bluetoothManager: BluetoothManager =
		context.getSystemService(Context.BLUETOOTH_SERVICE)
			as BluetoothManager,
	private val deviceName: String = "BLE Peripheral",
)
{
	companion object
	{
		/** Log tag used for debugging and error messages */
		private const val TAG = "BLEPeripheral"

		/**
		 * Primary service [UUID] for the BLE peripheral. This UUID is
		 * advertised and used to identify the service.
		 */
		val SERVICE_UUID: UUID =
			UUID.fromString("12345678-1234-5678-9abc-def123456789")

		/**
		 * [UUID] that identifies the read characteristic.
		 * Clients can read data from this characteristic.
		 */
		val READ_CHARACTERISTIC_UUID: UUID =
			UUID.fromString("12345678-1234-5678-9abc-def123456790")

		/**
		 * [UUID] that identifies the write characteristic.
		 * Clients can write data to this characteristic.
		 */
		val WRITE_CHARACTERISTIC_UUID: UUID =
			UUID.fromString("12345678-1234-5678-9abc-def123456791")

		/**
		 * [UUID] that identifies the notify characteristic.
		 * Used to send notifications to subscribed clients.
		 */
		val NOTIFY_CHARACTERISTIC_UUID: UUID =
			UUID.fromString("12345678-1234-5678-9abc-def123456792")

		/**
		 * Standard [UUID] for Client Characteristic Configuration Descriptor.
		 * Used by clients to enable/disable notifications.
		 */
		val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
			UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
	}

	/** Bluetooth adapter for Bluetooth functionality */
	private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

	/** BLE advertiser for broadcasting the peripheral */
	private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
		bluetoothAdapter.bluetoothLeAdvertiser

	/** GATT server that handles client connections and requests */
	private var gattServer: BluetoothGattServer? = null
	/** The device name that will be advertised to BLE clients */
	/** The value returned when clients read from the read characteristic */
	private var readCharacteristicValue: ByteArray =
		"Hello from BLE!".toByteArray()

	private val _writeValueFlow = MutableStateFlow(byteArrayOf(0x01))
	/** The value received when clients write to the write characteristic */
	val writeValueFlow: StateFlow<ByteArray> = _writeValueFlow.asStateFlow()

	/** Set of currently connected BLE devices */
	private val connectedDevices = mutableSetOf<BluetoothDevice>()

	/** Set of devices that have subscribed to notifications */
	private val notificationSubscribers = mutableSetOf<BluetoothDevice>()

	init
	{
		when
		{
			!bluetoothAdapter.isEnabled ->
			{
				Log.e(TAG, "Bluetooth not enabled")
			}

			!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ->
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
	 *   The [ByteArray] to return for read requests
	 */
	fun setReadCharacteristicValue(value: ByteArray)
	{
		this.readCharacteristicValue = value
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
		if (!checkPermissions())
		{
			Log.e(TAG, "Missing required permissions")
			return false
		}

		if (!setupGattServer())
		{
			Log.e(TAG, "Failed to setup GATT server")
			return false
		}

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
	@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
	 * - Primary service with [SERVICE_UUID]
	 * - Read characteristic with [READ_CHARACTERISTIC_UUID] (READ permission)
	 * - Write characteristic with [WRITE_CHARACTERISTIC_UUID] (WRITE permission)
	 * - Notify characteristic with [NOTIFY_CHARACTERISTIC_UUID] (NOTIFY property)
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
			val service =
				BluetoothGattService(
					SERVICE_UUID,
					BluetoothGattService.SERVICE_TYPE_PRIMARY
				)
			// Read characteristic
			val readCharacteristic = BluetoothGattCharacteristic(
				READ_CHARACTERISTIC_UUID,
				BluetoothGattCharacteristic.PROPERTY_READ,
				BluetoothGattCharacteristic.PERMISSION_READ
			)
			// Write characteristic
			val writeCharacteristic = BluetoothGattCharacteristic(
				WRITE_CHARACTERISTIC_UUID,
				BluetoothGattCharacteristic.PROPERTY_WRITE,
				BluetoothGattCharacteristic.PERMISSION_WRITE
			)
			// Notify characteristic
			val notifyCharacteristic = BluetoothGattCharacteristic(
				NOTIFY_CHARACTERISTIC_UUID,
				BluetoothGattCharacteristic.PROPERTY_NOTIFY,
				BluetoothGattCharacteristic.PERMISSION_READ
			)
			// Add descriptor for notifications
			val configDescriptor = BluetoothGattDescriptor(
				CLIENT_CHARACTERISTIC_CONFIG_UUID,
				BluetoothGattDescriptor.PERMISSION_READ
					or BluetoothGattDescriptor.PERMISSION_WRITE
			)
			notifyCharacteristic.addDescriptor(configDescriptor)

			service.addCharacteristic(readCharacteristic)
			service.addCharacteristic(writeCharacteristic)
			service.addCharacteristic(notifyCharacteristic)

			return gattServer!!.addService(service)
		}
		catch (e: SecurityException)
		{
			Log.e(
				TAG,
				"Security exception setting up GATT server: ${e.message}"
			)
			return false
		}
	}

	/**
	 * Starts BLE advertising to make this peripheral discoverable by BLE clients.
	 *
	 * Advertising configuration:
	 * - Mode: ADVERTISE_MODE_BALANCED (balanced power/latency)
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
				.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
				.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
				.setConnectable(true)
				.build()
			val advertiseData = AdvertiseData.Builder()
				.setIncludeDeviceName(true)
				.setIncludeTxPowerLevel(false)
				.addServiceUuid(ParcelUuid(SERVICE_UUID))
				.build()
			bluetoothAdapter.name = deviceName

			bluetoothLeAdvertiser!!.startAdvertising(
				advertiseSettings,
				advertiseData,
				advertiseCallback
			)
			return true
		}
		catch (e: SecurityException)
		{
			Log.e(TAG,
				"Security exception starting advertising: ${e.message}")
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
		catch (e: SecurityException)
		{
			Log.e(TAG, "Security exception stopping advertising: ${e.message}")
		}
	}

	/**
	 * Closes the GATT server and cleans up all connection-related resources.
	 *
	 * This method:
	 * - Closes the GATT server
	 * - Clears the connected devices set
	 * - Clears the notification subscribers set
	 * - Sets gattServer reference to null
	 */
	@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
	private fun closeGattServer()
	{
		gattServer?.close()
		gattServer = null
		connectedDevices.clear()
		notificationSubscribers.clear()
	}

	/**
	 * Sends a notification message to all devices subscribed to the notify
	 * characteristic.
	 *
	 * The message will be sent to all devices that have:
	 * 1. Connected to this peripheral
	 * 2. Enabled notifications by writing to the Client Characteristic
	 * Configuration descriptor
	 *
	 * @param message
	 *   The string message to send to subscribed clients
	 * @throws SecurityException if Bluetooth permissions are insufficient
	 */
	fun sendNotification(message: String)
	{
		if (notificationSubscribers.isEmpty())
		{
			Log.w(TAG, "No devices subscribed for notifications")
			return
		}

		try
		{
			val service = gattServer?.getService(SERVICE_UUID)
				?: return
			val characteristic =
				service.getCharacteristic(NOTIFY_CHARACTERISTIC_UUID)
					?: return

			val messageBytes = message.toByteArray()
			if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU)
			{
				characteristic.value = messageBytes

				for (device in notificationSubscribers)
				{
					gattServer?.notifyCharacteristicChanged(
						device, characteristic, false)
					Log.d(TAG,
						"Notification sent to ${device.address}: $message")
				}
			}
			else
			{
				characteristic.setValue(messageBytes)

				for (device in notificationSubscribers)
				{
					gattServer?.notifyCharacteristicChanged(
						device,
						characteristic,
						false,
						messageBytes)
					Log.d(TAG,
						"Notification sent to ${device.address}: $message")
				}
			}
		}
		catch (e: SecurityException)
		{
			Log.e(TAG,
				"Security exception sending notification: ${e.message}",
				e)
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
		 * @param settingsInEffect The actual advertising settings that took effect
		 */
		override fun onStartSuccess(settingsInEffect: AdvertiseSettings?)
		{
			Log.i(TAG, "BLE Advertising started successfully")
		}

		/**
		 * Called when BLE advertising fails to start.
		 *
		 * @param errorCode The error code indicating the reason for failure
		 */
		override fun onStartFailure(errorCode: Int)
		{
			Log.e(TAG, "BLE Advertising failed with error code: $errorCode")
		}
	}

	/**
	 * Callback handler for GATT server events.
	 *
	 * Handles all client interactions including connections, characteristic reads/writes,
	 * and descriptor writes for notification subscriptions.
	 */
	private val gattServerCallback = object : BluetoothGattServerCallback()
	{
		/**
		 * Called when a client device connects to or disconnects from this GATT server.
		 *
		 * @param device The remote device whose connection state changed
		 * @param status The status of the connection change operation
		 * @param newState The new connection state (CONNECTED or DISCONNECTED)
		 */
		override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int)
		{
			super.onConnectionStateChange(device, status, newState)

			device?.let {
				try
				{
					when (newState)
					{
						BluetoothProfile.STATE_CONNECTED ->
						{
							Log.i(TAG, "Device connected: ${it.address}")
							connectedDevices.add(it)
						}

						BluetoothProfile.STATE_DISCONNECTED ->
						{
							Log.i(TAG, "Device disconnected: ${it.address}")
							connectedDevices.remove(it)
							notificationSubscribers.remove(it)
						}
					}
				}
				catch (e: SecurityException)
				{
					Log.e(TAG, "Security exception in connection state change: ${e.message}")
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
		)
		{
			super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

			try
			{
				when (characteristic?.uuid)
				{
					READ_CHARACTERISTIC_UUID ->
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
							"Read request from ${device?.address}: $readCharacteristicValue")
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
					}
				}
			}
			catch (e: SecurityException)
			{
				Log.e(TAG,
					"Security exception in read request: ${e.message}")
			}
		}

		/**
		 * Called when a client requests to write to a characteristic.
		 *
		 * For the write characteristic, accepts the data and notifies the callback.
		 * For other characteristics, returns a GATT_FAILURE response.
		 *
		 * @param device The remote device that requested the write
		 * @param requestId Unique identifier for this request
		 * @param characteristic The characteristic being written to
		 * @param preparedWrite Whether this is a prepared write operation
		 * @param responseNeeded Whether the client expects a response
		 * @param offset Starting offset for the write operation
		 * @param value The data being written
		 */
		override fun onCharacteristicWriteRequest(
			device: BluetoothDevice?,
			requestId: Int,
			characteristic: BluetoothGattCharacteristic?,
			preparedWrite: Boolean,
			responseNeeded: Boolean,
			offset: Int,
			value: ByteArray?
		)
		{
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
					WRITE_CHARACTERISTIC_UUID ->
					{
						val receivedValue = value ?: return
						Log.d(TAG,
							"Write request from ${device?.address}: $receivedValue")
						_writeValueFlow.value = receivedValue

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
					}
				}
			}
			catch (e: SecurityException)
			{
				Log.e(TAG,
					"Security exception in write request: ${e.message}")
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
		)
		{
			super.onDescriptorWriteRequest(
				device,
				requestId,
				descriptor,
				preparedWrite,
				responseNeeded,
				offset,
				value
			)

			try
			{
				if (descriptor?.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID)
				{
					device?.let {
						val isNotificationEnabled =
							value != null && value.contentEquals(
								BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)

						if (isNotificationEnabled)
						{
							notificationSubscribers.add(it)
							Log.d(TAG,
								"Device ${it.address} subscribed to notifications")
						} else
						{
							notificationSubscribers.remove(it)
							Log.d(TAG,
								"Device ${it.address} unsubscribed from notifications")
						}
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
				}
			}
			catch (e: SecurityException)
			{
				Log.e(TAG,
					"Security exception in descriptor write request: ${e.message}")
			}
		}
	}
}