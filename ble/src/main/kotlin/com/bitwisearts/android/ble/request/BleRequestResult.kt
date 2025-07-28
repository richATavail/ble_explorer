package com.bitwisearts.android.ble.request

import com.bitwisearts.android.ble.BleDevice
import com.bitwisearts.android.ble.connection.BleConnection
import com.bitwisearts.android.ble.gatt.GattStatusCode

/**
 * A result of a [BleReadRequest] made to a [BleDevice] over a [BleConnection].
 *
 * @author Richard Arriaga
 */
sealed interface ReadRequestResult
{
	/**
	 * A [ReadRequestResult] that indicates the [BleReadRequest] was successful.
	 *
	 * @param data
	 *   The [ByteArray] data returned from the request.
	 */
	class ReadSuccess(val data: ByteArray): ReadRequestResult

	/**
	 * A [ReadRequestResult] that indicates the [BleReadRequest] failed.
	 *
	 * @param gattStatusCode
	 *   The [GattStatusCode] that indicates the failure reason.
	 * @param error
	 *   An optional [Throwable] that provides additional context for the
	 *   failure.
	 */
	class ReadFailure(
		val gattStatusCode: GattStatusCode,
		val error: Throwable?
	): ReadRequestResult
}

/**
 * A result of a [BleWriteRequest] made to a [BleDevice] over a [BleConnection].
 *
 * @author Richard Arriaga
 */
sealed interface WriteRequestResult
{
	/**
	 * A [WriteRequestResult] that indicates the [BleWriteRequest] was successful.
	 */
	data object WriteSuccess : WriteRequestResult

	/**
	 * A [WriteRequestResult] that indicates the [BleWriteRequest] failed.
	 *
	 * @param gattStatusCode
	 *   The [GattStatusCode] that indicates the failure reason.
	 * @param error
	 *   An optional [Throwable] that provides additional context for the
	 *   failure.
	 */
	data class WriteFailure(
		val gattStatusCode: GattStatusCode,
		val error: Throwable?
	): WriteRequestResult
}