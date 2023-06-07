package com.bitwisearts.android.ble.advertisement

/**
 * [Flags][AdvertisingDataType.FLAG]-specific
 * [AdvertisementData].
 *
 * The bit representation is:
 * * 0 - LE Limited Discoverable Mode
 * * 1 - LE General Discoverable Mode
 * * 2 - BR/EDR Not Supported
 * * 3 - Simultaneous LE and BR/EDR to Same Device Capa-ble (Controller)
 * * 4 - Simultaneous LE and BR/EDR to Same Device Capa-ble (Host)
 * * 5..7 - Reserved
 *
 * @author Richard Arriaga
 */
class FlagsData constructor (
	data: ByteArray
): AdvertisementData(AdvertisingDataType.FLAG, data)
{
	/**
	 * The flag value.
	 */
	val flag = data[0].toInt()

	/**
	 * LE Limited Discoverable Mode if `true`.
	 */
	val leLimitedDiscoverableMode: Boolean = flag.and(1) == 1

	/**
	 * LE General Discoverable Mode if `true`.
	 */
	val leGeneralDiscoverableMode: Boolean = flag.and(2) == 2

	/**
	 *  BR/EDR Not Supported if `true`.
	 */
	val brEdrNotSupported: Boolean = flag.and(4) == 4

	/**
	 * Simultaneous LE and BR/EDR to Same Device Capa-ble (Controller) if
	 * `true`.
	 */
	val simultaneousLeBrEdrController: Boolean = flag.and(8) == 8

	/**
	 * Simultaneous LE and BR/EDR to Same Device Capa-ble (Host) if `true`.
	 */
	val simultaneousLeBrEdrHost: Boolean = flag.and(16) == 16
}