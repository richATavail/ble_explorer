package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import java.util.UUID

/**
 * A commonly known [Descriptor].
 *
 * @author Richard Arriaga
 *
 * @constructor
 * Construct a [CommonDescriptor].
 *
 * @param uuid
 *   The [UUID] that uniquely identifies this [CommonDescriptor].
 * @param name
 *   The name of the [CommonDescriptor].
 */
sealed class CommonDescriptor constructor(
	uuid: UUID,
	name: String
): Descriptor(uuid, name)
{
	companion object
	{
		/** The [Map] of [Descriptor.uuid] to known [CommonDescriptor]. */
		internal val commonDescriptors = mutableMapOf<UUID, Descriptor>()

		/**
		 * Answer the known common [Descriptor] for the given [Descriptor.uuid].
		 *
		 * @param uuid
		 *   The [Descriptor.uuid] to look up.
		 * @return
		 *   The known common [Descriptor] for the given [Descriptor.uuid] or
		 *   `null` if not known.
		 */
		operator fun get (uuid: UUID): Descriptor? = commonDescriptors[uuid]
	}
}
