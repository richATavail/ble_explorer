package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattCharacteristic
import com.bitwisearts.android.ble.gatt.attribute.common.CommonCharacteristic
import java.util.UUID

/**
 * A [Service] that is not recognized by this application.
 *
 * @author Richard Arriaga
 */
class UnrecognizedService constructor(
	uuid: UUID,
	name: String = "Unrecognized Service",
	gattCharacteristics: List<BluetoothGattCharacteristic> = listOf()
): Service(uuid, name)
{
	override val characteristics: Set<Characteristic> by lazy {
		gattCharacteristics.map {
			CommonCharacteristic[it.uuid]
				?: UnrecognizedCharacteristic(
					it.uuid,
					"Unrecognized Characteristic",
					this@UnrecognizedService,
					setOf())
		}.toSet()
	}
}