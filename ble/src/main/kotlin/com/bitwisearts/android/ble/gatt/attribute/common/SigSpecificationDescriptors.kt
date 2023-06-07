package com.bitwisearts.android.ble.gatt.attribute.common

import android.bluetooth.BluetoothGattCharacteristic
import com.bitwisearts.android.ble.gatt.attribute.BleCharacteristicProperty
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.standardUUID

////////////////////////////////////////////////////////////////////////////////
//                  SIG Descriptor Assigned Section 3.7                       //
//         https://www.bluetooth.com/specifications/assigned-numbers/         //
////////////////////////////////////////////////////////////////////////////////

/**
 * This descriptor, when present, simply contains the two additional property
 * bits for a [BleCharacteristicProperty]:
 *
 * * **Queued Write** - If set, allows clients to use the Queued Writes ATT
 *   operations on the associated [BluetoothGattCharacteristic]. Splitting the 
 *   write operation into several commands makes it possible to write attribute 
 *   values that are longer than a single packet. Such a series of write 
 *   commands, called a `Queued Write` operation, consists of multiple prepare 
 *   write commands followed by one execute write command.
 * * **Writable Auxiliaries** - If set (`0x80`), a client can write to the
 *   descriptor, [CharacteristicUserDescription].
 */
object CharacteristicExtendedProperties: CommonDescriptor(
	standardUUID(0x2900), 
	"Characteristic Extended Properties"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * The value of this description is a user-readable UTF-8 string describing the
 * associated [BluetoothGattCharacteristic].
 *
 * @author Richard Arriaga
 */
object CharacteristicUserDescription : CommonDescriptor(
	standardUUID(0x2901),
	"Characteristic User Description"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * By writing to this attribute, a GATT server can configure the
 * SimpleProfileChar4 for notifications, by writing `0x0001`, or indications,
 * by writing `0x0002`. Writing 0x0000 to this attribute disable notifications 
 * and indications.
 *
 * This [Descriptor] acts as a switch, enabling or disabling server-initiated 
 * updates, but only for the [BluetoothGattCharacteristic] in which it finds 
 * itself enclosed.
 *
 * @author Richard Arriaga
 */
object ClientCharacteristicConfiguration : CommonDescriptor(
	standardUUID(0x2902),
	"Client Characteristic Configuration"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object ServerCharacteristicConfiguration : CommonDescriptor(
	standardUUID(0x2903),
	"Server Characteristic Configuration"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object CharacteristicPresentationFormat : CommonDescriptor(
	standardUUID(0x2904),
	"Characteristic Presentation Format"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object CharacteristicAggregateFormat : CommonDescriptor(
	standardUUID(0x2905),
	"Characteristic Aggregate Format"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object ValidRange : CommonDescriptor(
	standardUUID(0x2906),
	"Valid Range"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object ExternalReportReference : CommonDescriptor(
	standardUUID(0x2907),
	"External Report Reference"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object ReportReference : CommonDescriptor(
	standardUUID(0x2908),
	"Report Reference"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object NumberOfDigits : CommonDescriptor(
	standardUUID(0x2909),
	"Number of Digits"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object ValueTriggerSetting : CommonDescriptor(
	standardUUID(0x290A),
	"Value Trigger Setting"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object EnvironmentalSensingConfiguration : CommonDescriptor(
	standardUUID(0x290B),
	"Client Characteristic Configuration"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object EnvironmentalSensingMeasurement : CommonDescriptor(
	standardUUID(0x290C),
	"Environmental Sensing Measurement"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object EnvironmentalSensingTriggerSetting : CommonDescriptor(
	standardUUID(0x290D),
	"Environmental Sensing Trigger Setting"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object TimeTriggerSetting : CommonDescriptor(
	standardUUID(0x290E),
	"Time Trigger Setting"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}

/**
 * See [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)
 */
object CompleteBrEdrTransportBlockData : CommonDescriptor(
	standardUUID(0x290F),
	"Complete BR-EDR Transport Block Data"
) {
	init
	{
		commonDescriptors[uuid] = this
	}
}