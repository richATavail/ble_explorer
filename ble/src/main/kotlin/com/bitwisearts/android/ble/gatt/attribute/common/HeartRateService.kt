package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.standardUUID
import com.bitwisearts.android.ble.utility.asInt

/**
 * The [CommonService] that exposes the
 * [Heart Rate Service 1.0](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/HRS_v1.0/out/en/index-en.html#)
 * for batteries in a device.
 *
 * @author Richard Arriaga
 */
object HeartRateService: CommonService(standardUUID(0x180D), "Heart Rate Service")
{
	override val characteristics: Set<Characteristic> = setOf(HeartRateMeasurement)
}

/**
 * Contains the heart rate measurement from a heart rate sensor.
 *
 * @author Richard Arriaga
 */
object HeartRateMeasurement: CommonCharacteristic(
	standardUUID(0x2A37), "Heart Rate Measurement")
{
	override val service: Service get() = HeartRateService
	override val descriptors: Set<Descriptor> = setOf()
	override fun stringifyValue(value: ByteArray): String =
		value.asInt.toString()
}