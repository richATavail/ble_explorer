package com.bitwisearts.android.ble.connection

import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.advertisement.Advertisement
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The singleton manager of all known/discovered [BleDevice]s. It is responsible
 * for tracking the devices based on [Mac Address][BleDevice.macAddress].
 */
object BleDeviceManager
{
	/**
	 * The [mac address][BleDevice.macAddress] of the presently selected device
	 * or an empty string if no  device selected.
	 */
	val selectedAddress = MutableStateFlow("")

	/**
	 * The [Advertisement] of the presently selected device or `null` if no
	 * device selected.
	 */
	val selectedAdvertisement: Advertisement? get() =
		advertisements[selectedAddress.value]

	/**
	 * The map from [BleDevice.macAddress] to an associated [Advertisement].
	 */
	val advertisements = mutableMapOf<String, Advertisement>()

	/** The map from [BleDevice.macAddress] to its associated [BleDevice]. */
	val devices = mutableMapOf<String, BleDevice>()
}