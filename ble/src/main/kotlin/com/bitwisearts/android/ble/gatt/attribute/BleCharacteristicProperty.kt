package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattCharacteristic

/**
 * The possible properties of a [BluetoothGattCharacteristic].
 *
 * @author Richard Arriaga
 * 
 * @property mask
 *   The bit mask representing this permission in a 
 *   [BluetoothGattCharacteristic].
 * @property description
 *   A String that describes this [BleCharacteristicProperty].
 */
enum class BleCharacteristicProperty(val mask: Int, val description: String)
{
	/** Indicates that the [BluetoothGattCharacteristic] can be broadcast. */
	BROADCAST(0x01, "Broadcast"),

	/** Indicates the [BluetoothGattCharacteristic] is readable. */
	READ(0x02, "Read")
	{
		override val supportsRead: Boolean = true
	},

	/**
	 * Indicates the [BluetoothGattCharacteristic] can be written to without
	 * response.
	 */
	WRITE_NO_RESPONSE(0x04, "Write No Response")
	{
		override val supportsWrite: Boolean = true
	},

	/** Indicates the [BluetoothGattCharacteristic] can be written to. */
	WRITE(0x08, "Write")
	{
		override val supportsWrite: Boolean = true
	},

	/** Indicates the [BluetoothGattCharacteristic] supports notification. */
	NOTIFY(0x10, "Notify")
	{
		override val supportsNotify: Boolean = true
	},

	/**
	 * Indicates the [BluetoothGattCharacteristic] supports indication. This is
	 * a [NOTIFY] that requires a response.
	 */
	INDICATE(0x20, "Indicate")
	{
		override val supportsNotify: Boolean = true
	},

	/**
	 * Allows clients to use the Signed Write Command ATT operation on this
	 * [BluetoothGattCharacteristic].
	 */
	SIGNED_WRITE(0x40, "Signed Write Command")
	{
		override val supportsWrite: Boolean = true
	},

	/** Indicates the [BluetoothGattCharacteristic] has extended properties. */
	EXTENDED_PROPERTIES(0x80, "Extended Properties");

	/**
	 * `true` indicates this [BleCharacteristicProperty] enables a read; `false`
	 * otherwise.
	 */
	open val supportsRead = false

	/**
	 * `true` indicates this [BleCharacteristicProperty] enables a write; `false`
	 * otherwise.
	 */
	open val supportsWrite = false

	/**
	 * `true` if the [BluetoothGattCharacteristic] supports sending
	 * notifications; `false` otherwise.
	 */
	open val supportsNotify = false
}

/**
 * The [Set] of [BleCharacteristicProperty]s for this
 * [BluetoothGattCharacteristic].
 */
val BluetoothGattCharacteristic.bleCharacteristicProperties:
	Set<BleCharacteristicProperty> get() =
		BleCharacteristicProperty.values().filter {
			it.mask.and(properties) == it.mask
		}.toSet()