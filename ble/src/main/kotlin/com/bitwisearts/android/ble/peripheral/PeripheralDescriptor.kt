package com.bitwisearts.android.ble.peripheral

import android.util.Log
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.utility.asHex
import java.util.UUID

/**
 * a [Descriptor] used in a [PeripheralCharacteristic]. It has the manages an
 * internal [ByteArray] that holds the value of the characteristic.
 *
 * @author Richard Arriaga
 */
abstract class PeripheralDescriptor(
	uuid: UUID,
	name: String
): Descriptor(uuid, name) {
	/** The tag used for logging. */
	abstract val tag: String

	/**
	 * The default value for the characteristic. This is how the characteristic
	 * will be initialized and reset to its default state.
	 */
	open val defaultValue: ByteArray = byteArrayOf(0)

	/** The current value of this descriptor. */
	var value: ByteArray = defaultValue
		private set
		get() {
			Log.i(tag, "Read value from $name: ${field.asHex}")
			return field
		}

	/** Writes the provided data to the descriptor value. */
	open fun writeValue(data: ByteArray)
	{
		Log.i(
			tag, "Write value to $name: ${data.asHex}"
		)
		value = data
	}

	/** Resets the descriptor value to its [default state][defaultValue]. */
	open fun resetToDefault() {
		value = defaultValue
	}
}