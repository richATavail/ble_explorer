package com.bitwisearts.android.ble.advertisement

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Parcel
import android.os.Parcelable
import java.util.Collections
import java.util.UUID

/**
 * Represents a BLE advertisement received during a scan. This class parses and
 * provides structured access to the data contained in a [ScanResult], including
 * device information, signal strength, and advertisement payload data.
 *
 * BLE advertisements contain structured data organized as AD (Advertisement Data)
 * structures. Each AD structure consists of:
 * 1. A length byte indicating the size of the structure (excluding the length byte itself)
 * 2. An AD Type byte identifying the type of data
 * 3. AD Data bytes containing the actual data
 *
 * This class automatically parses the raw advertisement bytes into individual
 * [AdvertisementData] structures for easier access and interpretation.
 *
 * @property scanResult
 *   The raw [ScanResult] from the Android BLE scan callback containing all
 *   advertisement information.
 *
 * @author Richard Arriaga
 */
class Advertisement constructor(
	@Suppress("MemberVisibilityCanBePrivate") val scanResult: ScanResult
) : Parcelable
{
	/**
	 * The MAC address of the advertising BLE device. This uniquely identifies
	 * the physical device and can be used to establish a connection.
	 */
	val address: String get() = scanResult.device.address

	/**
	 * The advertised device [name][BluetoothDevice.getName] or `«UNKNOWN NAME»`
	 * if the device name is not included in the advertisement or is null. Some
	 * devices may not advertise a name to conserve advertisement payload space.
	 */
	val deviceName: String
		@SuppressLint("MissingPermission")
		get() = scanResult.device.name ?: "«UNKNOWN NAME»"

	/**
	 * The RSSI (Received Signal Strength Indicator) value in dBm. This
	 * indicates the strength of the received signal, with higher (less negative)
	 * values indicating stronger signals. Typical values range from -100 dBm
	 * (very weak) to -30 dBm (very strong).
	 */
	val rssi: Int get() = scanResult.rssi

	/**
	 * The TX (transmission) power level in dBm as advertised by the device.
	 * This indicates the transmit power of the device and can be used in
	 * combination with RSSI to estimate distance. Returns
	 * [Integer.MIN_VALUE][ScanResult.TX_POWER_NOT_PRESENT] if not present in
	 * the advertisement.
	 */
	val txPower: Int get() = scanResult.txPower

	/**
	 * The complete raw advertisement data bytes from the [scanResult] or an
	 * empty [ByteArray] if no scan record is available. This contains all
	 * advertisement data structures concatenated together, including those
	 * not explicitly parsed by this class.
	 */
	val scanRecordBytes: ByteArray
		get() =
			scanResult.scanRecord?.bytes ?: ByteArray(0)

	/**
	 * The set of service [UUID]s advertised by the device, extracted from the
	 * scan record's service UUID list. Returns an empty set if no service UUIDs
	 * are present. Devices typically advertise the UUIDs of primary services
	 * they offer to help central devices identify relevant peripherals.
	 */
	val serviceUUIDs: Set<UUID>
		get() =
			scanResult.scanRecord?.serviceUuids?.map { it.uuid }?.toSet() ?: setOf()

	/**
	 * The index into [scanRecordBytes] of the last byte that contains
	 * advertisement data. Used internally during parsing to track which portion
	 * of the scan record contains valid advertisement structures.
	 */
	private var lastScanDataByteIndex = 0

	/**
	 * A subrange of [scanRecordBytes] containing only the bytes that represent
	 * valid parsed [advertisementData] structures. This excludes any trailing
	 * padding or unused bytes in the scan record.
	 */
	val populatedAdvertisementBytes: ByteArray
		get() =
			scanRecordBytes.sliceArray(0..lastScanDataByteIndex)

	/**
	 * The parsed [List] of [AdvertisementData] structures extracted from the
	 * raw advertisement bytes. Each structure represents a distinct piece of
	 * advertised information (e.g., device name, manufacturer data, service
	 * UUIDs, etc.). The list is ordered as they appear in the advertisement.
	 */
	val advertisementData: List<AdvertisementData>

	init
	{
		val raw = scanRecordBytes
		advertisementData = if (raw.isEmpty())
		{
			Collections.emptyList()
		} else
		{
			val advList = mutableListOf<AdvertisementData>()
			var currentPosition = 0
			// Parse advertisement data structures. Each structure has:
			// - 1 byte: length (number of bytes following, not including length byte)
			// - 1 byte: AD type
			// - N bytes: AD data (where N = length - 1)
			// There must be at least one byte after the size byte for a valid structure
			while (currentPosition < raw.size - 1)
			{
				// The size byte indicates how many bytes are included in the
				// block of advertised data, not including the size byte itself.
				val nextSize = raw[currentPosition++].toInt()
				if (nextSize == 0)
				{
					// Zero-length structures are used as padding; skip them
					continue
				}
				// The Advertise Data (AD) Type is always the byte immediately
				// after the size byte
				val typeByte = raw[currentPosition++].toInt().and(0xFF)
				val lastByteIndex = currentPosition + nextSize - 2
				advList.add(
					AdvertisingDataType[typeByte].advertisementData(
						raw.sliceArray(currentPosition..lastByteIndex)
					)
				)
				currentPosition = lastByteIndex + 1
				lastScanDataByteIndex = lastByteIndex
			}
			advList
		}
	}

	override fun describeContents(): Int = 0

	override fun writeToParcel(dest: Parcel, flags: Int)
	{
		dest.writeParcelable(scanResult, flags)
	}

	companion object CREATOR : Parcelable.Creator<Advertisement>
	{
		override fun createFromParcel(`in`: Parcel): Advertisement =
			Advertisement(ScanResult.CREATOR.createFromParcel(`in`))

		override fun newArray(size: Int): Array<Advertisement?> =
			arrayOfNulls(size)
	}
}
