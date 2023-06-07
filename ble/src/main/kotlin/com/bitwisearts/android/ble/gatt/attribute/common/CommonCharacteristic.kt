package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import java.util.UUID

/**
 * A commonly known [Characteristic].
 *
 * @author Richard Arriaga
 *
 * @constructor
 * Construct a [CommonCharacteristic].
 *
 * @param uuid
 *   The [UUID] that uniquely identifies this [CommonCharacteristic].
 * @param name
 *   The name of the [CommonCharacteristic].
 */
sealed class CommonCharacteristic constructor(
	uuid: UUID,
	name: String
): Characteristic(uuid, name)
{
	companion object
	{
		/**
		 * The [Map] of [Characteristic.uuid] to known [CommonCharacteristic].
		 */
		private val commonCharacteristics by lazy {
			mutableMapOf<UUID, Characteristic>().apply {
				CommonCharacteristic::class.sealedSubclasses.forEach {
					it.objectInstance?.let { cc ->
						this[cc.uuid] = cc
					}
				}
			}
		}

		/**
		 * Answer the known common [Characteristic] for the given
		 * [Characteristic.uuid].
		 *
		 * @param uuid
		 *   The [Characteristic.uuid] to look up.
		 * @return
		 *   The known common [Characteristic] for the given
		 *   [Characteristic.uuid] or `null` if not known.
		 */
		operator fun get (uuid: UUID): Characteristic? =
			commonCharacteristics[uuid]
	}
}
