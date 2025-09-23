package com.bitwisearts.android.explorer.ble.peripheral

import android.util.Log
import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.gatt.attribute.common.ClientCharacteristicConfiguration
import java.io.ByteArrayInputStream
import java.util.UUID

/**
 * The sample [Service] for demonstration and testing purposes. This is used by
 * the [SampleBlePeripheral].
 *
 * @author Richard Arriaga
 */
object SampleBleService: Service(
	UUID.fromString("365F70D4-DE7C-43CA-91FE-A4F03E5F0F5A"),
	"Sample Service"
) {
	override val characteristics: Set<Characteristic> by lazy {
		setOf(SampleReadCharacteristic, SampleWriteCharacteristic,
			SampleNotifyCharacteristic)
	}
}

/**
 * A sample [Characteristic] that is readable for demonstration and testing
 * purposes. This is used by the [SampleBlePeripheral].
 *
 * @author Richard Arriaga
 */
object SampleReadCharacteristic: Characteristic(
	UUID.fromString("365F70D5-DE7C-43CA-91FE-A4F03E5F0F5A"),
	"Sample Read Characteristic"
) {
	override val service: Service
		get() = SampleBleService
	override val descriptors: Set<Descriptor> = emptySet()

	override fun stringifyValue(value: ByteArray): String =
		try
		{
			readUnsignedInt(ByteArrayInputStream(value)).toString()
		}
		catch (e: SerializationException)
		{
			Log.e("SampleReadCharacteristic",
				"Error reading unsigned int", e)
			"«INVALID DATA»: ${stringifyValue(value)}"
		}
}

/**
 * A sample [Characteristic] that is writable for demonstration and testing
 * purposes. This is used by the [SampleBlePeripheral].
 *
 * @author Richard Arriaga
 */
object SampleWriteCharacteristic: Characteristic(
	UUID.fromString("365F70D6-DE7C-43CA-91FE-A4F03E5F0F5A"),
	"Sample Write Characteristic"
) {
	override val service: Service
		get() = SampleBleService
	override val descriptors: Set<Descriptor> = emptySet()

	override fun stringifyValue(value: ByteArray): String =
		value.decodeToString()
}

/**
 * A sample [Characteristic] that is notifiable for demonstration and testing
 * purposes. This is used by the [SampleBlePeripheral].
 *
 * @author Richard Arriaga
 */
object SampleNotifyCharacteristic: Characteristic(
	UUID.fromString("365F70D7-DE7C-43CA-91FE-A4F03E5F0F5A"),
	"Sample Notify Characteristic"
) {
	override val service: Service
		get() = SampleBleService
	override val descriptors: Set<Descriptor> =
		setOf(ClientCharacteristicConfiguration)
	// TODO implement chunking based notify
	override fun stringifyValue(value: ByteArray): String = String(value)
}