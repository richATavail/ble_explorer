package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattDescriptor
import java.util.UUID

/**
 * A GATT [Attribute] that represents a [BluetoothGattDescriptor].
 *
 * @author Richard Arriaga
 */
abstract class Descriptor constructor(
	override val uuid: UUID,
	override val name: String
): Attribute
{
	override fun toString(): String = "name ($uuid)"
}