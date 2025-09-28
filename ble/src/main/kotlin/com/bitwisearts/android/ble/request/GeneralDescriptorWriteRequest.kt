package com.bitwisearts.android.ble.request

import android.bluetooth.BluetoothGattDescriptor
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.DescriptorId

/**
 * A general purpose [DescriptorWriteRequest] to write to a
 * [BluetoothGattDescriptor]. This should be used when writing to non-specific
 * descriptors
 *
 * @author Richard Arriaga
 *
 * @constructor
 * Construct a [GeneralDescriptorWriteRequest].
 *
 * @param identifier
 *   The [DescriptorId] of the descriptor to write to.
 * @param mtu
 *   The [BleConnection.mtu] used to chunk this message.
 * @param payload
 *   The entire [ByteArray] payload to write to the target GATT Attribute. If
 *   the size of this [ByteArray] exceeds the [mtu], the payload will be sent
 *   in chunks.
 * @param maxResendAttempts
 *   The maximum number of resend attempts for this write request. Each chunk
 *   sent resets the resend attempt counter to zero.
 * @param gattResponseHandler
 *   The suspend lambda that accepts the [GattStatusCode] responsible for
 *   handling the response to this [GeneralDescriptorWriteRequest].
 */
class GeneralDescriptorWriteRequest constructor (
	identifier: DescriptorId,
	mtu: Int,
	payload: ByteArray,
	maxResendAttempts: Int = MAX_RESEND_ATTEMPTS,
	gattResponseHandler: suspend (GattStatusCode) -> Unit
): DescriptorWriteRequest(
	identifier = identifier,
	mtu = mtu,
	payload = payload,
	maxResendAttempts = maxResendAttempts,
	gattResponseHandler = gattResponseHandler
)