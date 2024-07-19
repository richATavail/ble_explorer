package com.bitwisearts.android.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicId

/**
 * A [BleWriteRequest] to write to a [BluetoothGattCharacteristic].
 *
 * @author Richard Arriaga
 *
 * @property writeType
 *   This is used for [API 33][Build.VERSION_CODES.TIRAMISU] and higher in
 *   [BluetoothGatt.writeCharacteristic]. The [BluetoothGattCharacteristic]
 *   write type value:
 *   * [BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT]
 *   * [BluetoothGattCharacteristic.WRITE_TYPE_SIGNED]
 *   * [BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE]
 *
 * @constructor
 * Construct a [CharacteristicWriteRequest].
 *
 * @param identifier
 *   The [BluetoothGattCharacteristic.getUuid] of the characteristic to write
 *   to.
 * @param mtu
 *   The [BleConnection.mtu] used to chunk this message.
 * @param payload
 *   The entire [ByteArray] payload to write to the target GATT Attribute. If
 *   the size of this [ByteArray] exceeds the [mtu], the payload will be sent
 *   in chunks.
 * @param writeType
 *   This is used for [API 33][Build.VERSION_CODES.TIRAMISU] and higher in
 *   [BluetoothGatt.writeCharacteristic]. The [BluetoothGattCharacteristic]
 *   write type value:
 *   * [BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT]
 *   * [BluetoothGattCharacteristic.WRITE_TYPE_SIGNED]
 *   * [BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE]
 * @param gattResponseHandler
 *   The lambda that accepts the [GattStatusCode] responsible for handling the
 *   response to this [CharacteristicWriteRequest].
 */
class CharacteristicWriteRequest constructor (
	override val identifier: CharacteristicId,
	mtu: Int,
	payload: ByteArray,
	val writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
	gattResponseHandler: (GattStatusCode) -> Boolean
): BleWriteRequest<BluetoothGattCharacteristic, CharacteristicId>(
	mtu, payload, gattResponseHandler)
{
	@SuppressLint("MissingPermission")
	override fun request(
		gatt: BluetoothGatt,
		attribute: BluetoothGattCharacteristic)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			gatt.writeCharacteristic(
				attribute,
				next(),
				writeType)
		}
		else
		{
			attribute.value = next()
			gatt.writeCharacteristic(attribute)
		}
	}

	/**
	 * Attempt to resend the [bytesLastSent]. Answer `true` if the payload was
	 * resent, `false` if the maximum number of resend attempts has been
	 * reached.
	 */
	@SuppressLint("MissingPermission")
	fun resendLastPayload(
		gatt: BluetoothGatt,
		attribute: BluetoothGattCharacteristic
	): Boolean {
		val bytes = resendBytes() ?: return false
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			gatt.writeCharacteristic(
				attribute,
				bytes,
				writeType
			)
		} else {
			attribute.value = bytes
			gatt.writeCharacteristic(attribute)
		}
		return true
	}
}