package com.bitwisearts.android.ble.gatt.attribute.common

import com.bitwisearts.android.ble.gatt.attribute.Characteristic
import com.bitwisearts.android.ble.gatt.attribute.Descriptor
import com.bitwisearts.android.ble.gatt.attribute.Service
import com.bitwisearts.android.ble.standardUUID

/**
 * [CommonService] that exposes manufacturer and/or vendor information about a 
 * device. See 
 * [Device Information Service 1.1](https://www.bluetooth.com/specifications/specs/device-information-service-1-1/)
 *
 * @author Richard Arriaga
 */
object DeviceInformationService : CommonService(
	standardUUID(0x180A),
	"Device Information Service"
) {
	override val characteristics: Set<Characteristic> =
		setOf(
			ManufacturerName,
			ModelNumber,
			SerialNumber,
			HardwareRevision,
			FirmwareRevision,
			SoftwareRevision,
			PnPId,
			SystemId)
}

/**
 * [CommonCharacteristic] represents the name of the manufacturer of the device.
 *
 * @author Richard Arriaga
 */
object ManufacturerName: CommonCharacteristic(
	standardUUID(0x2A29), "Manufacturer Name String")
{
	override val service: Service get() = DeviceInformationService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that represents the model number of the device
 * assigned by the manufacturer.
 *
 * @author Richard Arriaga
 */
object ModelNumber: CommonCharacteristic(
	standardUUID(0x2A24), "Model Number String")
{
	override val service: Service get() = DeviceInformationService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that represents the serial number of this instance of
 * this device.
 *
 * @author Richard Arriaga
 */
object SerialNumber: CommonCharacteristic(
	standardUUID(0x2A25), "Serial Number String")
{
	override val service: Service get() = DeviceInformationService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that represents the hardware revision for the hardware
 * in the device.
 *
 * @author Richard Arriaga
 */
object HardwareRevision: CommonCharacteristic(
	standardUUID(0x2A27), "Hardware Revision String")
{
	override val service: Service get() = DeviceInformationService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that represents the firmware revision for the firmware
 * within the device.
 *
 * @author Richard Arriaga
 */
object FirmwareRevision: CommonCharacteristic(
	standardUUID(0x2A26), "Firmware Revision String")
{
	override val service: Service get() = DeviceInformationService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that represents the software revision for the software
 * within the device.
 *
 * @author Richard Arriaga
 */
object SoftwareRevision: CommonCharacteristic(
	standardUUID(0x2A28), "Software Revision String")
{
	override val service: Service get() = DeviceInformationService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that is a set of values that shall be used to create a
 * device ID value that is unique for this device. Included in the
 * characteristic area Vendor ID source field, a Vendor ID field, a Product ID
 * field,and a Product Version field.These values are used to identify all
 * devices of a given type/model/version using numbers.
 *
 * @author Richard Arriaga
 */
object PnPId: CommonCharacteristic(
	standardUUID(0x2A50), "PnP ID")
{
	override val service: Service get() = DeviceInformationService
	override val descriptors: Set<Descriptor> = setOf()
}

/**
 * [CommonCharacteristic] that shall represent a structure containing an
 * Organizationally Unique Identifier (OUI) followed by a manufacturer-defined
 * identifier and is unique for each individual instance of the product.
 *
 * @author Richard Arriaga
 */
object SystemId: CommonCharacteristic(
	standardUUID(0x2A23), "System Id")
{
	override val service: Service get() = DeviceInformationService
	override val descriptors: Set<Descriptor> = setOf()
}