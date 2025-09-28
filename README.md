BLE Explorer
--------------------------------------------------------------------------------

This is a refresher exploration of BLE utilizing patterns I have used in the
past, but adding new types for managing received data such as Kotlin Flows. I 
have been tinkering with this on and off for a few years. This is currently a 
work-in-progress. I have been updating the codebase as libraries have been 
updated. 

**NOTE** This is definitely not a hardened app.

**NOTE** This is not to demonstrate best practices of UI/UX design nor a model
app for Jetpack Compose usage; just enough to demonstrate the BLE functionality.

**NOTE** There is no road map for this project.

As of right now it:
- Scans for devices using [BleScan](ble/src/main/kotlin/com/bitwisearts/android/ble/scan/BleScan.kt)
- [GATT Status Codes](ble/src/main/kotlin/com/bitwisearts/android/ble/gatt/GattStatusCode.kt)
  - [Known GATT Status Codes](ble/src/main/kotlin/com/bitwisearts/android/ble/gatt/KnownGattStatusCode.kt)
- [Advertisements](ble/src/main/kotlin/com/bitwisearts/android/ble/advertisement/Advertisement.kt)
  - [Advertising Data Types](ble/src/main/kotlin/com/bitwisearts/android/ble/advertisement/AdvertisingDataType.kt)
    - [Flags](ble/src/main/kotlin/com/bitwisearts/android/ble/advertisement/FlagsData.kt)
    - [Manufacturer Data](ble/src/main/kotlin/com/bitwisearts/android/ble/advertisement/ManufacturerData.kt)
- Connects to devices using [BleConnection](ble/src/main/kotlin/com/bitwisearts/android/ble/connection/BleConnection.kt)
  - Supports MTU negotiation
  - Handles connection state changes
  - Handles disconnections
  - Handles connection timeouts
  - Discovers services and characteristics
    - [Service](ble/src/main/kotlin/com/bitwisearts/android/ble/gatt/attribute/Service.kt)
    - [Characteristic](ble/src/main/kotlin/com/bitwisearts/android/ble/gatt/attribute/Characteristic.kt)
    - [Descriptor](ble/src/main/kotlin/com/bitwisearts/android/ble/gatt/attribute/Descriptor.kt)
    - [BLE Attributes](ble/src/main/kotlin/com/bitwisearts/android/ble/gatt/attribute)
  - BLE Requests
    - Manages a queue of requests to the device, processing them one at a time
    - Synchronous blocking requests see 
      - `BleConnection.readCharacteristic`
      - `BleConnection.readDescriptor`
      - `BleConnection.writeCharacteristic`
      - `BleConnection.writeDescriptor`
    - Asynchronous requests processed via callbacks. See `BleConnection.submitBleRequest`
    - [BleRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/BleRequest.kt)
    - [BleRequestResult](ble/src/main/kotlin/com/bitwisearts/android/ble/request/BleRequestResult.kt)
    - Reads characteristics
      - [CharacteristicReadRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/CharacteristicReadRequest.kt) 
    - Writes characteristics
      - [CharacteristicWriteRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/CharacteristicWriteRequest.kt) 
      - Supports chunking for large writes
      - Supports resending on failure up to a configurable max retry count
    - Reads descriptors
      - [DescriptorReadRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/DescriptorReadRequest.kt)
    - Writes descriptors
      - [DescriptorWriteRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/DescriptorWriteRequest.kt)
      - Supports chunking for large writes
      - Supports resending on failure up to a configurable max retry count
    - Subscribes to notifications
      - [EnableNotifyCharacteristicRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/EnableNotifyCharacteristicRequest.kt)
- BLE Device representation ([BleDevice](ble/src/main/kotlin/com/bitwisearts/android/ble/BleDevice.kt)) - the Central's representation of 
  a Peripheral; this is the place to start exploring as any interaction with a
  Peripheral should be done through a subclass of `BleDevice`.
- Provides some common BLE Attributes (see [attributes](ble/src/main/kotlin/com/bitwisearts/android/ble/gatt/attribute/common))
- A test [app](app) UI to interact with devices that demonstrates the following 
  functionality:
  - Scanning
  - Scan results
  - Connecting to a device
  - Connection state changes
  - MTU changes
  - Services discovered
  - Sample Communication between a
    [SampleBlePeripheral](app/src/main/kotlin/com/bitwisearts/android/explorer/ble/peripheral/SampleBlePeripheral.kt)
    via Ble Peripheral screen and a
    [SampleBleDevice](app/src/main/kotlin/com/bitwisearts/android/explorer/ble/peripheral/SampleBleDevice.kt),
    which is an implementation of a `BleDevice`, via Sample Peripherals view. 
    This demonstrates:
      - Scanning for a specific device type using a service UUID filter
      - Connection
      - Read Characteristics
      - Write Characteristics
      - Notifications using the [SamplePeripheralProtocol](app/src/main/kotlin/com/bitwisearts/android/explorer/ble/peripheral/SamplePeripheralProtocol.kt)
      - Disconnecting

**ANOTHER NOTE** Development is slow moving here, not everything is perfect
