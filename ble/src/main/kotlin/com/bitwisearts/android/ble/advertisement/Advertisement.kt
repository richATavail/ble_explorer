package com.bitwisearts.android.ble.advertisement

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.Parcel
import android.os.Parcelable
import java.util.Collections
import java.util.UUID

/**
 * Provides the data from a BLE [ScanResult].
 *
 * @author Richard Arriaga
 */
class Advertisement constructor(
	@Suppress("MemberVisibilityCanBePrivate") val scanResult: ScanResult
) : Parcelable
{
	/** The mac address of the BLE device. */
	val address: String get() = scanResult.device.address

	/**
	 * The device [name][BluetoothDevice.getName] or `«UNKNOWN NAME»` if `null`.
	 */
	val deviceName: String
		@SuppressLint("MissingPermission")
		get() = scanResult.device.name ?: "«UNKNOWN NAME»"

	/** The [ScanResult.getRssi] of this [Advertisement]. */
	val rssi: Int get() = scanResult.rssi

	/** The [ScanResult.getTxPower] of this [Advertisement]. */
	val txPower: Int get() = scanResult.txPower

	/**
	 * The [ScanRecord.getBytes] from the [scanResult] or an empty [ByteArray]
	 * if `null`.
	 */
	val scanRecordBytes: ByteArray
		get() =
			scanResult.scanRecord?.bytes ?: ByteArray(0)

	/**
	 * The set of [UUID]s from the [scanResult]'s [ScanRecord.getServiceUuids]
	 * or an empty set if `null`.
	 */
	val serviceUUIDs: Set<UUID>
		get() =
			scanResult.scanRecord?.serviceUuids?.map { it.uuid }?.toSet() ?: setOf()

	/** The index of the [scanRecordBytes] that is populated with data. */
	private var lastScanDataByteIndex = 0

	/**
	 * The subrange of [scanRecordBytes] bytes that represent all of the
	 * [advertisementData].
	 */
	val populatedAdvertisementBytes: ByteArray
		get() =
			scanRecordBytes.sliceArray(0..lastScanDataByteIndex)

	/** This [Advertisement]'s [List] of [AdvertisementData]. */
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
			// There must be at least one byte after next size
			while (currentPosition < raw.size - 1)
			{
				// The size byte indicates how many bytes are included in the
				// block of advertised data not including the size byte.
				val nextSize = raw[currentPosition++].toInt()
				if (nextSize == 0)
				{
					continue
				}
				// The Advertise Data (AD) is always the byte immediately after
				// size byte
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
