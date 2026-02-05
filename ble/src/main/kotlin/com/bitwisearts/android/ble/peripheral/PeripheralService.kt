package com.bitwisearts.android.ble.peripheral

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Service
import java.util.UUID

/**
 * a [Service] used in a [BaseBlePeripheral]. It holds onto
 * [PeripheralCharacteristic]s that can be used to manage the state of a
 * [BaseBlePeripheral].
 *
 * @author Richard Arriaga
 */
abstract class PeripheralService(
	uuid: UUID,
	name: String
): Service(uuid, name) {
	/** The tag used for logging. */
	abstract val tag: String

	/**
	 * The set of [PeripheralCharacteristic]s owned by this [PeripheralService].
	 */
	abstract val peripheralCharacteristics: Set<PeripheralCharacteristic>
	override val characteristics: Set<Characteristic>
		get() = peripheralCharacteristics
}