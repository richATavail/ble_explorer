package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Service
import java.util.UUID

/**
 * A commonly known [Service].
 *
 * @author Richard Arriaga
 *
 * @constructor
 * Construct a [CommonService].
 *
 * @param uuid
 *   The [UUID] that uniquely identifies this [CommonService].
 * @param name
 *   The name of the [CommonService].
 */
sealed class CommonService constructor(
	uuid: UUID,
	name: String
): Service(uuid, name)
{
	companion object
	{
		/** The [Map] of [Service.uuid] to known [CommonService]. */
		private val commonServices by lazy {
			mutableMapOf<UUID, Service>().apply {
				CommonService::class.sealedSubclasses.forEach {
					it.objectInstance?.let { cs ->
						this[cs.uuid] = cs
					}
				}
			}
		}

		/**
		 * Answer the known common [Service] for the given [Service.uuid].
		 *
		 * @param uuid
		 *   The [Service.uuid] to look up.
		 * @return
		 *   The known common [Service] for the given [Service.uuid] or
		 *   `null`` if not known.
		 */
		operator fun get (uuid: UUID): Service? = commonServices[uuid]
	}
}
