package com.bitwisearts.android.ble

import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicChangeNotification

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
	var advertisement: Advertisement? = null)
{
	/** Helper for creating a identifying label for logging purposes. */
	val logLabel: String get() =
		"${advertisement?.deviceName ?: ""} (${macAddress})"

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
	 *
	 */
	open val notifyCharacteristics: List<Characteristic> = emptyList()
}