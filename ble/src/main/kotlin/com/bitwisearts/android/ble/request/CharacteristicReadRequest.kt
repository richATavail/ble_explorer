package com.bitwisearts.android.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicId

/**
 * [BLEReadRequest] used to read a value from a [BluetoothGattCharacteristic].
 *
 * @author Richard Arriaga
 */
class CharacteristicReadRequest constructor(
	override val identifier: CharacteristicId,
	override val resultHandler: (ByteArray?, GattStatusCode) -> Unit
): BLEReadRequest<BluetoothGattCharacteristic, CharacteristicId>()
{
	@SuppressLint("MissingPermission")
	override fun request(
		gatt: BluetoothGatt,
		attribute: BluetoothGattCharacteristic)
	{
		gatt.readCharacteristic(attribute)
	}
}