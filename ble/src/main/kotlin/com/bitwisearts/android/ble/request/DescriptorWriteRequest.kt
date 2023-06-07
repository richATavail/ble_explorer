package com.bitwisearts.android.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.DescriptorId

/**
 * A [BleWriteRequest] to write to a [BluetoothGattDescriptor].
 *
 * @author Richard Arriaga
 *
 * @constructor
 * Construct a [DescriptorWriteRequest].
 *
 * @param identifier
 *   The [BluetoothGattDescriptor.getUuid] of the characteristic to write
 *   to.
 * @param mtu
 *   The [BleConnection.mtu] used to chunk this message.
 * @param payload
 *   The entire [ByteArray] payload to write to the target GATT Attribute. If
 *   the size of this [ByteArray] exceeds the [mtu], the payload will be sent
 *   in chunks.
 * @param gattResponseHandler
 *   The lambda that accepts the [GattStatusCode] responsible for handling the
 *   response to this [DescriptorWriteRequest].
 */
class DescriptorWriteRequest constructor (
	override val identifier: DescriptorId,
	mtu: Int,
	payload: ByteArray,
	gattResponseHandler: (GattStatusCode) -> Unit
): BleWriteRequest<BluetoothGattDescriptor, DescriptorId>(
	mtu, payload, gattResponseHandler)
{
	@SuppressLint("MissingPermission")
	override fun request(
		gatt: BluetoothGatt,
		attribute: BluetoothGattDescriptor)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			gatt.writeDescriptor(attribute, next())
		}
		else
		{
			attribute.value = next()
			gatt.writeDescriptor(attribute)
		}
	}
}