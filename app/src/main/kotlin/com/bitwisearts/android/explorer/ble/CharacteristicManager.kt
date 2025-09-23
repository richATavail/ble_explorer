package com.bitwisearts.android.explorer.ble

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.common.CommonCharacteristic
import com.bitwisearts.android.explorer.ble.peripheral.SampleNotifyCharacteristic
import com.bitwisearts.android.explorer.ble.peripheral.SampleReadCharacteristic
import com.bitwisearts.android.explorer.ble.peripheral.SampleWriteCharacteristic
import java.util.UUID

/**
 * Manages known [Characteristic]s. It provides a way to look up known
 * [Characteristic]s by their [Characteristic.uuid].
 *
 * @author Richard Arriaga
 */
object CharacteristicManager
{
	private val knownCharacteristics: Map<UUID, Characteristic> by lazy {
		CommonCharacteristic.allCharacteristics +
			mapOf(
				SampleReadCharacteristic.uuid to SampleReadCharacteristic,
				SampleWriteCharacteristic.uuid to SampleWriteCharacteristic,
				SampleNotifyCharacteristic.uuid to SampleNotifyCharacteristic
			)
	}

	/**
	 * Answer the known [Characteristic] for the given [Characteristic.uuid].
	 *
	 * @param uuid
	 *   The [Characteristic.uuid] to look up.
	 * @return
	 *   The known [Characteristic] for the given [Characteristic.uuid] or
	 *   `null` if not known.
	 */
	operator fun get(uuid: UUID): Characteristic? = knownCharacteristics[uuid]
}