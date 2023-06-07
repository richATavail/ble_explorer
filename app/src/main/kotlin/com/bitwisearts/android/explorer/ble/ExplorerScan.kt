package com.bitwisearts.android.explorer.ble

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.bitwisearts.android.ble.advertisement.Advertisement
import com.bitwisearts.android.ble.scan.BleScan
import com.bitwisearts.android.ble.scan.BleScanManager
import com.bitwisearts.android.ble.scan.ScanFailureReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A [BleScan] used to used to scan for devices in this application.
 *
 * @author Richard Arriaga
 */
class ExplorerScan constructor(
	scanDuration: Long,
	allowDuplicates: Boolean,
	bleScanManager: BleScanManager,
	scanScope: CoroutineScope =
		CoroutineScope(SupervisorJob() + Dispatchers.IO),
	onScanFailure: (ScanFailureReason) -> Unit = {}
): BleScan(
	scanDuration,
	allowDuplicates,
	bleScanManager,
	onScanFailure,
	scanScope)
{
	override val scanFilters: List<ScanFilter> = listOf()

	/** The list of discovered BLE [Advertisement]s. */
	val found = mutableStateListOf<Advertisement>()

	override suspend fun processScanResult(scanResult: ScanResult)
	{
		val advertisement = Advertisement(scanResult)
		found.add(advertisement)
		Log.d(
			"Scanner",
			"Found ${advertisement.address} - ${advertisement.deviceName}")
	}
}