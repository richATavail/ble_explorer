package com.bitwisearts.android.ble.peripheral

import com.bitwisearts.android.ble.connection.BleConnection

/**
 * A request used send byte data for a BLE payload taking into account the MTU
 * size of the BLE connection.
 *
 * @author Richard Arriaga
 *
 * @property mtu
 *   The [BleConnection.mtu] used to chunk this message.
 * @property payload
 *   The entire [ByteArray] payload to write to the target GATT Attribute. If
 *   the size of this [ByteArray] exceeds the [mtu], the payload will be sent
 *   in chunks.
 * @property maxResendAttempts
 *   The maximum number of resend attempts for this write request. Each chunk
 *   sent resets the resend attempt counter to zero.
 */
class SendDataRequest constructor (
	private val mtu: Int,
	private val payload: ByteArray,
	private val maxResendAttempts: Int = MAX_RESEND_ATTEMPTS
): Iterator<ByteArray>
{
	/**
	 * The index into [payload] that represents the first byte of the byte
	 * array that must be sent by the next request if needed.
	 */
	private var startIndexOfNextSend: Int = 0

	/**
	 * The bytes that were last sent as the [next] bytes.
	 */
	private var bytesLastSent: ByteArray = byteArrayOf()

	/**
	 * The number of times this [SendDataRequest] has been attempted to be
	 * resent.
	 */
	private var resendAttempts: Int = 0

	override fun hasNext(): Boolean = payload.size > startIndexOfNextSend

	/**
	 * `true` if all chunks of the [payload] have been sent; `false` if
	 * there are still chunks to send.
	 */
	val isComplete: Boolean get() = !hasNext()

	/**
	 * Answer the [bytesLastSent] if the [resendAttempts] is less than 3,
	 * otherwise answer `null` to indicate that no more attempts should be made.
	 */
	fun resendBytes(): ByteArray? =
		if(++resendAttempts < maxResendAttempts) bytesLastSent else null

	override fun next(): ByteArray
	{
		if (payload.size <= mtu)
		{
			startIndexOfNextSend = payload.size
			return payload
		}
		val nextEndIndexExclusive =
			if (payload.size - startIndexOfNextSend <= mtu)
			{
				payload.size
			}
			else
			{
				startIndexOfNextSend + mtu
			}
		val bytesToSend =
			payload.copyOfRange(startIndexOfNextSend, nextEndIndexExclusive)
		startIndexOfNextSend = nextEndIndexExclusive
		bytesLastSent = bytesToSend
		resendAttempts = 0
		return bytesToSend
	}

	companion object {
		/** The maximum number of resend attempts for a write request. */
		private const val MAX_RESEND_ATTEMPTS: Int = 3
	}
}
