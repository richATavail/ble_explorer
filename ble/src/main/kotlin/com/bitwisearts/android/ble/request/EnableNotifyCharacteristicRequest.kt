package com.bitwisearts.android.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.KnownGattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicId
import com.bitwisearts.android.ble.gatt.attribute.bleCharacteristicProperties
import com.bitwisearts.android.ble.gatt.attribute.common.ClientCharacteristicConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The [BleRequest] that enables notifications to be received for changes to a
 * [BluetoothGattCharacteristic].
 *
 * @property connection
 *   The [BleConnection] over which to make the request.
 * @property ioScope
 *   The [CoroutineScope] to use for any asynchronous operations.
 * @property resultHandler
 *   The lambda that accepts `true` if notify was successfully turned on for the
 *   target [BluetoothGattCharacteristic]; `false` otherwise. As a second
 *   parameter it accepts the [GattStatusCode] from the descriptor write or
 *   `null` if the request to enable notifications was not made.
 *
 * @author Richard Arriaga
 */
class EnableNotifyCharacteristicRequest constructor(
	override val identifier: CharacteristicId,
	val connection: BleConnection,
	private val ioScope: CoroutineScope,
	val resultHandler: (Boolean, GattStatusCode?) -> Unit
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
			val enabled = gatt.setCharacteristicNotification(attribute, true)
			if (enabled) {
				ioScope.launch {
					connection.submitBleRequest(
						DescriptorWriteRequest(
							identifier.descriptorId(
								ClientCharacteristicConfiguration.uuid
							),
							connection.mtu,
							BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
						) { status ->
							val result = status == KnownGattStatusCode.SUCCESS
							ioScope.launch {
								resultHandler(result, status)
							}
							result
						})
				}
			}
			else resultHandler(false, null)
		}
		else resultHandler(false, null)
	}
}