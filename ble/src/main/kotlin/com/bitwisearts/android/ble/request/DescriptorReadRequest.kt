package com.bitwisearts.android.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.DescriptorId

/**
 * [BLEReadRequest] used to read a value from a [BluetoothGattDescriptor].
 *
 * @author Richard Arriaga
 */
class DescriptorReadRequest constructor(
	override val identifier: DescriptorId,
	override val resultHandler: (ByteArray?, GattStatusCode) -> Unit
): BLEReadRequest<BluetoothGattDescriptor, DescriptorId>()
{
	@SuppressLint("MissingPermission")
	override fun request(
		gatt: BluetoothGatt,
		attribute: BluetoothGattDescriptor)
	{
		gatt.readDescriptor(attribute)
	}
}