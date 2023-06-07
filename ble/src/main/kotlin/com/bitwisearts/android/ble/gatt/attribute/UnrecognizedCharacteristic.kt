package com.bitwisearts.android.ble.gatt.attribute

import java.util.UUID

/**
 * A [Characteristic] that is not recognized by this application.
 *
 * @author Richard Arriaga
 */
class UnrecognizedCharacteristic constructor(
	uuid: UUID,
	name: String,
	override val service: Service,
	override val descriptors: Set<Descriptor>
): Characteristic(uuid, name)