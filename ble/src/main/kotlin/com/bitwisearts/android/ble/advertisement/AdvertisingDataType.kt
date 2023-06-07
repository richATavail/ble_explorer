package com.bitwisearts.android.ble.advertisement

/**
 * Enumerates the bluetooth advertising data types (AD type) per the Bluetooth
 * specification's [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 * revision date 2023-04-20, section 2.3 Common Data Types.
 *
 * The advertising data types are referenced in the following documents:
 *
 * * Supplement to the Bluetooth Core Specification v11 Part A, Section 1
 * * Bluetooth Core Specification [Vol 3] Part C, Section 8
 * * Bluetooth Core Specification [Vol 3] Part C, Section 11
 * * Bluetooth Core Specification [Vol 6] Part B, Section 2.3.4.8.
 *
 * See the specifications folder for referenced specifications. Note, not all
 * specifications have been added; some may need to be looked up on the
 * internet.
 *
 * @author Richard Arriaga
 *
 * @property adByte
 *   The "Common Data Type" byte, represented as an integer, that represents the
 *   [AdvertisingDataType] in an advertisement.
 * @property adName
 *   The name of the data type.
 */
enum class AdvertisingDataType (val adByte: Int, val adName: String)
{
	/**
	 * The canonical representation of an Invalid [AdvertisingDataType]. This
	 * does not appear in the specification.
	 */
	INVALID(Int.MIN_VALUE, "Invalid AD Type"),

	/** Core Specification Supplement, Part A, Section 1.3 */
	FLAG(0x01, "Flags")
	{
		override fun advertisementData(data: ByteArray): AdvertisementData
		{
			return FlagsData(data)
		}
	},

	/** Core Specification Supplement, Part A, Section 1.1 */
	INCOMPLETE_16_SERVICE_UUID(
		0x02, "Incomplete List of 16-bit Service Class UUIDs"),

	/** Core Specification Supplement, Part A, Section 1.1 */
	COMPLETE_16_SERVICE_UUID(
		0x03, "Complete List of 16-bit Service Class UUIDs"),

	/** Core Specification Supplement, Part A, Section 1.1 */
	INCOMPLETE_32_SERVICE_UUID(
		0x04, "Incomplete List of 32-bit Service Class UUIDs"),

	/** Core Specification Supplement, Part A, Section 1.1 */
	COMPLETE_32_SERVICE_UUID(
		0x05, "Complete List of 32-bit Service Class UUIDs"),

	/** Core Specification Supplement, Part A, Section 1.1 */
	INCOMPLETE_128_SERVICE_UUI(
		0x06, "Incomplete List of 128-bit Service Class UUIDs"),

	/** Core Specification Supplement, Part A, Section 1.1 */
	COMPLETE_128_SERVICE_UUID(
		0x07, "Complete List of 128-bit Service Class UUIDs"),

	/** Core Specification Supplement, Part A, Section 1.2 */
	SHORT_LOCAL_NAME(0x08, "Shortened Local Name"),

	/** Core Specification Supplement, Part A, Section 1.2 */
	COMPLETE_LOCAL_NAME(0x09, "Complete Local Name"),

	/** Core Specification Supplement, Part A, Section 1.5 */
	TX_POWER_LEVEL(0x0A, "Tx Power Level"),

	/** Core Specification Supplement, Part A, Section 1.6 */
	CLASS_OF_DEVICE(0x0D, "Class of Device"),

	/** Core Specification Supplement, Part A, Section 1.6 */
	SIMPLE_PAIRING_HASH_C192(0x0E, "Simple Pairing Hash C-192"),

	/** Core Specification Supplement, Part A, Section 1.6 */
	SIMPLE_PAIRING_HASH_R192(0x0F, "Simple Pairing Randomizer R-192"),

	/** Device ID Profile (DeviceID_SPEC_V13) */
	DEVICE_ID(0x10, "Device ID"),

	/** Core Specification Supplement, Part A, Section 1.8 */
	SECURITY_MANGER_TK_VALUE(
		0x10, "Security Manager TK Value"),

	/** Core Specification Supplement, Part A, Section 1.7 */
	SECURITY_MANGER_OUT_BANDS_FLAG(0x11, "Security Manager Out of Band Flags"),

	/** Core Specification Supplement, Part A, Section 1.9 */
	PERIPHERAL_CONNECTION_INTERVAL_RANGE(
		0x12, "Peripheral Connection Interval Range"),

	/** Core Specification Supplement, Part A, Section 1.10 */
	SERVICE_SOLICITATION_16_UUID(
		0x14, "List of 16-bit Service Solicitation UUIDs"),

	/** Core Specification Supplement, Part A, Section 1.11 */
	SERVICE_SOLICITATION_128_UUID(
		0x15, "List of 128-bit Service Solicitation UUIDs"),
	

	/** Core Specification Supplement, Part A, section 1.11 */
	SERVICE_DATA_16_UUID(0x16, "Service Data - 16-bit UUID"),

	/** Core Specification Supplement, Part A, section 1.13 */
	PUBLIC_TARGET_ADDRESS(0x17, "Public Target Address"),

	/** Core Specification Supplement, Part A, section 1.14 */
	RANDOM_TARGET_ADDRESS(0x18, "Random Target Address"),

	/** Core Specification Supplement, Part A, section 1.12 */
	APPEARANCE(0x19, "Appearance"),

	/** Core Specification Supplement, Part A, section 1.15 */
	ADVERTISING_INTERVAL(0x1A, "Advertising Interval"),

	/** Core Specification Supplement, Part A, section 1.16 */
	LE_BT_DEVICE_ADDRESS(0x1B, "LE Bluetooth Device Address"),

	/** Core Specification Supplement, Part A, section 1.17 */
	LE_ROLE(0x1C, "LE Role"),

	/** Core Specification Supplement, Part A, section 1.6 */
	SIMPLE_PAIRING_HASH_C256(0x1D, "Simple Pairing Hash C-256"),

	/** Core Specification Supplement, Part A, section 1.6 */
	SIMPLE_PAIRING_HASH_R256(0x1E, "Simple Pairing Randomizer R-256"),

	/** Core Specification Supplement, Part A, section 1.10 */
	SERVICE_SOLICITATION_32_UUID(
		0x1F, "List of 32-bit Service Solicitation UUIDs"),

	/** Core Specification Supplement, Part A, section 1.11 */
	SERVICE_DATA_32_UUID(0x20, "Service Data - 32-bit UUID"),

	/** Core Specification Supplement, Part A, section 1.11 */
	SERVICE_DATA_128_UUID(0x21, "Service Data - 128-bit UUID"),

	/** Core Specification Supplement, Part A, section 1.6 */
	LE_SECURE_CONNECTIONS_CONFIRM_VALUE(
		0x22, "LE Secure Connections Confirmation Value"),

	/** Core Specification Supplement, Part A, section 1.6 */
	LE_SECURE_CONNECTIONS_RANDOM_VALUE(
		0x23, "LE Secure Connections Random Value"),

	/** Core Specification Supplement, Part A, section 1.18 */
	URI(0x24, "URI"),

	/** Indoor Positioning Service spec */
	INDOOR_POSITIONING(0x25, "Indoor Positioning"),

	/** Transport Discovery Service spec */
	TRANSPORT_DISC_DATA(0x26, "Transport Discovery Data"),

	/** Core Specification Supplement, Part A, Section 1.19 */
	LE_SUPPORTED_FEATURES(0x27, "LE Supported Features"),

	/** Core Specification Supplement, Part A, Section 1.20 */
	CHANNEL_MAP_UPDATE_INDICATION(
		0x28, "Channel Map Update Indication"),

	/** Mesh Profile Specification Section 5.2.1 */
	PB_ADV(0x29, "PB-ADV"),

	/** Mesh Profile Specification Section 3.3.1 */
	MESH_MESSAGE(0x2A, "Mesh Message"),

	/** Mesh Profile Specification Section 3.9 */
	MESH_BEACON(0x2B, "Mesh Beacon"),

	/** Core Specification Supplement, Part A, Section 1.21 */
	BIG_INFO(0x2C, "BIGInfo"),

	/** Core Specification Supplement, Part A, Section 1.22 */
	BROADCAST_CODE(0x2D, "Broadcast_Code"),

	/** Coordinated Set Identification Profile v1.0 or later */
	RESOLVABLE_SET_IDENTIFIER(0x2E, "Resolvable Set Identifier"),

	/** Coordinated Set Identification Profile v1.0 or later */
	ADVERTISING_INTERVAL_LONG(0x2F, "Advertising Interval - long"),

	/** Public Broadcast Profile v1.0 or later */
	BROADCAST_NAME(0x30, "Broadcast Name"),

	/** Core Specification Supplement, Part A, Section 1.23 */
	ENCRYPTED_ADVERTISING_DATA(0x31, "Encrypted Advertising Data"),

	/** Core Specification Supplement, Part A, Section 1.24 */
	PERIODIC_ADV_RESPONSE_TIMING_INFO(
		0x32, "Periodic Advertising Response Timing Information"),

	/** ESL Profile */
	ELECTRONIC_SHELF_LABEL(0x34, "Electronic Shelf Label"),

	/** 3D Synchronization Profile */
	THREE_D_INFO_DATA(0x3D, "3D Information Data"),

	/** Core Specification Supplement, Part A, Section 1.4 */
	MAN_SPEC_DATA(0xFF, "Manufacturer Specific Data")
	{
		override fun advertisementData(data: ByteArray): AdvertisementData
		{
			return ManufacturerData(data)
		}
	};

	/**
	 * Create an [AdvertisementData] from the provided bytes for this
	 * [AdvertisingDataType].
	 *
	 * @param data
	 *   The data being advertised.
	 * @return An [AdvertisementData].
	 */
	open fun advertisementData(data: ByteArray): AdvertisementData =
		AdvertisementData(this, data)

	companion object
	{
		/**
		 * The [Map] from [adByte] to [AdvertisingDataType].
		 */
		private val adByteMap: Map<Int, AdvertisingDataType> by lazy {
			val m = mutableMapOf<Int, AdvertisingDataType>()
			values().forEach {
				m[it.adByte] = it
			}
			m
		}

		/**
		 * @return
		 *   The [AdvertisingDataType] for the given [type] or [INVALID] if
		 *   an unknown [adByte] is provided.
		 */
		operator fun get (type: Int): AdvertisingDataType =
			adByteMap[type] ?: INVALID
	}
}