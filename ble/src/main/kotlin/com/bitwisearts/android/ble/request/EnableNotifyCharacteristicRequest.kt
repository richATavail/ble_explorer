package com.bitwisearts.android.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicId
import com.bitwisearts.android.ble.gatt.attribute.bleCharacteristicProperties

/**
 * The [BleRequest] that enables notifications to be received for changes to a
 * [BluetoothGattCharacteristic].
 *
 * @author Richard Arriaga
 */
class EnableNotifyCharacteristicRequest constructor(
	override val identifier: CharacteristicId,
	val resultHandler: (Boolean) -> Unit
) : BleRequest<BluetoothGattCharacteristic, CharacteristicId>()
{
	override var isComplete: Boolean = false

	@SuppressLint("MissingPermission")
	override fun request(
		gatt: BluetoothGatt,
		attribute: BluetoothGattCharacteristic)
	{
		if (attribute.bleCharacteristicProperties.any { it.supportsNotify })
		{
			resultHandler(gatt.setCharacteristicNotification(attribute, true))
		}
		else resultHandler(false)
	}
}