package com.bitwisearts.android.ble.gatt.attribute

import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.flow.SharedFlow

/**
 * Represents a notification (NOTIFY) for a change to a
 * [BluetoothGattCharacteristic] and exposes a [SharedFlow] containing
 * the associated [ByteArray] value.
 *
 * @author Richard Arriaga
 *
 * @property characteristic
 *   The [BluetoothGattCharacteristic] that the change notification was
 *   received for.
 * @property value
 *   The new [ByteArray] value.
 */
class CharacteristicChangeNotification constructor(
	val characteristic: BluetoothGattCharacteristic,
	val value: ByteArray)
