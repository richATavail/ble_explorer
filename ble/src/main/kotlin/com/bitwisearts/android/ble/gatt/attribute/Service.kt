package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattService
import java.util.UUID

/**
 * A GATT [Attribute] that represents a [BluetoothGattService].
 *
 * @author Richard Arriaga
 */
abstract class Service constructor(
	override val uuid: UUID,
	override val name: String
): Attribute
{
	/** The [Set] of [Characteristic]s owned by this [Service]. */
	abstract val characteristics: Set<Characteristic>

	override fun toString(): String = "name ($uuid)"
}