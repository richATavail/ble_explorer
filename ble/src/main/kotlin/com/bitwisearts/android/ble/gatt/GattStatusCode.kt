package com.bitwisearts.android.ble.gatt

/**
 * Interface for handling different GATT status codes.
 *
 * @author Richard Arriaga
 */
sealed interface GattStatusCode
{
	/** The numeric status code. */
	val code: Int

	/** The code's name. */
	val name: String

	/**
	 * Display the name and the code as a single string.
	 */
	val display: String get() = "(0x${code.toString(16)}) $name"
}