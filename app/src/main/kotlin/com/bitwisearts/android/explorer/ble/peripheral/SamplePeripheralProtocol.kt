package com.bitwisearts.android.explorer.ble.peripheral

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * A string-based message that is sent or received via the sample peripheral
 * protocol.
 *
 * @property id
 *   The unique identifier for this message.
 * @property message
 *   The full text of the message.
 *
 * @constructor
 * Creates a new [Message] with the provided [message] and an auto-generated
 * [id].
 * @param id
 *    The unique identifier for this message.
 * @param message
 *    The full text of the message.
 */
class Message(
	val id: Int,
	val message: String
) {
	/**
	 * Creates a new [Message] with the provided [message] and an auto-generated
	 * [id].
	 *
	 * @param message
	 *   The full text of the message.
	 */
	constructor(message: String) : this(getNextId(), message)

	/**
	 * A builder for constructing messages to be sent via the sample peripheral
	 * protocol. It converts data types to byte arrays and handles the
	 * construction of the message format.
	 */
	inner class MessageBuilder
	{
		/**
		 * The output stream where the message bytes are written.
		 */
		private val output = ByteArrayOutputStream()

		/**
		 * Writes the provided [ByteArray] to the message [output].
		 */
		private fun writeBytes(value: ByteArray): MessageBuilder
		{
			output.write(value)
			return this
		}

		/**
		 * Builds this [Message] as a [ByteArray].
		 *
		 * The message format is as follows:
		 * 1. A size prefix indicating the total length of the message (including
		 *    the size prefix itself).
		 * 2. The message ID, which uniquely identifies this message.
		 * 3. The actual message content as a UTF-8 encoded byte array.
		 *
		 * @return
		 *   The constructed message as a [ByteArray].
		 */
		internal fun build(): ByteArray
		{
			val rawMessageBytes = message.toByteArray()
			val messageIdBytes = serializeUnsignedInt(id)
			val messageLength = rawMessageBytes.size + messageIdBytes.size
			val sizePrefixBytes = serializeUnsignedInt(messageLength)
			writeBytes(sizePrefixBytes)
			writeBytes(serializeUnsignedInt(messageLength))
			writeBytes(serializeUnsignedInt(id))
			writeBytes(message.toByteArray())
			return output.toByteArray()
		}
	}

	/**
	 * Serializes this message into a [ByteArray] suitable for transmission.
	 */
	fun serialize(): ByteArray = MessageBuilder().build()

	companion object {
		private var nextId = 0

		private fun getNextId(): Int
		{
			return nextId++
		}
	}
}

/**
 * A deserializer for messages received via the sample peripheral protocol.
 *
 * This class handles the incremental construction of a message from
 * potentially fragmented payloads. It reads the size prefix to determine
 * the total expected size of the message and accumulates additional payload
 * bytes until the complete message is reconstructed.
 *
 * @param initialPayload
 *   The initial payload containing at least the size prefix of the message.
 */
class MessageDeserializer(initialPayload: ByteArray)
{
	/**
	 * The total expected size of the message, excluding the size prefix.
	 */
	val messageSize: Int

	/**
	 * The input stream that accumulates the message bytes as they are
	 * received.
	 */
	val messageInputStream = ByteArrayOutputStream()

	init
	{
		val (sizePrefix, remaining) = readSizePrefix(initialPayload)
		messageSize = sizePrefix
		messageInputStream.write(remaining)
	}

	/**
	 * Adds additional payload bytes to the message being received.
	 */
	fun additionalPayload(additionalPayload: ByteArray)
	{
		messageInputStream.write(additionalPayload)
	}

	/**
	 * Indicates whether the complete message has been fully received. `true`
	 * indicates that all bytes have been received, `false` indicates that more
	 * bytes are still expected.
	 */
	val hasAllBytes: Boolean get() = messageInputStream.size() == messageSize

	/**
	 * Deserializes and answer the complete [Message] from the accumulated
	 * payload bytes.
	 *
	 * @throws SerializationException
	 *   If the complete message has not yet been received.
	 */
	fun deserialize(): Message
	{
		if (!hasAllBytes)
		{
			throw SerializationException(
				"Cannot deserialize message; only " +
					"${messageInputStream.size()} of $messageSize " +
					"bytes have been received")
		}
		val input = ByteArrayInputStream(messageInputStream.toByteArray())
		val messageId = readUnsignedInt(input)
		val messageBytes = input.readBytes()
		return Message(messageId, String(messageBytes))
	}

	companion object
	{
		/**
		 * Reads the size prefix from the initial payload of a message. The size
		 * prefix indicates the total length of the message, including the size
		 * prefix itself. This method uses the [readUnsignedInt] function to
		 * decode the size prefix from the provided [initialPayload].
		 *
		 * @param initialPayload
		 *   The initial payload containing the size prefix.
		 * @return
		 *   A [Pair] where the first element is the size prefix as an [Int] and
		 *   the second element is the remaining [ByteArray] after the size
		 *   prefix has been read.
		 */
		private fun readSizePrefix(
			initialPayload: ByteArray
		): Pair<Int, ByteArray>
		{
			val input = ByteArrayInputStream(initialPayload)
			return (
				readUnsignedInt(input) to
					initialPayload.slice(
						input.available() until
						initialPayload.size).toByteArray())
		}
	}
}

/**
 * An exception that is thrown when there is an error during serialization or
 * deserialization of messages in the sample peripheral protocol.
 *
 * @param message
 *   The message that describes the error.
 */
class SerializationException(message: String) : RuntimeException(message)

/**
 * Serializes an integer using a variable-length encoding and answers
 * the serialized bytes. This expects the integer to be non-negative.
 *
 * This method uses a variable-length quantity (VLQ) encoding to
 * serialize the integer. Each byte contains 7 bits of the integer, with
 * the most significant bit (MSB) indicating whether there are more
 * bytes to read. If the MSB is set to 1, it indicates that more bytes
 * follow; if it's set to 0, it indicates that this is the last byte.
 * The consequence of this encoding is that very large values will take
 * 5 up bytes, however most values will require less than 4 bytes.
 *
 * See https://en.wikipedia.org/wiki/Variable-length_quantity
 *
 * @throws IllegalArgumentException
 *   If the provided [value] is negative.
 */
fun serializeUnsignedInt(value: Int): ByteArray
{
	require(value >= 0) { "Value must be non-negative" }
	val byteStream = ByteArrayOutputStream()
	var unwritten = value
	do
	{
		var toWrite = unwritten and 0x7F
		unwritten = unwritten ushr 7
		if (unwritten != 0)
		{
			toWrite = toWrite or 0x80
		}
		byteStream.write(toWrite)
	} while (unwritten != 0)
	return byteStream.toByteArray()
}

/**
 * Deserializes an unsigned integer from the provided [ByteArray]
 * adhering to a Variable Length Quantity (VLQ) encoding. The [ByteArray]
 * should contain the bytes that represent the unsigned integer in a
 * VLQ encoding format. The method reads bytes until it encounters a
 * byte with the most significant bit (MSB) set to 0, indicating the end
 * of the integer. It is expected that the integer is non-negative.
 *
 * @param input
 *   The [ByteArrayInputStream] containing the VLQ-encoded unsigned
 *   integer.
 * @return
 *   A The deserialized unsigned integer.
 * @throws SerializationException
 *   If the input does not contain enough bytes to form a valid unsigned
 *   integer or if the encoding is malformed (e.g., more than 5 bytes
 *   are read without encountering a terminating byte).
 */
fun readUnsignedInt(input: ByteArrayInputStream): Int
{
	var value = 0
	var shift = 0
	do
	{
		val byteRead: Int = input.read()
		if (byteRead == -1)
		{
			throw SerializationException(
				"Tried to read an unsigned int at byte " +
					"index ${input.available() - shift} however there " +
					"were no bytes left to be read in the buffer")
		}
		if (shift >= 32)
		{
			throw SerializationException(
				"Tried to read an unsigned int at byte " +
					"index ${input.available() - shift} however there " +
					"were 5 bytes read without a terminating byte; a " +
					"byte with the first bit being 1.")
		}
		value = value or (byteRead and 0x7F shl shift)
		shift += 7
	}
	while (byteRead and 0x80 == 0x80)
	return value
}