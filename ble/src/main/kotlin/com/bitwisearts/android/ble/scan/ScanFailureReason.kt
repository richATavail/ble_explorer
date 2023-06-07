package com.bitwisearts.android.ble.scan

import android.bluetooth.le.ScanCallback
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Describes why a BLE scan has failed.
 *
 * @author Richard Arriaga
 */
interface ScanFailureReason
{
	/** The numeric code that corresponds to this failure. */
	val failureCode: Int

	/** A description of this [ScanFailureReason]. */
	val description: String

	/** The descriptive name of this [ScanFailureReason]. */
	val name: String

	/**
	 * The fully descriptive string representing this [ScanFailureReason].
	 */
	val displayString: String get() = "($failureCode) $name: $description"

	companion object
	{
		/**
		 * Answer the [ScanFailureReason] associated with the provided
		 * [failureCode].
		 *
		 * @param failureCode
		 *   The [ScanFailureReason.failureCode] to look up.
		 */
		operator fun get (failureCode: Int): ScanFailureReason =
			ScanFailureCode.values().firstOrNull { failureCode == it.failureCode }
				?: UnknownScanFailureCode(failureCode)
	}
}

/**
 * An unrecognized [ScanFailureReason].
 *
 * @author Richard Arriaga.
 */
data class UnknownScanFailureCode constructor(
	override val failureCode: Int
): ScanFailureReason
{
	override val description: String =
		"$failureCode is not a recognized BLE Scan Failure Code"

	override val name: String = "Unknown BLE Scan Failure Code"
}


/**
 * The enumeration of known, system-defined, [ScanFailureReason]s that indicate
 * why a BLE scan failed to start.
 *
 * @author Richard Arriaga
 */
enum class ScanFailureCode(
	override val failureCode: Int,
	override val description: String
): ScanFailureReason
{
	/**
	 * Fails to start scan as BLE scan with the same settings is already
	 * started by the app.
	 *
	 * See [ScanCallback.SCAN_FAILED_ALREADY_STARTED].
	 */
	SCAN_FAILED_ALREADY_STARTED(
		ScanCallback.SCAN_FAILED_ALREADY_STARTED,
		"Fails to start scan as BLE scan with the same settings is " +
			"already started by the app."),

	/**
	 * Fails to start scan as app cannot be registered.
	 *
	 * See [ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED].
	 */
	SCAN_FAILED_APPLICATION_REGISTRATION_FAILED(
		ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,
		"Fails to start scan as app cannot be registered."),

	/**
	 * Fails to start power optimized scan as this feature is not supported.
	 *
	 * See [ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED].
	 */
	SCAN_FAILED_FEATURE_UNSUPPORTED(
		ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED,
		"Fails to start power optimized scan as this feature is not " +
			"supported."),

	/**
	 * Fails to start scan due an internal error.
	 *
	 * See [ScanCallback.SCAN_FAILED_INTERNAL_ERROR].
	 */
	SCAN_FAILED_INTERNAL_ERROR(
		ScanCallback.SCAN_FAILED_INTERNAL_ERROR,
		"Fails to start scan due an internal error."),

	/**
	 * Fails to start scan as it is out of hardware resources.
	 *
	 * See [ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES]
	 */
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES(
		ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES,
		"Fails to start scan as it is out of hardware resources."),

	/**
	 * Fails to start scan as application tries to scan too frequently.
	 *
	 * See [ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY].
	 */
	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	SCAN_FAILED_SCANNING_TOO_FREQUENTLY(
		ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY,
		"Fails to start scan as application tries to scan too frequently.");
}