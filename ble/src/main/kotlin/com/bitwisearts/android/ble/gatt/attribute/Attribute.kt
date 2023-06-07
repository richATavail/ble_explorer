package com.bitwisearts.android.ble.gatt.attribute

import java.util.UUID

/**
 * Describes a GATT Attribute.
 *
 * @author Richard Arriaga
 */
interface Attribute
{
	/** The [UUID] that uniquely identifies this [Attribute]. */
	val uuid: UUID

	/** The name of this [Attribute]. */
	val name: String
}