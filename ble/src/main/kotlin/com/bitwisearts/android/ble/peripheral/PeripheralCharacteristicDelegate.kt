package com.bitwisearts.android.ble.peripheral

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service

/**
 * A [PeripheralCharacteristic] that wraps a [Characteristic], delegating to
 * that [Characteristic] for core functionality. This allows the wrapping of
 * existing [Characteristic]s allowing them to be used in a [BaseBlePeripheral].
 *
 * @author Richard Arriaga
 *
 * @property characteristic
 *   The [Characteristic] to wrap.
 * @property tag
 *   The tag used for logging. Defaults to the [Characteristic.name].
 */
class PeripheralCharacteristicDelegate(
	private val characteristic: Characteristic,
	override val tag: String = characteristic.name
): PeripheralCharacteristic(characteristic.uuid, characteristic.name) {
	/**
	 * The set of [PeripheralDescriptorDelegate]s owned by this
	 * [PeripheralCharacteristicDelegate]. These wrap the wrapped
	 * [characteristic]'s [descriptors][Characteristic.descriptors].
	 */
	val descriptorDelegates: Set<PeripheralDescriptorDelegate> by lazy {
		characteristic.descriptors.map {
			PeripheralDescriptorDelegate(it)
		}.toSet()
	}
	override val properties: Int get() = characteristic.properties
	override val permissions: Int get() = characteristic.permissions
	override val service: Service
		get() = characteristic.service
	override val descriptors: Set<Descriptor> get() = descriptorDelegates
}