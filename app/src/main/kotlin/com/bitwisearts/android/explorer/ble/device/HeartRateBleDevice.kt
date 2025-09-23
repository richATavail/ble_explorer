package com.bitwisearts.android.explorer.ble.device

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicChangeNotification
import com.bitwisearts.android.ble.gatt.attribute.common.HeartRateMeasurement
import com.bitwisearts.android.ble.gatt.attribute.common.HeartRateService
import com.bitwisearts.android.ble.standardUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The [BleDevice] for a Heart Rate Monitor device. This is determined by the
 * [HeartRateService] being used in the advertisement or GATT services.
 *
 * @author Richard Arriaga
 */
class HeartRateBleDevice(
	macAddress: String,
	bluetoothManager: BluetoothManager,
	context: Context,
	ioScope: CoroutineScope,
	defaultScope: CoroutineScope,
	advertisement: Advertisement? = null
): BleDevice(
	macAddress = macAddress,
	bluetoothManager = bluetoothManager,
	context = context,
	ioScope = ioScope,
	defaultScope = defaultScope,
	advertisement = advertisement
) {
	/**
	 * The [MutableStateFlow] that contains the most recent heart rate data.
	 */
	private val _heartRate = MutableStateFlow(byteArrayOf())

	/**
	 * The [StateFlow] that contains the most recent heart rate data.
	 */
	val heartRate: StateFlow<ByteArray> = _heartRate.asStateFlow()
	override val notifyCharacteristics: List<Characteristic>
		get() = notifyHeartRate

	override fun processNotification(
		notification: CharacteristicChangeNotification
	) {
		_heartRate.value = notification.value
		Log.d(TAG, "Heart rate: ${notification.value}")
	}


	companion object
	{
		private const val TAG = "HeartRateBleDevice"

		private val notifyHeartRate = listOf(HeartRateMeasurement)

		/**
		 * Determines if the provided byte array contains the Heart Rate
		 * Service UUID.
		 *
		 * @param data
		 *   The byte array to check for the Heart Rate Service UUID.
		 * @return
		 *   `true` if the byte array contains the Heart Rate Service UUID,
		 *   `false` otherwise.
		 */
		fun isHeartRateDevice(data: ByteArray): Boolean =
			if (data.size == 2)
			{
				standardUUID(data[1], data[0]) ==
					HeartRateService.uuid
			}
			else false
	}
}