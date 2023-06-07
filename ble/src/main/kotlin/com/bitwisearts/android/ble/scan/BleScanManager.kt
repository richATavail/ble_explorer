package com.bitwisearts.android.ble.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.util.Log
import com.bitwisearts.android.ble.scan.ScanFailureCode.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Abstract class that manages [BLE scans][BluetoothLeScanner].
 *
 * @author Richard Arriaga
 *
 * @property bluetoothManager
 *   The [BluetoothManager] used by this device to provide BLE scanning
 *   functionality.
 */
class BleScanManager constructor(
	val bluetoothManager: BluetoothManager)
{
	/** The [Mutex] used to synchronize scanning operations. */
	private val mutex = Mutex()

	/** `true` indicates Bluetooth is enabled; `false` otherwise. */
	private val _bleEnabled = MutableStateFlow(
		bluetoothManager.adapter?.isEnabled ?: false)

	/**
	 * Notify this [BluetoothManager] of the updated status of whether or not
	 * Bluetooth is enabled.
	 *
	 * @param isEnabled
	 *   `true` indicates bluetooth is enabled; `false` indicates it is disabled.
	 */
	fun notifyBleEnabled (isEnabled: Boolean)
	{
		_bleEnabled.value = isEnabled
	}

	/** `true` indicates Bluetooth is enabled; `false` otherwise. */
	val isBleIsEnabled: StateFlow<Boolean> get() = _bleEnabled

	/** `true` indicates that a BLE scan is in progress; `false` otherwise. */
	private val _isScanning = MutableStateFlow(false)

	/** `true` indicates that a BLE scan is in progress; `false` otherwise. */
	val isScanning: StateFlow<Boolean> get() = _isScanning

	/**
	 * A [BluetoothLeScanner] used to perform the scanning operations or `null`
	 * if the device Bluetooth is turned off.
	 */
	val bluetoothLeScanner: BluetoothLeScanner? get() =
		bluetoothManager.adapter.bluetoothLeScanner

	/**
	 * The active [BleScan] or `null` if not scanning.
	 */
	private var bleScan: BleScan? = null

	/**
	 * Scan for BLE devices advertising in the area. If a scan is already in
	 * progress, the [BleScan.onScanFailure] will immediately be triggered with
	 * a [ScanFailureCode.SCAN_FAILED_ALREADY_STARTED].
	 *
	 * @param bleScan
	 *   The [BleScan] to execute.
	 */
	fun requestScan (bleScan: BleScan)
	{
		val scanner =
			bluetoothLeScanner ?: run {
				Log.e(
					"BleScanManager",
					"Could not start scan as could not access Bluetooth: " +
						"BluetoothLeScanner unavailable.")
				bleScan.onScanFailure(SCAN_FAILED_INTERNAL_ERROR)
				return
			}
		bleScan.scanScope.launch {
			mutex.withLock {
				val currentRequest = this@BleScanManager.bleScan?.let {
					bleScan.onScanFailure(SCAN_FAILED_ALREADY_STARTED)
					return@launch
				} ?: bleScan
				this@BleScanManager.bleScan = bleScan
				_isScanning.value = true
				currentRequest.scan(scanner)
			}
		}
	}

	/**
	 * Request that the active scan be stopped. If one is running, it will be
	 * stopped.
	 */
	@SuppressLint("MissingPermission")
	suspend fun requestStopScan()
	{
		mutex.withLock {
			if (!_isScanning.value) return
			bleScan?.let {
				bluetoothManager.adapter.bluetoothLeScanner
					?.stopScan(it.scanCallback)
				_isScanning.value = false
			}
			bleScan = null
		}
	}

	/**
	 * Requests that the running scan be canceled prior to its expected
	 * completion. This cancels the timeout job used to [stop][requestStopScan]
	 * the [bleScan] after the expected [BleScan.scanDuration] expires.
	 */
	@SuppressLint("MissingPermission")
	suspend fun requestCancelScan()
	{
		mutex.withLock {
			if (!_isScanning.value) return
			bleScan?.timeoutJob?.cancel()
			Log.d("BleScanManager", "Cancel BLE Scan Requested")
		}
	}
}
