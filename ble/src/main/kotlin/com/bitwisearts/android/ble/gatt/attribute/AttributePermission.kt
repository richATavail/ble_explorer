package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

/**
 * All the possible permissions of a Bluetooth Low Energy Attribute.
 *
 * @author Richard Arriaga
 *
 * @property mask
 *   The bit mask representing this permission in a Bluetooth GATT Attribute.
 * @property description
 *   A String that describes this property.
 */
enum class AttributePermission (val mask: Int, val description: String)
{
	/** BLE read permission. */
	READ(0x01, "Read Permission")
	{
		override val supportsRead: Boolean = true
	},

	/** Attribute allows encrypted read operations. */
	READ_ENCRYPTED(0x02, "Read Encrypted Permission")
	{
		override val supportsRead: Boolean = true
	},

	/**
	 * Attribute allows encrypted reads with person-in-the-middle protection.
	 */
	READ_ENCRYPTED_MITM(0x04, "Read Encrypted MITM Permission")
	{
		override val supportsRead: Boolean = true
	},

	/** Attribute write permission. */
	WRITE(0x10, "Write Permission")
	{
		override val supportsWrite: Boolean = true
	},

	/** Attribute allows encrypted write operations. */
	WRITE_ENCRYPTED(0x20, "Write Encrypted Permission")
	{
		override val supportsWrite: Boolean = true
	},

	/**
	 * Attribute allows encrypted writes with person-in-the-middle protection.
	 */
	WRITE_ENCRYPTED_MITM(0x40, "Write Encrypted MITM Permission")
	{
		override val supportsWrite: Boolean = true
	};

	/**
	 * `true` indicates this [AttributePermission] enables a read; `false`
	 * otherwise.
	 */
	open val supportsRead = false

	/**
	 * `true` indicates this [AttributePermission] enables a write; `false`
	 * otherwise.
	 */
	open val supportsWrite = false
}

/**
 * Answer the [Set] of [AttributePermission]s for the provided BLE Attribute's
 * permission mask.
 *
 * @param permissionsMask
 *   The integer value that represents all of the Attribute's permissions.
 * @return
 *   The supported set of [AttributePermission].
 */
fun permissions(permissionsMask: Int): Set<AttributePermission> =
	AttributePermission.values().filter {
		it.mask.and(permissionsMask) == it.mask
	}.toSet()

/**
 * The [Set] of [AttributePermission]s set for this
 * [BluetoothGattCharacteristic].
 */
val BluetoothGattCharacteristic.attributePermissions: Set<AttributePermission>
	get() = mutableSetOf<AttributePermission>().apply {
		permissions
	}

/**
 * The [Set] of [AttributePermission]s set for this [BluetoothGattDescriptor].
 */
val BluetoothGattDescriptor.attributePermissions: Set<AttributePermission>
	get() = mutableSetOf<AttributePermission>().apply {
		permissions
	}