package com.bitwisearts.android.ble.peripheral

import android.util.Log
import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.utility.MessageAccumulator
import com.bitwisearts.android.ble.utility.asHex
import java.util.UUID


/**
 * a [Characteristic] used in a [BaseBlePeripheral]. It has the manages an
 * internal [ByteArray] that holds the value of the characteristic.
 *
 * Any special chunking of data due to MTU transmission size limitations would
 * need to be handled within the implementor of this abstract class. Use the
 * [MessageAccumulator] for receiving and handling fragmented messages.
 *
 * @author Richard Arriaga
 */
abstract class PeripheralCharacteristic(
	uuid: UUID,
	name: String
): Characteristic(uuid, name) {
	/** The tag used for logging. */
	abstract val tag: String

	/**
	 * The default value for the characteristic. This is how the characteristic
	 * will be initialized and reset to its default state.
	 */
	open val defaultValue: ByteArray = byteArrayOf(0)

	/** The current value of the characteristic. */
	var value: ByteArray = defaultValue
		private set
		get() {
			Log.i(tag, "Read value from $name: ${field.asHex}")
			return field
		}

	/** Writes the provided data to the characteristic value. */
	open fun writeValue(data: ByteArray)
	{
		Log.i(
			tag, "Write value to $name: ${data.asHex}"
		)
		value = data
	}

	/** Resets the characteristic value to its [default state][defaultValue]. */
	open fun resetToDefault() {
		value = defaultValue
	}
}