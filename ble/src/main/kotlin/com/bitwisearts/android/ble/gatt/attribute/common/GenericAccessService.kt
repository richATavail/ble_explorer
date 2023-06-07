package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.standardUUID

/**
 * [CommonService] that is service that exposes basic information about a
 * device.
 *
 * @author Richard Arriaga
 */
object GenericAccessService: CommonService(
	standardUUID(0x1800), "Generic Access Service")
{
	override val name: String = "Generic Access"
	override val characteristics: Set<Characteristic> =
		setOf(
			DeviceName,
			Appearance,
			PeripheralPreferredConnectionParameters)
}

/**
 * [CommonCharacteristic] that provides the of the connected device.
 *
 * @author Richard Arriaga
 */
object DeviceName: CommonCharacteristic(
standardUUID(0x2A00), "Device Name")
{
	override val service: Service get() = GenericAccessService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that provides the device appearance value.
 *
 * @author Richard Arriaga
 */
object Appearance: CommonCharacteristic(
	standardUUID(0x2A01), "Appearance")
{
	override val service: Service get() = GenericAccessService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that holds various parameters used to establish a
 * connection.
 *
 * @author Richard Arriaga
 */
object PeripheralPreferredConnectionParameters: CommonCharacteristic(
	standardUUID(0x2A04),
	"Peripheral Preferred Connection Parameters"
) {
	override val service: Service get() = GenericAccessService
	override val descriptors: Set<Descriptor> = setOf()
}