package com.bitwisearts.android.ble.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import com.bitwisearts.android.ble.scan.ScanFailureCode.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Abstract class that manages [BLE scans][BluetoothLeScanner].
 *
 * @author Richard Arriaga
 *
 * @property scanDuration
 *   The number of milliseconds to scan for. When this time has expired, the
 *   scan will be [stopped][stopScan].
 * @property allowDuplicates
 *   `true` if a device is permitted to be found multiple times and have its
 *   advertisement processed multiple times; `false` if a device's
 *   advertisement is only permitted to be discovered and processed once
 *   during the scan.
 * @property bleScanManager
 *   The [BleScanManager] that will be used to perform this scan.
 * @property onScanFailure
 *   Accepts a [ScanFailureReason] in the event a [scan] fails to start.
 * @property scanScope
 *   The [CoroutineScope] used to perform BLE scans.
 */
abstract class BleScan constructor(
	private val scanDuration: Long,
	private val allowDuplicates: Boolean,
	private val bleScanManager: BleScanManager,
	internal val onScanFailure: (ScanFailureReason) -> Unit,
	internal val scanScope: CoroutineScope =
		CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
	/**
	 * The list of [ScanFilter]s to apply to the BLE [scan].
	 */
	abstract val scanFilters: List<ScanFilter>

	/**
	 * Process the [ScanResult] discovered during a BLE [scan].
	 *
	 * @param scanResult
	 *   The [ScanResult] to be processed.
	 */
	abstract suspend fun processScanResult(scanResult: ScanResult)

	/**
	 * The set of device MAC addresses discovered during the most recent scan.
	 */
	private val discovered = mutableSetOf<String>()

	/** The [Channel] used to collect and provide new [ScanResult]s. */
	private val scanResultChannel = Channel<ScanResult>(Channel.BUFFERED)

	/**
	 * The [Job] that is running down a timer that when expires will stop the
	 * running BLE scan.
	 */
	internal var timeoutJob: Job? = null
		private set

	/**
	 * The [ScanSettings] used to define the parameters of the
	 * [scan][startScan].
	 */
	protected val scanSettings: ScanSettings get() =
		ScanSettings.Builder().build()

	/**
	 * [ScanCallback] used to send a newly discovered [ScanResult] to the
	 * [scanResultChannel].
	 */
	val scanCallback: ScanCallback = object : ScanCallback()
	{
		override fun onScanResult(callbackType: Int, result: ScanResult)
		{
			scanScope.launch {
				scanResultChannel.send(result)
			}
		}
		override fun onBatchScanResults(results: MutableList<ScanResult>?)
		{
			super.onBatchScanResults(results) // TODO explore further
		}

		override fun onScanFailed(errorCode: Int)
		{
			onScanFailure(ScanFailureReason[errorCode])
		}
	}

	/**
	 * Start a scan for nearby BLE devices constrained by the [scanFilters].
	 *
	 * @param scanner
	 *   The [BluetoothLeScanner] used to perform the BLE scan.
	 */
	@SuppressLint("MissingPermission")
	private fun startScan (scanner: BluetoothLeScanner)
	{
		scanner.startScan(scanFilters, scanSettings, scanCallback)
	}

	/**
	 * Stop the BLE scan.
	 *
	 * @param bluetoothManager
	 *   The [BluetoothManager] used by this device to provide BLE scanning
	 *   functionality.
	 */
	@SuppressLint("MissingPermission")
	internal suspend fun stopScan(
		bluetoothManager: BluetoothManager, then: () -> Unit)
	{
		bluetoothManager.adapter.bluetoothLeScanner
			?.stopScan(scanCallback)
		then()
		Log.d("ScannerStopping", "Stopping BLE Scan")
	}

	/**
	 * Scan for BLE devices advertising in the area using the provided
	 * [BluetoothLeScanner] and return the [Job] handling the
	 * timeout to stop the scan.
	 */
	internal fun scan (scanner: BluetoothLeScanner)
	{
		timeoutJob = scanScope.launch {
			launch {
				if (allowDuplicates)
				{
					handleScanResultsAllowDuplicates()
				}
				else
				{
					handleScanResultsNoDuplicates()
				}
			}
			startScan(scanner)
			try
			{
				delay(scanDuration)
				Log.d("BleScan", "Scan duration expired")
			}
			finally
			{
				Log.d("BleScan", "Stopping Scan")
				bleScanManager.requestStopScan()
			}
		}
	}

	/**
	 * Process the discovered [ScanResult]s once, disallowing subsequent
	 * processing of already processed scan results.
	 */
	private suspend fun handleScanResultsNoDuplicates ()
	{
		discovered.clear()
		for (result in scanResultChannel)
		{
			if (!discovered.contains(result.device.address))
			{
				discovered.add(result.device.address)
				processScanResult(result)
			}
		}
	}

	/**
	 * Process the discovered [ScanResult]s, allowing subsequent processing of
	 * already processed scan results.
	 */
	private suspend fun handleScanResultsAllowDuplicates ()
	{
		for (result in scanResultChannel)
		{
			processScanResult(result)
		}
	}
}
