package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.standardUUID

/**
 * The [CommonService] that exposes the
 * [Battery Service 1.0](https://www.bluetooth.com/specifications/specs/battery-service-1-0/)
 * for batteries in a device.
 *
 * @author Richard Arriaga
 */
object Battery: CommonService(standardUUID(0x180F), "Battery Service")
{
	override val characteristics: Set<Characteristic> = setOf(BatteryLevel)
}

/**
 * `BatteryLevel` returns the current battery level as a percentage from 0% to
 * 100%; 0% represents a battery that is fully discharged, 100% represents a
 * battery that is fully charged.
 *
 * @author Richard Arriaga
 */
object BatteryLevel: CommonCharacteristic(
	standardUUID(0x2A19), "Battery Level")
{
	override val service: Service get() = Battery
	override val descriptors: Set<Descriptor> = setOf()
}