package com.bitwisearts.android.explorer.ble.peripheral

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicChangeNotification
import com.bitwisearts.android.ble.request.ReadRequestResult
import com.bitwisearts.android.ble.request.WriteRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The [BleDevice] representing the [SampleBlePeripheral].
 *
 * @author Richard Arriaga
 */
class SampleBleDevice(
	macAddress: String,
	bluetoothManager: BluetoothManager,
	context: Context,
	ioScope: CoroutineScope,
	defaultScope: CoroutineScope,
	advertisement: Advertisement?
): BleDevice(
	macAddress = macAddress,
	bluetoothManager = bluetoothManager,
	context = context,
	ioScope = ioScope,
	defaultScope = defaultScope,
	advertisement = advertisement
) {
	override val notifyCharacteristics = listOf(SampleNotifyCharacteristic)

	private val _receivedMessage = MutableStateFlow<Message?>(null)

	/**
	 * The most recently received [Message] or `null` if no message has been
	 * received.
	 */
	val receivedMessage: StateFlow<Message?> = _receivedMessage.asStateFlow()

	private var messageDeserializer: MessageDeserializer? = null

	override fun processNotification(
		notification: CharacteristicChangeNotification
	) {
		Log.d(TAG, "Received notification")
		if(notification.characteristic.uuid == SampleNotifyCharacteristic.uuid)
		{
			val deserializer = messageDeserializer?.also {
				it.additionalPayload(notification.value)
			} ?: MessageDeserializer(notification.value).also {
				messageDeserializer = it
			}
			Log.d(TAG,
				"Received notification with ${notification.value.size} bytes")
			if(deserializer.hasAllBytes)
			{
				try
				{
					Log.d(TAG, "Deserializing message")
					_receivedMessage.value = deserializer.deserialize()
					Log.d(TAG, "Deserialized message: ${_receivedMessage.value}")
				}
				catch (e: SerializationException)
				{
					Log.e(TAG, "Error deserializing message", e)
				}
				messageDeserializer = null
			}
			else
			{
				Log.d(
					TAG,
					"Waiting for more bytes. Have " +
						"${deserializer.currentReceivedBytes} of " +
						"${deserializer.messageSize} bytes."
				)
			}
		}
		else
		{
			super.processNotification(notification)
		}
	}

	private val _readMessage = MutableStateFlow("")
	val readMessage: StateFlow<String> = _readMessage.asStateFlow()

	/**
	 * Read the [SampleReadCharacteristic] from this [BleDevice].
	 *
	 * @return
	 *   A [ReadRequestResult] indicating the result of the read request.
	 */
	suspend fun readSampleReadCharacteristic()
	{
		val result = connection.readCharacteristic(SampleReadCharacteristic)
		if (result is ReadRequestResult.ReadSuccess)
		{
			_readMessage.value =
				SampleReadCharacteristic.stringifyValue(result.data)
		}
	}



	/**
	 * Write the given [text] to the [SampleWriteCharacteristic] of this
	 * [BleDevice].
	 */
	suspend fun writeSampleWriteCharacteristic(
		text: String
	): WriteRequestResult =
		connection.writeCharacteristic(
			SampleWriteCharacteristic,
			Message(text).serialize()
		)

	companion object
	{
		private const val TAG = "SampleBleDevice"

		fun isSampleDevice(advertisement: Advertisement): Boolean =
			advertisement.serviceUUIDs.contains(SampleBleService.uuid)
	}
}