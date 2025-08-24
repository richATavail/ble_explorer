package com.bitwisearts.android.ble

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class BleIdentifierUtilityTest {

	@Test
	fun testStandardUUID_withHexString() {
		val uuid = standardUUID("180D")
		assertEquals(
			UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"),
			uuid
		)
	}

	@Test
	fun testStandardUUID_withSig16BitId() {
		val uuid = standardUUID(0x180D)
		assertEquals(
			UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"),
			uuid
		)
	}

	@Test
	fun testStandardUUID_withBytes() {
		val uuid = standardUUID(0x18.toByte(), 0x0D.toByte())
		assertEquals(
			UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"),
			uuid
		)
	}

	@Test
	fun testStandardUUID_equivalence() {
		val uuidFromString = standardUUID("180D")
		val uuidFromLong = standardUUID(0x180D)
		val uuidFromBytes = standardUUID(0x18.toByte(), 0x0D.toByte())
		assertEquals(uuidFromString, uuidFromLong)
		assertEquals(uuidFromLong, uuidFromBytes)
	}
}