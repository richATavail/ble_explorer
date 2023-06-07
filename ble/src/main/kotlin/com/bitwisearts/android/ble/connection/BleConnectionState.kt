package com.bitwisearts.android.ble.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile

/**
 * The [BluetoothProfile.getConnectionState] as non-primitive value.
 *
 * @author Richard Arriaga
 */
interface ConnectionState
{
	/** `true` if the device is connected via BLE; `false` otherwise. */
	val isConnected: Boolean

	/**
	 *  The [status code][BluetoothProfile.getConnectionState] status code for
	 *  the connection state.
	 */
	val statusCode: Int

	/**
	 * The screen appropriate label for this [ConnectionState].
	 */
	val label: String
}

/**
 * The canonical invalid [ConnectionState].
 *
 * @author Richard Arriaga
 */
class InvalidConnectionState constructor(
	override val statusCode: Int
): ConnectionState
{
	override val isConnected: Boolean = false
	override val label: String get() = "Invalid State"
}

/**
 * All the possible [ConnectionState]s for a BLE connection.
 *
 * @author Richard Arriaga
 */
enum class BleConnectionState (
	override val isConnected: Boolean,
	override val statusCode: Int
): ConnectionState
{
	/** The device is actively connected. */
	CONNECTED(true, 0x02)
	{
		override val label: String get() = "Connected"
	},

	/**  Attempting to establish a connection. */
	CONNECTING(false, 0x01)
	{
		override val label: String get() = "Connecting"
	},

	/** There is no active connection. */
	DISCONNECTED(false, 0x00)
	{
		override val label: String get() = "Disconnected"
	},

	/** Disconnecting from an established connection. */
	DISCONNECTING(false, 0x03)
	{
		override val label: String get() = "Disconnecting"
	},

	/**
	 * [BluetoothGatt.requestMtu] has been called to negotiate the MTU with the
	 * connected device.
	 */
	MTU_NEGOTIATION(true, Int.MIN_VALUE)
	{
		override val label: String get() = "MTU Negotiation"
	},

	/**
	 * The device is technically connected but it is not available as services
	 * are still being discovered.
	 */
	DISCOVERING_SERVICES(true, Int.MIN_VALUE + 1)
	{
		override val label: String get() = "Discovering Services"
	},

	/** The connection attempt has failed. */
	CONNECTION_FAILED(false, Int.MIN_VALUE + 2)
	{
		override val label: String get() = "Connection Failed"
	},

	/** A disconnect has been requested. */
	DISCONNECT_REQUESTED(false, Int.MIN_VALUE + 3)
	{
		override val label: String get() = "Disconnect Requested"
	};

	companion object
	{
		/**
		 * Answer the [ConnectionState] for the given
		 * [ConnectionState.statusCode].
		 *
		 * @param statusCode
		 *   The status code to check.
		 * @return
		 *   The corresponding [ConnectionState] or [InvalidConnectionState] if
		 *   not found.
		 */
		operator fun get(statusCode: Int): ConnectionState
		{
			for (state in values())
			{
				if (statusCode == state.statusCode)
				{
					return state
				}
			}
			return InvalidConnectionState(statusCode)
		}
	}
}