package com.bitwisearts.android.ble.utility

/**
 * Converts this [Byte] to a binary string representation with 8 digits,
 * padding with leading zeros if necessary.
 *
 * Example: The byte value 165 (0xA5) returns `"10100101"`
 *
 * @return
 *   An 8-character string representing the binary value of this byte.
 */
val Byte.asBinary: String get() =
	toUByte().toString(2).padStart(8, '0')

/**
 * Converts this [Byte] to a two-character uppercase hexadecimal string,
 * padding with a leading zero if necessary.
 *
 * Example: The byte value 42 returns `"2A"`
 *
 * @return
 *   A 2-character uppercase hex string representation of this byte.
 */
val Byte.asHex: String get() =
	java.lang.String.format("%02X", this)

/**
 * Converts this [Byte] to a hexadecimal string prefixed with "0x",
 * padding with a leading zero if necessary.
 *
 * Example: The byte value 42 returns `"0x2A"`
 *
 * @return
 *   A hex string with "0x" prefix representing this byte.
 */
val Byte.asLiteralHex: String get() =
	java.lang.String.format("0x%02X", this)

/**
 * Converts this [ByteArray] to a space-separated hexadecimal string where
 * each byte is represented as a two-character uppercase hex value.
 *
 * Example: `byteArrayOf(0x01, 0x6A, 0xFF.toByte())` returns `"01 6A FF"`
 *
 * This format is useful for displaying byte arrays in a human-readable form.
 *
 * @return
 *   A space-separated hex string representation of this byte array.
 */
val ByteArray.asHex: String get() =
	this.map { it.asHex }.joinToString(" ") { it }

/**
 * Converts this [ByteArray] to a compact hexadecimal string with no spaces
 * between bytes, where each byte is represented as a two-character uppercase
 * hex value.
 *
 * Example: `byteArrayOf(0x01, 0x6A, 0xFF.toByte())` returns `"016AFF"`
 *
 * This format is useful for compact hex representations and parsing.
 *
 * @return
 *   A compact (no spaces) hex string representation of this byte array.
 */
val ByteArray.asCompactHex: String get() =
	this.map { it.asHex }.joinToString("") { it }

/**
 * Converts this [ByteArray] to a binary string representation where each byte
 * is shown as an 8-digit binary value, with spaces between bytes. Lines are
 * automatically wrapped to a maximum of 78 characters (7 bytes per line plus
 * spaces).
 *
 * Example: `byteArrayOf(0xFF.toByte(), 0x00)` returns `"11111111 00000000"`
 *
 * This format is useful for bit-level analysis and debugging.
 *
 * @return
 *   A space-separated binary string with automatic line wrapping.
 */
@Suppress("unused")
val ByteArray.asBinary: String get() =
	buildString {
		if (isEmpty()) return@buildString
		append(this@asBinary[0].asBinary)
		for (i in 1 until size - 1)
		{
			this@asBinary[i].asBinary
			append(if (i % 7 == 0) '\n' else ' ')
		}
		append(this@asBinary.last().asBinary)
	}

/**
 * Converts this [ByteArray] to a space-separated hexadecimal string where
 * each byte is prefixed with "0x" and represented as a two-character
 * uppercase hex value.
 *
 * Example: `byteArrayOf(0x01, 0x2A)` returns `"0x01 0x2A"`
 *
 * @return
 *   A space-separated hex string with "0x" prefixes for each byte.
 */
@Suppress("unused")
val ByteArray.asLiteralHex: String get() =
	this.map { it.asLiteralHex }.joinToString(" ") { it }

/**
 * Interprets this [ByteArray] as a little-endian encoded [Int] value. The
 * first byte is treated as the least significant byte, and subsequent bytes
 * as progressively more significant.
 *
 * Example: `byteArrayOf(0x01, 0x02, 0x00, 0x00)` returns `513` (0x00000201)
 *
 * **Note:** This assumes the byte array contains at most 4 bytes. If the
 * array is longer, only the first 4 bytes are used. If shorter, the remaining
 * higher-order bytes are treated as zero.
 *
 * @return
 *   The integer value represented by this byte array in little-endian format.
 */
val ByteArray.asInt: Int get() =
	this.foldIndexed(0) { index, acc, byte ->
		acc or (byte.toInt() and 0xFF shl (index * 8))
	}

/**
 * Interprets this [ByteArray] as a big-endian encoded [Int] value. The
 * first byte is treated as the most significant byte, and subsequent bytes
 * as progressively less significant.
 *
 * Example: `byteArrayOf(0x00, 0x00, 0x02, 0x01)` returns `513` (0x00000201)
 *
 * **Note:** This assumes the byte array contains at most 4 bytes. If the
 * array is longer, only the first 4 bytes are used. If shorter, the remaining
 * higher-order bytes are treated as zero.
 *
 * @return
 *   The integer value represented by this byte array in big-endian format.
 */
val ByteArray.asBigEndianInt: Int get() =
	this.reversedArray().foldIndexed(0) { index, acc, byte ->
		acc or (byte.toInt() and 0xFF shl (index * 8))
	}