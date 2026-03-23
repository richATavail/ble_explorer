package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattCharacteristic
import com.bitwisearts.android.ble.utility.asLiteralHex
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

	/**
	 * The [BluetoothGattCharacteristic] properties supported by this
	 * [Characteristic]. These are constructed using logical or operations
	 * on [BluetoothGattCharacteristic.PROPERTY_READ],
	 * [BluetoothGattCharacteristic.PROPERTY_WRITE], and
	 * [BluetoothGattCharacteristic.PROPERTY_NOTIFY].
	 *
	 * It defaults to supporting all three properties.
	 */
	open val properties: Int get() =
		BluetoothGattCharacteristic.PROPERTY_READ or
			BluetoothGattCharacteristic.PROPERTY_WRITE or
			BluetoothGattCharacteristic.PROPERTY_NOTIFY

	/**
	 * The [BluetoothGattCharacteristic] permissions supported by this
	 * [Characteristic]. These are constructed using logical or operations
	 * on [BluetoothGattCharacteristic.PERMISSION_READ] and
	 * [BluetoothGattCharacteristic.PERMISSION_WRITE].
	 *
	 * It defaults to supporting both read and write.
	 */
	open val permissions: Int get() =
		BluetoothGattCharacteristic.PERMISSION_READ or
			BluetoothGattCharacteristic.PERMISSION_WRITE

	/**
	 * Answer a [DescriptorId] that uniquely identifies the provided
	 * [Descriptor] for this [Characteristic].
	 */
	fun descriptorId(descriptor: Descriptor): DescriptorId =
		DescriptorId(
			serviceId = characteristicId.serviceId,
			characteristicId = characteristicId.characteristicId,
			descriptorId = descriptor.uuid
		)

	override fun toString(): String = "name ($uuid)"

	/**
	 * Convert the given [ByteArray] value of this [Characteristic] into a
	 * human readable [String]. If the [Characteristic] has no special way to
	 * represent its value, the default implementation returns the value as a
	 * literal hex [String] (eg. 0x01AF).
	 * @param value
	 *   The [ByteArray] value to stringify.
	 * @return
	 *   A human readable [String] representation of the given
	 *   [ByteArray] value.
	 */
	open fun stringifyValue (value: ByteArray): String =
		value.asLiteralHex
}