BLE Explorer
--------------------------------------------------------------------------------

This is a refresher exploration of BLE utilizing patterns I have used in the
past, but adding new types for managing received data such as Kotlin Flows. I 
have been tinkering with this locally on my computer (**not in git**) for a few 
months now. This is currently a work-in-progress. I have been updating the 
codebase as libraries have been updated. 

**NOTE** This is definitely not a hardened app.

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
    - Writes characteristics (_untested_)
      - [CharacteristicWriteRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/CharacteristicWriteRequest.kt) 
      - Supports chunking for large writes
      - Supports resending on failure up to a configurable max retry count
    - Reads descriptors
      - [DescriptorReadRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/DescriptorReadRequest.kt)
    - Writes descriptors (_untested_)
      - [DescriptorWriteRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/DescriptorWriteRequest.kt)
      - Supports chunking for large writes
      - Supports resending on failure up to a configurable max retry count
    - Subscribes to notifications (_untested_)
      - [EnableNotifyCharacteristicRequest](ble/src/main/kotlin/com/bitwisearts/android/ble/request/EnableNotifyCharacteristicRequest.kt)
- BLE Device representation ([BleDevice](ble/src/main/kotlin/com/bitwisearts/android/ble/BleDevice.kt))
- Provides some common BLE Attributes (see [attributes](ble/src/main/kotlin/com/bitwisearts/android/ble/gatt/attribute/common))
- A test [app](app) UI to interact with devices

**ANOTHER NOTE** Development is slow moving here, not everything is perfect or
necessarily even "good". 