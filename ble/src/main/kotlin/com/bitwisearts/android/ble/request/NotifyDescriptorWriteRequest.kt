package com.bitwisearts.android.ble.request

import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.CharacteristicId
import com.bitwisearts.android.ble.gatt.attribute.common.ClientCharacteristicConfiguration

/**
 * A [DescriptorWriteRequest] specific to enabling or disabling notifications
 * for a given BLE characteristic. This request writes to the
 * [ClientCharacteristicConfiguration] descriptor.
 *
 * @author Richard Arriaga
 *
 * @constructor
 * Construct a [NotifyDescriptorWriteRequest].
 *
 * @param identifier
 *   The [CharacteristicId] of the characteristic to write to.
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
 *   handling the response to this [NotifyDescriptorWriteRequest].
 */
class NotifyDescriptorWriteRequest constructor (
	targetCharacteristic: CharacteristicId,
	mtu: Int,
	payload: ByteArray,
	maxResendAttempts: Int = MAX_RESEND_ATTEMPTS,
	gattResponseHandler: suspend (GattStatusCode) -> Unit
): DescriptorWriteRequest(
	identifier = targetCharacteristic.descriptorId(
		ClientCharacteristicConfiguration.uuid),
	mtu = mtu,
	payload = payload,
	maxResendAttempts = maxResendAttempts,
	gattResponseHandler = gattResponseHandler
)