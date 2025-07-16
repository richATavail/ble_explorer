package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.UUID

/**
 * An identifier for a GATT Attribute, either [BluetoothGattCharacteristic] or
 * [BluetoothGattDescriptor].
 *
 * @author Richard Arriaga
 */
sealed interface AttributeId
{
	/** The owning [BluetoothGattService.getUuid]. */
	val serviceId: UUID

	/** The associated [BluetoothGattCharacteristic.getUuid]. */
	val characteristicId: UUID
}

/**
 * [AttributeId] that uniquely identifies a [BluetoothGattCharacteristic]
 * relative to its [BluetoothGattService].
 *
 * @author Richard Arriaga
 */
data class CharacteristicId constructor (
	override val serviceId: UUID,
	override val characteristicId: UUID
): AttributeId
{
	/**
	 * Answer a [DescriptorId] that uniquely identifies a
	 * [BluetoothGattDescriptor] for the given [Descriptor.uuid].
	 */
	fun descriptorId(descriptorId: UUID): DescriptorId =
		DescriptorId(serviceId, characteristicId, descriptorId)

	override fun toString(): String =
		"service ($serviceId) - characteristic ($characteristicId)"
}

/**
 * [AttributeId] that uniquely identifies a [BluetoothGattDescriptor]
 * relative to its [BluetoothGattService] - [BluetoothGattCharacteristic].
 *
 * @author Richard Arriaga
 *
 * @property descriptorId
 *   The associated [BluetoothGattDescriptor.getUuid].
 */
data class DescriptorId constructor (
	override val serviceId: UUID,
	override val characteristicId: UUID,
	val descriptorId: UUID
): AttributeId
{
	override fun toString(): String =
		"service ($serviceId) - characteristic ($characteristicId) - " +
			"descriptor ($descriptorId)"
}