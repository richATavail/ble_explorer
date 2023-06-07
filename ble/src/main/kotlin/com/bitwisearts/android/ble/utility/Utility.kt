package com.bitwisearts.android.ble.utility

/** This [Byte] as a binary string. For example: `10100101` */
val Byte.asBinary: String get() =
	toUByte().toString(2).padStart(8, '0')

/** This [Byte] as a hex encoded string. For example `2A`. */
val Byte.asHex: String get() =
	java.lang.String.format("%02X", this)

/** This [Byte] as a literal hex encoded string. For example `0x2A` */
val Byte.asLiteralHex: String get() =
	java.lang.String.format("0x%02X", this)

/**
 * This [ByteArray] as a [hex-encoded][asHex] string with spaces between each
 * [hex][asHex] byte.
 */
val ByteArray.asHex: String get() =
	this.map { it.asHex }.joinToString(" ") { it }

/**
 * This [ByteArray] as a [hex-encoded][asHex] string with no spaces between each
 * [hex][asHex] byte.
 */
val ByteArray.asCompactHex: String get() =
	this.map { it.asHex }.joinToString("") { it }

/**
 * This [ByteArray] as a [binary string][asBinary] of bytes with a space in
 * between each byte. Each line will be at most be 78 characters long.
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
 * @return
 *   This [ByteArray] as a literal [hex-encoded][asLiteralHex] string.
 */
@Suppress("unused")
val ByteArray.asLiteralHex: String get() =
	this.map { it.asLiteralHex }.joinToString(" ") { it }