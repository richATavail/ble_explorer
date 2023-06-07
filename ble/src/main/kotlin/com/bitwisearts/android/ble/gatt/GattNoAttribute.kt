package com.bitwisearts.android.ble.gatt

import com.bitwisearts.android.ble.gatt.attribute.AttributeId

/**
 * An unofficial [GattStatusCode] that indicates there is no BLE Attribute that
 * is found matching the given Attribute [AttributeId].
 *
 * @author Richard Arriaga
 *
 * @property id
 *   The [AttributeId] that represents the Attribute that was not found.
 */
class GattNoAttribute constructor(private val id: AttributeId): GattStatusCode
{
	override val code: Int get() = Int.MAX_VALUE - 1
	override val name: String get() = "Custom GATT Unrecognized Attribute: $id"
}