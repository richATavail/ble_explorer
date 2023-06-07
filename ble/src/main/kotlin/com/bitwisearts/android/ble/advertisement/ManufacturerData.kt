package com.bitwisearts.android.ble.advertisement

/**
 * [Manufacturer][AdvertisingDataType.MAN_SPEC_DATA]-specific
 * [AdvertisementData].
 *
 * @author Richard Arriaga
 */
class ManufacturerData constructor (
	data: ByteArray
): AdvertisementData(AdvertisingDataType.MAN_SPEC_DATA, data)