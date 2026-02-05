package com.bitwisearts.android.explorer.ble.peripheral

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.peripheral.BaseBlePeripheral
import com.bitwisearts.android.ble.peripheral.PeripheralCharacteristic
import com.bitwisearts.android.ble.peripheral.PeripheralCharacteristicDelegate
import com.bitwisearts.android.ble.peripheral.PeripheralService
import com.bitwisearts.android.explorer.ExplorerApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A refactored BLE peripheral implementation that extends [BaseBlePeripheral]
 * and uses a custom [PeripheralService] that wraps the existing [SampleBleService].
 *
 * This provides equivalent functionality to [SampleBlePeripheral] but leverages
 * the [BaseBlePeripheral] infrastructure.
 *
 * @property context
 *   The Android context used for Bluetooth operations and permission checks
 * @property bluetoothManager
 *   The [BluetoothManager] for managing Bluetooth operations. If not provided,
 *   it will be obtained from the context.
 * @property deviceName
 *   The name that will be advertised to BLE clients. Defaults to "Mine".
 *
 * @author Richard Arriaga
 */
@Suppress("MissingPermission")
class SampleBlePeripheral(
	context: Context = ExplorerApp.app,
	bluetoothManager: BluetoothManager =
		context.getSystemService(Context.BLUETOOTH_SERVICE)
			as BluetoothManager,
	deviceName: String = "Mine"
) : BaseBlePeripheral(
	context = context,
	bluetoothManager = bluetoothManager,
	deviceName = deviceName,
	advertisementService = SampleBleService,
	peripheralServices = setOf(SampleBlePeripheralService())
) {
	companion object {
		/** Log tag used for debugging and error messages */
		private const val TAG = "SampleBlePeripheral"
	}

	override val tag: String = TAG

	/**
	 * The [PeripheralCharacteristic] for the read characteristic.
	 */
	private val readCharacteristic: PeripheralCharacteristic =
		characteristics[SampleReadCharacteristic.uuid]
			?: error("Read characteristic not found")

	/**
	 * The [PeripheralCharacteristic] for the write characteristic.
	 */
	private val writeCharacteristic: SampleBlePeripheralWriteCharacteristic =
		characteristics[SampleWriteCharacteristic.uuid] as? SampleBlePeripheralWriteCharacteristic
			?: error("Write characteristic not found or wrong type")

	/**
	 * The [PeripheralCharacteristic] for the notify characteristic.
	 */
	private val notifyCharacteristic: PeripheralCharacteristic =
		characteristics[SampleNotifyCharacteristic.uuid]
			?: error("Notify characteristic not found")

	/**
	 * The value received when clients write to the write characteristic.
	 */
	val writeValueFlow: StateFlow<ByteArray> = writeCharacteristic.writeValueFlow

	/**
	 * Sets the value that will be returned when clients read from the read
	 * characteristic.
	 *
	 * @param value
	 *   The integer value to set for the read characteristic
	 */
	fun setReadCharacteristicValue(value: Int) {
		readCharacteristic.writeValue(serializeUnsignedInt(value))
		Log.d(TAG, "Set read characteristic value to $value")
	}

	/**
	 * Sends a notification message to all devices subscribed to the notify
	 * characteristic.
	 *
	 * @param text
	 *   The string message to send to subscribed clients
	 */
	suspend fun sendNotification(text: String) {
		val message = Message(text)
		val payload = message.serialize()
		sendNotification(notifyCharacteristic, payload)
	}
}

/**
 * A custom [PeripheralService] that wraps the [SampleBleService] but provides
 * a custom write characteristic that updates a flow.
 */
private class SampleBlePeripheralService : PeripheralService(
	SampleBleService.uuid,
	SampleBleService.name
) {
	override val tag: String = "SampleBlePeripheralService"

	override val peripheralCharacteristics: Set<PeripheralCharacteristic> by lazy {
		SampleBleService.characteristics.map { characteristic ->
			when (characteristic.uuid) {
				SampleWriteCharacteristic.uuid ->
					SampleBlePeripheralWriteCharacteristic(characteristic)
				else ->
					PeripheralCharacteristicDelegate(characteristic)
			}
		}.toSet()
	}
}

/**
 * A custom [PeripheralCharacteristic] for the write characteristic that
 * updates a [StateFlow] when written to. This characteristic handles
 * multi-chunk writes by using [MessageDeserializer] to accumulate message
 * fragments until the complete message is received.
 */
private class SampleBlePeripheralWriteCharacteristic(
	private val characteristic: Characteristic
) : PeripheralCharacteristic(characteristic.uuid, characteristic.name) {
	override val tag: String = "SampleBlePeripheralWriteChar"

	private val _writeValueFlow = MutableStateFlow(byteArrayOf())

	/**
	 * The flow that emits values written to this characteristic.
	 */
	val writeValueFlow: StateFlow<ByteArray> = _writeValueFlow.asStateFlow()

	/**
	 * Accumulator for assembling multi-part write messages.
	 */
	private var messageDeserializer: MessageDeserializer? = null

	override val service: Service
		get() = characteristic.service

	override val descriptors: Set<Descriptor>
		get() = characteristic.descriptors

	override fun writeValue(data: ByteArray) {
		super.writeValue(data)

		// Use MessageDeserializer to accumulate chunks
		val deserializer = messageDeserializer?.also {
			it.additionalPayload(data)
			Log.d(tag, "Added chunk: ${data.size} bytes. Total: ${it.currentReceivedBytes}/${it.accumulator.messageSize}")
		} ?: MessageDeserializer(data).also {
			messageDeserializer = it
			Log.d(tag, "Started new message: ${it.accumulator.messageSize} bytes expected")
		}

		// Check if we have received all chunks
		if (deserializer.hasAllBytes) {
			val completeMessage = deserializer.accumulator.bytes
			_writeValueFlow.value = completeMessage
			messageDeserializer = null
			Log.d(tag, "Write characteristic updated with complete message: ${completeMessage.size} bytes")
		} else {
			Log.d(tag, "Waiting for more chunks: ${deserializer.currentReceivedBytes}/${deserializer.accumulator.messageSize}")
		}
	}
}

