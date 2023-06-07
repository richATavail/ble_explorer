package com.bitwisearts.android.ble.advertisement

/**
 * Data extracted from a BLE advertisement.
 *
 * @author Richard Arriaga
 *
 * @property type
 *   The [AdvertisingDataType] that describes what kind of data is contained
 *   in this [AdvertisementData].
 * @property data
 *   The raw extracted advertisement data excluding the
 *   [AdvertisingDataType.adByte].
 */
open class AdvertisementData constructor (
	val type: AdvertisingDataType,
	val data: ByteArray)
{
	/** The length in bytes of the advertisement data. */
	val length: Int = data.size
}