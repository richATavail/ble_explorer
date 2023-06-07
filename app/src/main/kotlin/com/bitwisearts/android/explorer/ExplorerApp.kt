package com.bitwisearts.android.explorer

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import com.bitwisearts.android.ble.scan.BleScanManager

/**
 * The [Application] that represents the Android BLE Explorer app.
 *
 * @author Richard Arriaga
 */
class ExplorerApp: Application()
{
	/**
	 * The [BleScanManager] that manages all BLE scanning for the application.
	 */
	val bleScanManager by lazy {
		BleScanManager(
			getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
	}

	override fun onCreate()
	{
		super.onCreate()
		app = this
	}

	companion object
	{
		/** The singular running [ExplorerApp]. */
		lateinit var app: ExplorerApp
			private set
	}
}