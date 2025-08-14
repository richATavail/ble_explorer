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
 * **NOTE** The Android SDK does not provide a way to cancel an in-flight GATT
 * request and will only process one request at a time per connection. It
 * maintains an internal timeout for requests that do not complete in a timely
 * fashion that is not configurable. Therefore, cancelling a [BleRequest] only
 * marks it as inactive so that when/if the request does complete, its results
 * are ignored. No other request will be able to be made on the same
 * [BleConnection] until the in-flight request completes or times out.
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

	/**
	 * `true` indicates the request is still active; false indicates it has been
	 * cancelled.
	 */
	var isActive: Boolean = true
		private set

	/**
	 * Cancel this [BleRequest]. This transitions the [isActive] state to `false`.
	 *
	 * **NOTE** This does not actually cancel the underlying GATT request as
	 * Android does not provide a way to do so. It simply marks this request as
	 * inactive so that when/if the request does complete, its results are
	 * ignored. The request will still complete or timeout as per Android's
	 * internal handling. No other request will be able to be made on the same
	 * [BleConnection] until the in-flight request completes or times out.
	 */
	fun cancel()
	{
		isActive = false
	}

	override fun toString(): String = "${this.javaClass.simpleName} : $identifier"
}