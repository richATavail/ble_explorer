package com.bitwisearts.android.ble.gatt

/**
 * An unofficial [GattStatusCode] that indicates there is no BLE connection.
 *
 * @author Richard Arriaga
 */
object GattNoConnection: GattStatusCode
{
	override val code: Int get() = Int.MAX_VALUE
	override val name: String get() = "Custom GATT No Connection"
}