package com.bitwisearts.android.ble.peripheral

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
import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.gatt.attribute.common.ClientCharacteristicConfiguration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.math.min

/**
 * Abstract base class for implementing a BLE (Bluetooth Low Energy) peripheral
 * on Android. This class provides a complete infrastructure for creating a
 * BLE GATT server that can advertise services and handle connections from
 * central devices.
 *
 * This class handles:
 * - BLE advertising with customizable settings and data
 * - GATT server setup and lifecycle management
 * - Multiple client connection management
 * - Characteristic read/write request handling
 * - Notification/indication support for subscribed clients
 * - MTU negotiation
 * - Permission checking
 *
 * Subclasses must provide:
 * - A unique [tag] for logging purposes
 * - Implementations of [peripheralServices] that define the GATT services
 *   exposed by this peripheral
 *
 * **Usage Example:**
 * ```kotlin
 * class MyPeripheral(context: Context) : BaseBlePeripheral(
 *     context = context,
 *     deviceName = "My Device",
 *     advertisementService = MyService,
 *     peripheralServices = setOf(MyPeripheralService())
 * ) {
 *     override val tag = "MyPeripheral"
 * }
 * ```
 *
 * @property context
 *   The Android [Context] used for Bluetooth operations and permission checks.
 * @property bluetoothManager
 *   The [BluetoothManager] for managing Bluetooth operations. If not provided,
 *   it will be obtained from the context.
 * @property deviceName
 *   The name that will be advertised to BLE clients, making the peripheral
 *   discoverable with this human-readable identifier.
 * @property advertisementService
 *   The primary [Service] UUID to include in the advertisement data. Central
 *   devices often filter scan results by service UUID to find relevant
 *   peripherals.
 * @property peripheralServices
 *   The set of [PeripheralService] implementations that define the GATT
 *   services, characteristics, and descriptors exposed by this peripheral.
 *
 * @author Richard Arriaga
 */
@Suppress("MissingPermission")
abstract class BaseBlePeripheral(
	private val context: Context,
	private val bluetoothManager: BluetoothManager =
		context.getSystemService(Context.BLUETOOTH_SERVICE)
			as BluetoothManager,
	protected val deviceName: String,
	protected val advertisementService: Service,
	protected val peripheralServices: Set<PeripheralService>
)
{
	/**
	 * Log tag used for debugging and error messages. Subclasses must provide
	 * a unique, descriptive tag for logging purposes.
	 */
	abstract val tag: String

	/**
	 * The negotiated MTU (Maximum Transmission Unit) size in bytes for BLE
	 * communication with connected clients. This determines the maximum size
	 * of a single GATT operation. Defaults to
	 * [BleConnection.ADJUSTED_MAX_MTU_SIZE] and is updated when clients
	 * negotiate a different MTU.
	 *
	 * The MTU includes header bytes, so the actual payload size is smaller.
	 * This value is updated per-device when `onMtuChanged` is called.
	 */
	private var mtu = BleConnection.ADJUSTED_MAX_MTU_SIZE

	/**
	 * The [BluetoothAdapter] providing access to Bluetooth functionality on
	 * this Android device.
	 */
	private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

	/**
	 * The [BluetoothLeAdvertiser] used for broadcasting BLE advertisements,
	 * or `null` if BLE advertising is not supported on this device.
	 */
	private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
		bluetoothAdapter.bluetoothLeAdvertiser

	/**
	 * The active [BluetoothGattServer] that handles client connections and
	 * GATT operations, or `null` if the server is not currently running.
	 */
	private var gattServer: BluetoothGattServer? = null

	/**
	 * A set of [BluetoothDevice]s that are currently connected to this
	 * peripheral. Devices are added when they connect and removed when they
	 * disconnect.
	 */
	private val connectedDevices = mutableSetOf<BluetoothDevice>()

	/**
	 * A set of [BluetoothDevice]s that have subscribed to notifications or
	 * indications from this peripheral by enabling the Client Characteristic
	 * Configuration descriptor. Only these devices will receive notifications.
	 */
	private val notificationSubscribers = mutableSetOf<BluetoothDevice>()

	/**
	 * The [PeripheralLog] for this peripheral, which tracks connection events,
	 * read operations, write operations, and other GATT interactions for
	 * debugging and monitoring purposes.
	 */
	val log: PeripheralLog = PeripheralLog()

	/**
	 * The [AdvertiseSettings] used for BLE advertising of this peripheral.
	 * Subclasses can override this to customize advertising mode, TX power,
	 * and other settings.
	 *
	 * Default settings:
	 * - Mode: LOW_LATENCY (fastest advertisement interval)
	 * - TX Power: MEDIUM
	 * - Connectable: true
	 */
	open val advertiseSettings: AdvertiseSettings get() =
		AdvertiseSettings.Builder()
			.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
			.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
			.setConnectable(true)
			.build()

	/**
	 * The primary [AdvertiseData] packet used in BLE advertising. This contains
	 * the essential information that central devices will see when scanning.
	 * Subclasses can override this to customize the advertised data.
	 *
	 * Default data includes:
	 * - Device name
	 * - Primary service UUID from [advertisementService]
	 */
	open val advertiseData: AdvertiseData get() =
		AdvertiseData.Builder()
			.setIncludeDeviceName(true)
			.setIncludeTxPowerLevel(false)
			.addServiceUuid(advertisementService.asParcelUuid)
			.build()

	/**
	 * The scan response [AdvertiseData] used in BLE advertising. This is sent
	 * as a response to active scan requests from central devices and can
	 * contain additional information beyond the primary advertisement.
	 * Subclasses can override this to customize the scan response.
	 *
	 * Default scan response includes:
	 * - TX power level
	 */
	open val scanResponse: AdvertiseData get() =
		AdvertiseData.Builder().setIncludeTxPowerLevel(true).build()

	/**
	 * A map from [PeripheralCharacteristic.uuid] to the corresponding
	 * [PeripheralCharacteristic] instance. This provides quick lookup of
	 * characteristics when handling GATT operations.
	 *
	 * This map is populated from all characteristics defined in the
	 * [peripheralServices].
	 */
	protected val characteristics: Map<UUID, PeripheralCharacteristic> =
		peripheralServices.map { it.peripheralCharacteristics }.flatten()
			.associateBy { it.uuid }

	/**
	 * A map from [PeripheralDescriptor.uuid] to the corresponding
	 * [PeripheralDescriptor] instance. This provides quick lookup of
	 * descriptors when handling GATT operations.
	 *
	 * This map is populated from all descriptors defined in the characteristics
	 * of [peripheralServices].
	 */
	protected val descriptors: Map<UUID, PeripheralDescriptor> =
		characteristics.values.map { d ->
			d.descriptors.map { PeripheralDescriptorDelegate(it) }
		}.flatten().associateBy { it.uuid }

	init
	{
		when
		{
			!bluetoothAdapter.isEnabled ->
			{
				Log.e(tag, "Bluetooth not enabled")
			}

			!context.packageManager.hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE) ->
			{
				Log.e(tag, "BLE not supported")
			}

			else ->
			{
				if (bluetoothLeAdvertiser == null)
				{
					Log.e(tag, "BLE advertising not supported")
				}
			}
		}
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
		Log.i(tag, "Starting BLE Peripheral")
		if (!checkPermissions())
		{
			Log.e(tag, "Missing required permissions")
			return false
		}

		// Make sure we close any existing server first to avoid conflicts
		closeGattServer()

		if (!setupGattServer())
		{
			Log.e(tag, "Failed to setup GATT server")
			return false
		}

		// Stop any existing advertising first
		stopAdvertising()

		if (!startAdvertising())
		{
			Log.e(tag, "Failed to start advertising")
			return false
		}

		Log.i(tag, "BLE Peripheral started successfully")
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
		Log.i(tag, "BLE Peripheral stopped")
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
				Log.e(tag, "Missing permission: $permission")
				return false
			}
		}
		return true
	}

	/**
	 * Sets up the GATT server with the primary service and three characteristics.
	 *
	 * @return
	 *   `true` if GATT server setup was successful, false otherwise
	 * @throws SecurityException if Bluetooth permissions are insufficient
	 */
	protected open fun setupGattServer(): Boolean
	{
		var allSuccess = true
		try
		{
			gattServer = bluetoothManager
				.openGattServer(context, gattServerCallback)
				?: return false

			Log.d(tag, "Setting up GATT server with ${peripheralServices.size} service(s)")

			peripheralServices.forEach { s ->
				Log.d(tag, "Creating service: ${s.name} (${s.uuid})")
				val service =
					BluetoothGattService(
						s.uuid,
						BluetoothGattService.SERVICE_TYPE_PRIMARY
					)

				Log.d(tag, "  Adding ${s.characteristics.size} characteristic(s) to service")
				s.characteristics.forEach { c ->
					Log.d(tag, "    Adding characteristic: ${c.name} (${c.uuid})")
					// We by default make all characteristics maximally
					// accessible to ensure they can be used in any way.
					val cc = BluetoothGattCharacteristic(
						c.uuid,
						c.properties,
						c.permissions
					)
					c.descriptors.forEach { d ->
						Log.d(tag, "      Adding descriptor: ${d.name} (${d.uuid})")
						val descriptor = BluetoothGattDescriptor(
							d.uuid,
							BluetoothGattDescriptor.PERMISSION_READ or
								BluetoothGattDescriptor.PERMISSION_WRITE
						)
						// Initialize CCCD descriptors to prevent client errors
						if (d.uuid == ClientCharacteristicConfiguration.uuid) {
							descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
							Log.d(tag, "        Initialized CCCD descriptor to DISABLED")
						}
						cc.addDescriptor(descriptor)
					}
					service.addCharacteristic(cc)
				}

				// Add the complete service once after all characteristics are added
				val success = gattServer!!.addService(service)
				Log.d(tag, "  Added service ${s.name} to GATT server: $success")
				allSuccess = allSuccess && success
			}

			Log.d(tag, "GATT server setup complete. Overall success: $allSuccess")
			return allSuccess
		}
		catch (e: Exception)
		{
			Log.e(
				tag,
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
	open fun startAdvertising(): Boolean
	{
		try
		{
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
				Log.e(tag, "BluetoothLeAdvertiser is null")
				false
			}
		}
		catch (e: Exception)
		{
			Log.e(tag, "Exception starting advertising: ${e.message}", e)
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
			Log.e(tag, "Exception stopping advertising: ${e.message}")
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
			Log.e(tag, "Exception closing GATT server: ${e.message}")
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
	 * @param characteristic
	 *   The [Characteristic] to notify.
	 * @param payload
	 *   The [ByteArray] payload to send. This may be sent in chunks depending
	 *   on the size of the payload relative to the mtu.
	 * @throws SecurityException if Bluetooth permissions are insufficient
	 */
	@Suppress("DEPRECATION")
	suspend fun sendNotification(
		characteristic: Characteristic,
		payload: ByteArray
	)
	{

		notificationMutex.withLock {
			if (notificationSubscribers.isEmpty())
			{
				Log.w(tag, "No devices subscribed for notifications")
				return
			}

			try
			{
				val service = gattServer?.getService(characteristic.service.uuid)
					?: return
				val characteristic =
					service.getCharacteristic(characteristic.uuid)
						?: return
				val request = SendDataRequest(mtu, payload)
				var chunkSent = true
				while (request.hasNext())
				{
					val chunk =
						if(chunkSent) request.next() else request.resendBytes()
					if (chunk == null)
					{
						Log.e(
							tag,
							"Max resend attempts reached, aborting " +
								"notification to ${characteristic.uuid}")
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
									tag,
									"Notification sent to ${device.address}"
								)
							}
							else
							{
								chunkSent = false
								Log.e(
									tag,
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
									tag,
									"Notification sent to ${device.address}"
								)
							}
							else
							{
								Log.e(
									tag,
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
					tag,
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
			Log.i(tag,
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
			Log.e(tag, "BLE Advertising failed: $errorMessage")
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

					Log.d(tag,
						"Connection state change for ${it.address}, " +
							"status: $statusMessage")

					when (newState)
					{
						BluetoothProfile.STATE_CONNECTING ->
						{
							Log.i(tag,
								"Device connecting: ${it.address}")
							stopAdvertising()
						}
						BluetoothProfile.STATE_CONNECTED ->
						{
							Log.i(tag,
								"Device connected: ${it.address}")
							connectedDevices.add(it)
						}

						BluetoothProfile.STATE_DISCONNECTED ->
						{
							Log.i(tag,
								"Device disconnected: ${it.address}")
							connectedDevices.remove(it)
							notificationSubscribers.remove(it)
							
							// Restart advertising if the GATT server is still
							// active
							if (gattServer != null && connectedDevices.isEmpty())
							{
								Log.i(tag,
									"Restarting advertising after " +
										"disconnection")
								startAdvertising()
							}
						}
					}
				}
				catch (e: SecurityException)
				{
					Log.e(tag,
						"Security exception in connection state " +
							"change: ${e.message}")
				}
			}
		}

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
				characteristic?.uuid?.let { id ->
					characteristics[id]?.let {
						val request = SendDataRequest(
							mtu,
							it.value
						)
						var chunkSent: Boolean? = true
						while (request.hasNext())
						{
							val chunk =
								if(chunkSent == true) request.next()
								else request.resendBytes()
							if (chunk == null)
							{
								Log.e(
									tag,
									"Max resend attempts reached, aborting " +
										"notification to ${characteristic.uuid}")
								return
							}
							chunkSent = gattServer?.sendResponse(
								device,
								requestId,
								BluetoothGatt.GATT_SUCCESS,
								offset,
								chunk
							)
						}

						Log.d(tag,
							"Read request sent to ${device?.address}"
						)
						return
					} ?: Log.d(tag,
						"Read request for unknown characteristic " +
							"from${device?.address}: $"
					)

				} ?: Log.d(tag,
					"Read request for null characteristic from ${device?.address}")

				gattServer?.sendResponse(
					device,
					requestId,
					BluetoothGatt.GATT_FAILURE,
					offset,
					null
				)
			}
			catch (e: Exception)
			{
				Log.e(tag,
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

				characteristic?.uuid?.let { id ->
					characteristics[id]?.let { c ->
						value?.let {
							c.writeValue(it)
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
					} ?: Log.d(tag,
						"Read request for unknown characteristic " +
							"from${device?.address}: $"
					)
				} ?: Log.d(tag,
					"Read request for null characteristic from ${device?.address}")

				gattServer?.sendResponse(
					device,
					requestId,
					BluetoothGatt.GATT_FAILURE,
					offset,
					null
				)
			}
			catch (e: Exception)
			{
				Log.e(tag,
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
			Log.d(tag,
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
							Log.d(tag,
								"Device ${it.address} subscribed to " +
									"notifications")
						}
						else
						{
							notificationSubscribers.remove(it)
							Log.d(tag,
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
					// At this point no attempt is made to support other
					// descriptors as this isn't intended to be an actual
					// library people use.
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
					Log.d(tag,
						"Write request for unknown descriptor " +
							"${descriptor?.uuid} from ${device?.address}")
				}
			}
			catch (e: Exception)
			{
				Log.e(tag,
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
					Log.d(tag,
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
					Log.d(tag,
						"Read request for unknown descriptor " +
							"${descriptor?.uuid} from ${device?.address}")
				}
			}
			catch (e: Exception)
			{
				Log.e(tag,
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

			this@BaseBlePeripheral.mtu =
				min(this@BaseBlePeripheral.mtu, mtu)
			Log.d(tag,
				"MTU size changed to " +
					"${this@BaseBlePeripheral.mtu} for device ${device?.address}")
		}

		override fun onServiceAdded(status: Int, service: BluetoothGattService?)
		{
			super.onServiceAdded(status, service)
			val statusString =
				if (status == BluetoothGatt.GATT_SUCCESS) "GATT_SUCCESS"
				else "GATT_FAILURE (code: $status)"
			Log.d(tag,
				"Service ${service?.uuid} added with status: $statusString")
		}
	}
}