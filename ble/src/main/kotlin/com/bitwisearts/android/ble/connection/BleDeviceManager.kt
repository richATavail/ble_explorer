package com.bitwisearts.android.ble.connection

import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.advertisement.Advertisement
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The singleton manager of all known/discovered [BleDevice]s. It is responsible
 * for tracking discovered devices and their advertisements, as well as
 * maintaining the currently selected device for interaction.
 *
 * This manager serves as a central registry for:
 * - Storing discovered BLE advertisements indexed by MAC address
 * - Maintaining references to connected/known BLE devices
 * - Tracking which device is currently selected for interaction
 *
 * @author Richard Arriaga
 */
object BleDeviceManager
{
	/**
	 * The [mac address][BleDevice.macAddress] of the presently selected device
	 * or an empty string if no device is selected. This [MutableStateFlow]
	 * allows UI components to reactively observe changes to the selected device.
	 */
	val selectedAddress = MutableStateFlow("")

	/**
	 * The [Advertisement] of the presently selected device or `null` if no
	 * device is selected. This is derived from the [advertisements] map using
	 * the current [selectedAddress].
	 */
	val selectedAdvertisement: Advertisement? get() =
		advertisements[selectedAddress.value]

	/**
	 * A mutable map from [BleDevice.macAddress] to the associated
	 * [Advertisement] received from that device. This stores all discovered
	 * advertisements from BLE scans, allowing them to be referenced later
	 * when creating connections or displaying device information.
	 */
	val advertisements = mutableMapOf<String, Advertisement>()

	/**
	 * A mutable map from [BleDevice.macAddress] to its associated [BleDevice]
	 * instance. This maintains references to all BLE devices that have been
	 * created, whether currently connected or not, allowing the application
	 * to reuse existing device instances rather than creating duplicates.
	 */
	val devices = mutableMapOf<String, BleDevice>()
}