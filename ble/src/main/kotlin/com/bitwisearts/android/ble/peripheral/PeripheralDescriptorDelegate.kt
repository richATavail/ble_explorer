package com.bitwisearts.android.ble.peripheral

import com.bitwisearts.android.ble.gatt.attribute.Descriptor

/**
 * A [PeripheralDescriptor] that wraps a [Descriptor], delegating to that
 * [Descriptor] for core functionality. This allows the wrapping of existing
 * [Descriptor]s allowing them to be used in a [BaseBlePeripheral].
 *
 * @author Richard Arriaga
 *
 * @property descriptor
 *   The [Descriptor] to wrap.
 * @property tag
 *   The tag used for logging. It defaults to the [Descriptor.name].
 */
class PeripheralDescriptorDelegate(
	private val descriptor: Descriptor,
	override val tag: String = descriptor.name
): PeripheralDescriptor(descriptor.uuid, descriptor.name)