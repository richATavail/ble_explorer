package com.bitwisearts.android.ble.peripheral

import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service

/**
 * A [PeripheralService] that wraps a [Service], delegating to that [Service]
 * for core functionality. This allows the wrapping of existing [Service]s
 * allowing them to be used in a [BaseBlePeripheral].
 *
 * @author Richard Arriaga
 *
 * @property service
 *   The [Descriptor] to wrap.
 * @property tag
 *   The tag used for logging. It defaults to the [Descriptor.name].
 */
class PeripheralServiceDelegate(
	private val service: Service,
	override val tag: String = service.name
): PeripheralService(service.uuid, service.name) {
	override val peripheralCharacteristics: Set<PeripheralCharacteristic> by lazy {
		service.characteristics.map {
			PeripheralCharacteristicDelegate(it)
		}.toSet()
	}
}