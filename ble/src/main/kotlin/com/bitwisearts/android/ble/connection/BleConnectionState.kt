package com.bitwisearts.android.ble.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile

/**
 * Represents the connection state of a BLE device. This sealed interface
 * provides a type-safe representation of [BluetoothProfile.getConnectionState]
 * values, along with additional custom states for managing the BLE connection
 * lifecycle.
 *
 * Implementations of this interface represent both standard Android Bluetooth
 * connection states and custom states needed for managing the complete
 * connection workflow (e.g., MTU negotiation, service discovery).
 *
 * @author Richard Arriaga
 */
sealed interface ConnectionState
{
	/**
	 * `true` if the device is connected via BLE; `false` otherwise. Note that
	 * some intermediate states (e.g., MTU_NEGOTIATION, DISCOVERING_SERVICES)
	 * may report as connected even though the connection is not fully
	 * established and ready for use.
	 */
	val isConnected: Boolean

	/**
	 * The [status code][BluetoothProfile.getConnectionState] for the
	 * connection state. Standard Android Bluetooth states use values defined
	 * in [BluetoothProfile], while custom states use negative values to avoid
	 * conflicts.
	 */
	val statusCode: Int

	/**
	 * A human-readable label for this [ConnectionState] suitable for display
	 * in user interfaces.
	 */
	val label: String
}

/**
 * Represents an invalid or unrecognized [ConnectionState]. This is used as a
 * fallback when encountering a status code that doesn't match any known
 * [BleConnectionState] values.
 *
 * @property statusCode
 *   The unrecognized status code value.
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
 * Enumeration of all possible [ConnectionState]s for a BLE connection. This
 * includes both standard Android Bluetooth connection states and custom states
 * for managing the complete BLE connection workflow.
 *
 * The connection lifecycle typically follows this sequence:
 * 1. [DISCONNECTED] - Initial state, no connection
 * 2. [CONNECTING] - Connection attempt initiated
 * 3. [MTU_NEGOTIATION] - Negotiating maximum transmission unit
 * 4. [DISCOVERING_SERVICES] - Discovering available GATT services
 * 5. [NOTIFICATION_SETUP] - Enabling notifications/indications (if needed)
 * 6. [CONNECTED] - Fully connected and ready for use
 * 7. [DISCONNECTING] or [DISCONNECT_REQUESTED] - Disconnection in progress
 * 8. [DISCONNECTED] - Connection closed
 *
 * @property isConnected
 *   `true` if this state represents an active connection; `false` otherwise.
 * @property statusCode
 *   The numeric status code for this state. Standard Bluetooth states use
 *   positive values from [BluetoothProfile], while custom states use negative
 *   values.
 *
 * @author Richard Arriaga
 */
enum class BleConnectionState (
	override val isConnected: Boolean,
	override val statusCode: Int
): ConnectionState
{
	/**
	 * The device is actively connected and fully ready for GATT operations.
	 * All setup phases (MTU negotiation, service discovery, notification
	 * setup) have completed successfully.
	 */
	CONNECTED(true, 0x02)
	{
		override val label: String get() = "Connected"
	},

	/**
	 * A connection attempt is in progress. The device is not yet connected,
	 * and GATT operations cannot be performed.
	 */
	CONNECTING(false, 0x01)
	{
		override val label: String get() = "Connecting"
	},

	/**
	 * There is no active connection to the device. This is the initial state
	 * and the state after a disconnection completes.
	 */
	DISCONNECTED(false, 0x00)
	{
		override val label: String get() = "Disconnected"
	},

	/**
	 * The connection is in the process of being terminated. This is a
	 * transitional state that occurs after calling disconnect but before
	 * the disconnection completes.
	 */
	DISCONNECTING(false, 0x03)
	{
		override val label: String get() = "Disconnecting"
	},

	/**
	 * MTU (Maximum Transmission Unit) negotiation is in progress. This occurs
	 * after the initial connection is established but before the connection
	 * is fully ready. The [BluetoothGatt.requestMtu] method has been called
	 * and the response is pending.
	 */
	MTU_NEGOTIATION(true, Int.MIN_VALUE)
	{
		override val label: String get() = "MTU Negotiation"
	},

	/**
	 * GATT services are being discovered from the remote device. The device
	 * is technically connected but not yet fully usable, as the available
	 * services and characteristics are not yet known. This state occurs after
	 * MTU negotiation and before the connection is fully established.
	 */
	DISCOVERING_SERVICES(true, Int.MIN_VALUE + 1)
	{
		override val label: String get() = "Discovering Services"
	},

	/**
	 * The connection attempt has failed for reasons other than timeout. This
	 * could be due to the device being out of range, authentication failure,
	 * or other connection errors.
	 */
	CONNECTION_FAILED(false, Int.MIN_VALUE + 2)
	{
		override val label: String get() = "Connection Failed"
	},

	/**
	 * The connection attempt has failed due to exceeding the timeout period.
	 * This occurs when the device doesn't respond within the specified
	 * connection timeout duration.
	 */
	CONNECTION_TIMEOUT(false, Int.MIN_VALUE + 3)
	{
		override val label: String get() = "Connection Attempt Timed Out"
	},

	/**
	 * A disconnection has been explicitly requested by the application. This
	 * is a transitional state used to track intentional disconnections as
	 * opposed to unexpected connection losses.
	 */
	DISCONNECT_REQUESTED(false, Int.MIN_VALUE + 4)
	{
		override val label: String get() = "Disconnect Requested"
	},

	/**
	 * Notifications or indications are being enabled on one or more
	 * characteristics. The device is technically connected but not fully ready
	 * for normal operations until all required notifications/indications have
	 * been successfully enabled. This state occurs after service discovery
	 * and before transitioning to the fully [CONNECTED] state.
	 */
	NOTIFICATION_SETUP(false, Int.MIN_VALUE + 5)
	{
		override val label: String get() = "Notification Setup"
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
			for (state in entries)
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