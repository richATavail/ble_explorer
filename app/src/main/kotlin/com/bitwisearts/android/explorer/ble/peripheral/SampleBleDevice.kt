package com.bitwisearts.android.explorer.ble.peripheral

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicChangeNotification
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
		if(notification.characteristic.uuid == SampleNotifyCharacteristic.uuid)
		{
			val deserializer = messageDeserializer?.also {
				it.additionalPayload(notification.value)
			} ?: MessageDeserializer(notification.value).also {
				messageDeserializer = it
			}
			if(deserializer.hasAllBytes)
			{
				try
				{
					_receivedMessage.value = deserializer.deserialize()
				}
				catch (e: SerializationException)
				{
					Log.e(TAG, "Error deserializing message", e)
				}
				messageDeserializer = null
			}
		}
		else
		{
			super.processNotification(notification)
		}
	}

	companion object
	{
		private const val TAG = "SampleBleDevice"

		fun isSampleDevice(advertisement: Advertisement): Boolean =
			advertisement.serviceUUIDs.contains(SampleBleService.uuid)
	}
}