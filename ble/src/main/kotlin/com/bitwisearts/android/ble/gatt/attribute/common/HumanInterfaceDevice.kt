package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.standardUUID

/**
 * [CommonService] that exposes HID reports and other HID data intended for HID 
 * Hosts and HID Devices. See 
 * [Human Interface Device Service 1.0](https://www.bluetooth.com/specifications/specs/hid-service-1-0/)
 *
 * @author Richard Arriaga
 */
object HumanInterfaceDevice: CommonService(
	standardUUID(0x1812), "Human Interface Device")
{
	override val characteristics: Set<Characteristic> =
		setOf(
			ProtocolMode, ReportMap, Report, BootKeyboardInputReport,
			BootKeyboardOutputReport, BootMouseInputReport, HIDInformation,
			HIDControlPoint)
}

/**
 * [CommonCharacteristic] that is used to expose the current protocol mode of
 * the HID Service with which it is associated, or to set the desired protocol
 * mode of the HID Service.
 *
 * @author Richard Arriaga
 */
object ProtocolMode: CommonCharacteristic(
	standardUUID(0x2A4E), "Protocol Mode")
{
	override val service: Service get() = HumanInterfaceDevice
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that is used to define formatting information forInput
 * Report, Output Report, and Feature Report data transferred between an HID
 * Device and HID Host. It also contains information on how this data can be
 * used regarding physical aspects of the device(i.e. that the device functions
 * as a keyboard, for example, or has multiple functions such as a keyboard and
 * volume controls).
 *
 * @author Richard Arriaga
 */
object ReportMap: CommonCharacteristic(
	standardUUID(0x2A4B), "Report Map")
{
	override val service: Service get() = HumanInterfaceDevice
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] with properties of the [Report] characteristic that
 * are dependent on the Report Type. Data contained in the characteristic
 * are shown in:
 *
 * | Report Type    | Requirement | Read | Write | Write Without Response | Notify |
 * |----------------|-------------|------|-------|------------------------|--------|
 * | Input Report   | C.1         | M    | O     | X                      | M      |
 * | Output Report  | C.1         | M    | M     | M                      | X      |
 * | Feature Report | C.1         | M    | M     | X                      | X      |
 *
 * @author Richard Arriaga
 */
object Report: CommonCharacteristic(standardUUID(0x2A4D), "Report")
{
	override val service: Service get() = HumanInterfaceDevice
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that is used to transfer fixed format and length Input
 * Report data between a HID Host operating in Boot Protocol Mode and a HID
 * Service corresponding to a boot keyboard.
 *
 * @author Richard Arriaga
 */
object BootKeyboardInputReport: CommonCharacteristic(
	standardUUID(0x2A22), "Boot Keyboard Input Report")
{
	override val service: Service get() = HumanInterfaceDevice
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that is used to transfer fixed format and length
 * Output Report data between a HID Host operating in Boot Protocol Mode and an
 * HID Service corresponding toa boot keyboard.
 *
 * @author Richard Arriaga
 */
object BootKeyboardOutputReport: CommonCharacteristic(
	standardUUID(0x2A32), "Boot Keyboard Output Report")
{
	override val service: Service get() = HumanInterfaceDevice
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that is used to transfer fixed format and length Input
 * Report data between an HID Host operating in Boot Protocol Mode and an HID
 * Service corresponding toa boot mouse.
 *
 * @author Richard Arriaga
 */
object BootMouseInputReport: CommonCharacteristic(
	standardUUID(0x2A33), "Boot Mouse Input Report")
{
	override val service: Service get() = HumanInterfaceDevice
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that is used to hold a set of values known as the HID
 * Device's HID Attributes.
 *
 * @author Richard Arriaga
 */
object HIDInformation: CommonCharacteristic(
	standardUUID(0x2A4a), "HID Information")
{
	override val service: Service get() = HumanInterfaceDevice
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that contains the following enumeration of values:
 *
 * | Value     | Command      | Description                                                    |
 * |-----------|--------------|----------------------------------------------------------------|
 * | 0x00      | Suspend      | Informs HID Device that HID Host is entering the Suspend State |
 * | 0x01      | Exit Suspend | Informs HID Device that HID Host is exiting Suspend State      |
 * | 0x02-0xFF | N/A          | Reserved for future use                                        |
 *
 * @author Richard Arriaga
 */
object HIDControlPoint: CommonCharacteristic(
	standardUUID(0x2A4c), "HID Control Point")
{
	override val service: Service get() = HumanInterfaceDevice
	override val descriptors: Set<Descriptor> = setOf()
}