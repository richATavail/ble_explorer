package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattCharacteristic
import java.util.UUID

/**
 * A GATT [Attribute] that represents a [BluetoothGattCharacteristic].
 *
 * @author Richard Arriaga
 */
abstract class Characteristic constructor(
	override val uuid: UUID,
	override val name: String
): Attribute
{
	/** The [Service] that owns this [Characteristic]. */
	abstract val service: Service

	/** The set of [Descriptor]s owned by this [Characteristic]. */
	abstract val descriptors: Set<Descriptor>

	/**
	 * The [CharacteristicId] that uniquely identifies this [Characteristic].
	 */
	val characteristicId: CharacteristicId by lazy {
		CharacteristicId(service.uuid, uuid) }

	override fun toString(): String = "name ($uuid)"
}