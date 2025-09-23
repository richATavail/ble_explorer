package com.bitwisearts.android.explorer.ble

import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.gatt.attribute.common.CommonService
import com.bitwisearts.android.explorer.ble.peripheral.SampleBleService
import java.util.UUID

/**
 * Manages known [Service]s. It provides a way to look up known [Service]s by
 * their [Service.uuid].
 *
 * @author Richard Arriaga
 */
object ServiceManager
{
	private val knownServices: Map<UUID, Service> by lazy {
		CommonService.allServices +
			mapOf(
				SampleBleService.uuid to SampleBleService
			)
	}

	/**
	 * Answer the known [Service] for the given [Service.uuid].
	 *
	 * @param uuid
	 *   The [Service.uuid] to look up.
	 * @return
	 *   The known [Service] for the given [Service.uuid] or `null` if not
	 *   known.
	 */
	operator fun get(uuid: UUID): Service? = knownServices[uuid]
}