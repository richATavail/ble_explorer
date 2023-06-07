package com.bitwisearts.android.ble

import java.util.UUID

/**
 * The [least significant bits][UUID.getLeastSignificantBits] of the standard
 * SIG Bluetooth 16-bit [UUID]: `8000-00805F9B34FB`
 */
const val STANDARD_BLE_UUID_LEAST_SIG: Long = -9_223_371_485_494_954_757

/**
 * The [most significant bits][UUID.getMostSignificantBits] of the standard
 * SIG Bluetooth 16-bit [UUID]: `0000XXXX-0000-1000`. The `XXXX` is where the 16 
 * bits are placed to create the implementation.
 */
const val STANDARD_BLE_UUID_BASE_MOST_SIG: Long = 4096

/**
 * Answer the standard SIG 16-bit [UUID] that takes the form of
 * `0000XXXX-0000-1000-8000-00805F9B34FB`, where `XXXX` is the hex-encoded two 
 * bytes that represents the variable portion of the standard [UUID].
 *
 * @param byteHexString
 *   The 4-character, 2-byte hex string that represents the 16-bit SIG id.
 * @return
 *   A standard 16-bit SIG [UUID].
 */
fun standardUUID(byteHexString: String): UUID =
	UUID.fromString(
		"0000$byteHexString-0000-1000-8000-00805F9B34FB")

/**
 * Answer the standard SIG 16-bit [UUID] that takes the form of
 * `0000XXXX-0000-1000-8000-00805F9B34FB`, where `XXXX` is the hex-encoded two 
 * bytes that represents the variable portion of the standard [UUID].
 *
 * @param sig16BitId
 *   The 2 bytes that represent the 16-bit SIG id.
 * @return
 *   A standard 16-bit SIG [UUID].
 */
fun standardUUID(sig16BitId: Long): UUID
{
	// The two byte value is the right most two bytes of the most significant
	// 4 bytes of the [UUID] most significant bits. The value requires a bit shift
	// of 32 bits to get it into the proper position of the upper two bytes.
	val upper32 = sig16BitId shl 32
	val mostSigBits = upper32 + STANDARD_BLE_UUID_BASE_MOST_SIG
	return UUID(mostSigBits, STANDARD_BLE_UUID_LEAST_SIG)
}

/**
 * Answer the standard SIG 16-bit [UUID] that takes the form of 
 * `0000XXXX-0000-1000-8000-00805F9B34FB`, where `XXXX` is the hex-encoded two 
 * bytes that represents the variable portion of the standard [UUID].
 *
 * @param byte1
 *   The first configurable byte in the prefix of the standard [UUID].
 * @param byte2
 *   The first configurable byte in the prefix of the standard [UUID].
 * @return
 *   A standard 16-bit SIG [UUID].
 */
fun standardUUID(byte1: Byte, byte2: Byte): UUID
{
	// The two byte value is the right most two bytes of the most significant
	// 4 bytes of the [UUID] most significant bits. The value requires a bit shift
	// of 32 bits to get it into the proper position of the upper two bytes.
	val upper32 = (byte1.toLong() shl 8 + byte2) shl 32
	val mostSigBits = upper32 + STANDARD_BLE_UUID_BASE_MOST_SIG
	return UUID(mostSigBits, STANDARD_BLE_UUID_LEAST_SIG)
}