package com.bitwisearts.android.ble.gatt

/**
 * AThe canonical representation of an unrecognized/unknown [GattStatusCode].
 *
 * @author Richard Arriaga
 */
class UnrecognizedGattStatusCode constructor (
	override val code: Int
) : GattStatusCode
{
	override val name: String = "Unrecognized/Invalid GATT Status Code"
}