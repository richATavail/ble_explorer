package com.bitwisearts.android.ble.request

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.gatt.attribute.AttributeId

/**
 * The representation of a request made to a [BleDevice] over a [BleConnection].
 * This represents a common hierarchy for both requests targeting both
 * [BluetoothGattCharacteristic] and [BluetoothGattDescriptor].
 *
 * @author Richard Arriaga
 *
 * @param Attribute
 *   The type of GATT Attribute that is the target of this [BleRequest].
 * @param Id
 *   The type of [AttributeId] used to uniquely identify the target [Attribute]
 *   for this [BleRequest].
 */
sealed class BleRequest<Attribute, Id: AttributeId>
{
	/**
	 * The [AttributeId] that represents the target Bluetooth GATT Attribute for
	 * this [BleRequest].
	 */
	abstract val identifier: Id

	/**
	 * Submit this [BleRequest] to the provided [BluetoothGatt].
	 *
	 * @param gatt
	 *   The [BluetoothGatt] that represents the active connection.
	 * @param attribute
	 *   The [Attribute] that is the target of this [BleRequest].
	 */
	abstract fun request (gatt: BluetoothGatt, attribute: Attribute)

	/**
	 * `true` indicates the request has been made and completed; `false`
	 * indicates the request has either not been made or it has been made but
	 * not completed.
	 */
	internal abstract val isComplete: Boolean

	override fun toString(): String = "${this.javaClass.simpleName} : $identifier"
}