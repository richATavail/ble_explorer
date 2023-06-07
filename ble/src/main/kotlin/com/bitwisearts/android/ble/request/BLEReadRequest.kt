package com.bitwisearts.android.ble.request

import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.AttributeId

/**
 * An abstract [BleRequest] used to read values from a BLE GATT Attribute.
 *
 * @author Richard Arriaga
 *
 * @param Attribute
 *   The type of the GATT Attribute to read from.
 * @param Id
 *   The type of [AttributeId] used to uniquely identify the target [Attribute]
 *   for this [BleRequest].
 */
sealed class BLEReadRequest<Attribute, Id: AttributeId>:
	BleRequest<Attribute, Id>()
{
	/**
	 * Accepts the [value][ByteArray] read from the attribute or `null` if read
	 * fails. Also accepts the [GattStatusCode] that indicates the type of the
	 * result of the read.
	 */
	protected abstract val resultHandler: (ByteArray?, GattStatusCode) -> Unit

	override var isComplete: Boolean = false

	/**
	 * Complete this [BLEReadRequest].
	 *
	 * @param readValue
	 *   The [value][ByteArray] read from the attribute or `null` if read fails.
	 */
	fun complete (readValue: ByteArray?, statusCode: GattStatusCode)
	{
		isComplete = true
		resultHandler(readValue, statusCode)
	}
}
