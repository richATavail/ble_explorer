package com.bitwisearts.android.ble.gatt.attribute

import java.util.UUID

/**
 * A [Descriptor] that is not recognized by this application.
 *
 * @author Richard Arriaga
 */
class UnrecognizedDescriptor internal constructor(
	uuid: UUID,
	name: String
): Descriptor(uuid, name)