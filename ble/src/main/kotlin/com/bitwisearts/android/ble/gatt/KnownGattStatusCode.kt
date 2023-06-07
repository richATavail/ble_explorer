package com.bitwisearts.android.ble.gatt

/**
 * An enumeration of known/standard [GattStatusCode]s.
 *
 * @author Richard Arriaga
 */
enum class KnownGattStatusCode(override val code: Int) : GattStatusCode
{
	/** A GATT operation completed successfully. */
	SUCCESS(0x00),

	/** GATT read operation is not permitted. */
	READ_NOT_PERMITTED(0x02),

	/** GATT write operation is not permitted. */
	WRITE_NOT_PERMITTED(0x03),

	/** Insufficient authentication for a given operation. */
	INSUFFICIENT_AUTHENTICATION(0x05),

	/** The given request is not supported. */
	REQUEST_NOT_SUPPORTED(0x06),

	/** A read or write operation was requested with an invalid offset. */
	INVALID_OFFSET(0x07),

	/**
	 * [see](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 * [also](https://medium.com/@abrisad_it/ble-error-codes-a3c6675b29c1)
	 * [and](http://allmydroids.blogspot.com/2015/06/android-ble-error-status-codes-explained.html)
	 */
	CONNECTION_TIMEOUT(0x08),

	/** Insufficient encryption for a given operation. */
	INSUFFICIENT_ENCRYPTION(0x0F),

	/** A write operation exceeds the maximum length of the attribute. */
	INVALID_ATTRIBUTE_LENGTH(0x0D),

	/**
	 * [see](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 */
	GATT_UNSUPPORT_GRP_TYPE(0x10),

	/**
	 * [see](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 */
	GATT_INSUF_RESOURCE(0x11),

	/**
	 * [see](https://medium.com/@abrisad_it/ble-error-codes-a3c6675b29c1)
	 * [also](http://allmydroids.blogspot.com/2015/06/android-ble-error-status-codes-explained.html)
	 */
	BLE_HCI_STATUS_CODE_INVALID_BTLE_COMMAND_PARAMETERS(0x12),

	/**
	 * [see](https://medium.com/@abrisad_it/ble-error-codes-a3c6675b29c1)
	 * [also](http://allmydroids.blogspot.com/2015/06/android-ble-error-status-codes-explained.html)
	 */
	BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION(0x13),

	/**
	 * [see](https://medium.com/@abrisad_it/ble-error-codes-a3c6675b29c1)
	 * [also](http://allmydroids.blogspot.com/2015/06/android-ble-error-status-codes-explained.html)
	 */
	BLE_HCI_REMOTE_DEV_TERMINATION_DUE_TO_LOW_RESOURCES(0x14),

	/**
	 * [see](https://medium.com/@abrisad_it/ble-error-codes-a3c6675b29c1)
	 * [also](http://allmydroids.blogspot.com/2015/06/android-ble-error-status-codes-explained.html)
	 */
	BLE_HCI_REMOTE_DEV_TERMINATION_DUE_TO_POWER_OFF(0x15),

	/**
	 * [see](https://medium.com/@abrisad_it/ble-error-codes-a3c6675b29c1)
	 * [also](http://allmydroids.blogspot.com/2015/06/android-ble-error-status-codes-explained.html)
	 */
	BLE_HCI_LOCAL_HOST_TERMINATED_CONNECTION(0x16),

	/**
	 * [see](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 */
	GATT_NO_RESOURCES(0x80),

	/**
	 * [see](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 */
	GATT_INTERNAL_ERROR(0x81),

	/**
	 * No other description discovered. Sources at time of creation:
	 *
	 * * (https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 */
	GATT_WRONG_STATE(0x82),

	/**
	 * [see](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 */
	GATT_DB_FULL(0x83),

	/**
	 * [see](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 */
	GATT_BUSY(0x84),

	/**
	 * An android mystery, the infamous Android "133" error code that is said to
	 * occur due to many issues, some of which being:
	 *
	 * * Low battery power / old battery due to power requirements of BLE
	 *   (ironically)
	 * * Timing issues associated with scanning proximity to attempting
	 *   connection
	 * * Threading issues - some advocate running connections on the main thread
	 * * Poor implementations by Android device manufacturer
	 * * Buggy Android code not accessible by app developers
	 *
	 * Much of this can be seen in this multi-year bug report:
	 * [https://github.com/android/connectivity-samples/issues/18]
	 */
	MYSTERY_133_ERROR(0x85),

	/**
	 * [see](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h)
	 */
	GATT_ILLEGAL_PARAMETER(0x87),

	/** A remote device connection is congested. */
	CONNECTION_CONGESTED(0x8F),

	/**  A GATT operation failed, errors other than the above. */
	FAILURE(0x0101);

	companion object
	{
		/**
		 * Answer the [GattStatusCode] for the provided numeric code.
		 *
		 * @param code
		 *   The [GattStatusCode.code] to look up.
		 * @return
		 *   The corresponding [GattStatusCode] or an
		 *   [UnrecognizedGattStatusCode] if the code is unknown.
		 */
		operator fun get(code: Int): GattStatusCode
		{
			for (gsc in values())
			{
				if (gsc.code == code)
				{
					return gsc
				}
			}
			return UnrecognizedGattStatusCode(code)
		}
	}
}