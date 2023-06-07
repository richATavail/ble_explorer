package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.standardUUID

/**
 * [CommonService] that is a service that can be used to notify the central
 * of changes made to the fundamental structure of services and characteristics
 * on the peripheral.
 *
 * @author Richard Arriaga
 */
object GenericAttribute: CommonService(standardUUID(0x1801), "Generic Attribute")
{
	override val characteristics: Set<Characteristic> =
		setOf(ServiceChanged)
}

/**
 * [CommonCharacteristic] that is the mode by which the central is notified of
 * the service change.
 *
 * @author Richard Arriaga
 */
object ServiceChanged: CommonCharacteristic(
	standardUUID(0x2A05), "Service Changed")
{
	override val service: Service get() = GenericAttribute
	override val descriptors: Set<Descriptor> = setOf()
}