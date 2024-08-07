package com.bitwisearts.android.ble.request


import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.gatt.GattStatusCode
import com.bitwisearts.android.ble.gatt.attribute.AttributeId

/**
 * An abstract [BleRequest] used to chunk BLE write requests to a BLE GATT
 * Attribute.
 *
 * @author Richard Arriaga
 *
 * @param Attribute
 *   The type of the GATT Attribute to write to.
 * @param Id
 *   The type of [AttributeId] used to uniquely identify the target [Attribute]
 *   for this [BleRequest].
 * @property mtu
 *   The [BleConnection.mtu] used to chunk this message.
 * @property payload
 *   The entire [ByteArray] payload to write to the target GATT Attribute. If
 *   the size of this [ByteArray] exceeds the [mtu], the payload will be sent
 *   in chunks.
 * @property gattResponseHandler
 *   The lambda that accepts the [GattStatusCode] responsible for handling the
 *   response to this [BleWriteRequest]. Answer `true` if the next request
 *   should be processed; `false` otherwise.
 */
sealed class BleWriteRequest<Attribute, Id: AttributeId> constructor (
	private val mtu: Int,
	private val payload: ByteArray,
	val gattResponseHandler: (GattStatusCode) -> Boolean
) : BleRequest<Attribute, Id>(), Iterator<ByteArray>
{
	/**
	 * The index into [payload] that represents the first byte of the byte
	 * array that must be sent by the next request if needed.
	 */
	private var startIndexOfNextSend: Int = 0

	/**
	 * The bytes that were last sent to the [Attribute] as the [next] bytes.
	 */
	protected var bytesLastSent: ByteArray = byteArrayOf()

	/**
	 * The number of times this [BleWriteRequest] has been attempted to be
	 * resent.
	 */
	protected var resendAttempts: Int = 0
		private set

	override fun hasNext(): Boolean = payload.size > startIndexOfNextSend

	override val isComplete: Boolean get() = !hasNext()

	/**
	 * Answer the [bytesLastSent] if the [resendAttempts] is less than 3,
	 * otherwise answer `null` to indicate that no more attempts should be made.
	 */
	fun resendBytes(): ByteArray? =
		if(++resendAttempts < 3) bytesLastSent else null

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
}
