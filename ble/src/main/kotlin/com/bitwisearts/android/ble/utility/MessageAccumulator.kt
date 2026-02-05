package com.bitwisearts.android.ble.utility

import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * A utility to accumulate a message from potentially fragmented payloads.
 *
 * @author Richard Arriaga
 *
 * @property messageSize
 *   The total expected size of the message in bytes.
 */
class MessageAccumulator(
	val messageSize: Int,
	initialPayload: ByteArray
)
{
	/**
	 * The input stream that accumulates the message bytes as they are
	 * received.
	 */
	private val messageInputStream = ByteArrayOutputStream()

	/**
	 * All the bytes that have been received so far for this message.
	 */
	val bytes: ByteArray get() = messageInputStream.toByteArray()


	init
	{
		addChunk(initialPayload)
	}

	/**
	 * Adds additional payload bytes to the message being received.
	 */
	fun addChunk(chunk: ByteArray): Boolean
	{
		val newAccumulatedSize = currentReceivedBytes + chunk.size
		if (newAccumulatedSize > messageSize)
		{
			Log.e(TAG, "Message size exceeded. Expected: $messageSize, " +
				"received: $newAccumulatedSize")
			return false
		}
		messageInputStream.write(chunk)
		return true
	}

	/**
	 * Indicates whether the complete message has been fully received. `true`
	 * indicates that all bytes have been received, `false` indicates that more
	 * bytes are still expected.
	 */
	val hasAllBytes: Boolean get() = messageInputStream.size() == messageSize

	/**
	 * The number of bytes that have been received so far for this message.
	 */
	val currentReceivedBytes: Int get() = messageInputStream.size()

	companion object
	{
		private const val TAG = "MessageAccumulator"
	}
}