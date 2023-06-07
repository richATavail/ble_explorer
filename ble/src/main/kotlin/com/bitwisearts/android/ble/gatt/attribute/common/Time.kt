package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.standardUUID

/**
 * [CommonService] that exposes the time state of a device. See 
 * [Current Time Service 1.1](https://www.bluetooth.com/specifications/specs/current-time-service-1-1/)
 *
 * @author Richard Arriaga
 */
object CurrentTimeService: CommonService(
	standardUUID(0x1805), "Current Time Service")
{
	override val characteristics: Set<Characteristic> =
		setOf(
			CurrentTime,
			LocalTimeInformation,
			ReferenceTimeInformation)
}

/**
 * [CommonCharacteristic] that returns the current time as recognized by the 
 * connected device.
 *
 * @author Richard Arriaga
 */
object CurrentTime: CommonCharacteristic(
	standardUUID(0x2A2B), "Current Time")
{
	override val service: Service get() = CurrentTimeService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that contains the timezone of the device and the
 * current daylight savings offset (DST).
 *
 * @author Richard Arriaga
 */
object LocalTimeInformation: CommonCharacteristic(
	standardUUID(0x2A0F), "Local Time Information")
{
	override val service: Service get() = CurrentTimeService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that returns the information about the reference time
 * source.
 *
 * @author Richard Arriaga
 */
object ReferenceTimeInformation: CommonCharacteristic(
	standardUUID(0x2A14), "Reference Time Information")
{
	override val service: Service get() = CurrentTimeService
	override val descriptors: Set<Descriptor> = setOf()
}