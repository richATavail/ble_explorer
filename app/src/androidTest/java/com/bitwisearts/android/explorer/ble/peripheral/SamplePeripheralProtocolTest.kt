package com.bitwisearts.android.explorer.ble.peripheral

import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream

class MessageTest {
    @Test
    fun testMessageConstructor() {
        val message = Message(1, "Hello, World!")
        assertEquals(1, message.id)
        assertEquals("Hello, World!", message.message)
    }

    @Test
    fun testMessageConstructorWithAutoId() {
        val message1 = Message("First message")
        val message2 = Message("Second message")

        // IDs should be different and auto-incremented
        assertNotEquals(message1.id, message2.id)
        assertEquals(message1.id + 1, message2.id)
    }

    @Test
    fun testMessageSerialization() {
        val message = Message(42, "Test message")
        val serialized = message.serialize()

        // Test the structure indirectly through deserialization
        val deserializer = MessageDeserializer(serialized)
        val deserialized = deserializer.deserialize()

        assertEquals(42, deserialized.id)
        assertEquals("Test message", deserialized.message)
    }
}

class MessageDeserializerTest {
    @Test
    fun testMessageDeserializerWithFullPayload() {
        val message = Message(123, "Hello BLE!")
        val serialized = message.serialize()

        val deserializer = MessageDeserializer(serialized)
        assertTrue(deserializer.hasAllBytes)

        val deserialized = deserializer.deserialize()
        assertEquals(123, deserialized.id)
        assertEquals("Hello BLE!", deserialized.message)
    }

    @Test
    fun testMessageDeserializerWithPartialPayload() {
        val message = Message(456, "This is a longer message to ensure fragmentation")
        val serialized = message.serialize()

        // Split the payload into two parts
        val firstHalf = serialized.sliceArray(0 until serialized.size / 2)
        val secondHalf = serialized.sliceArray(serialized.size / 2 until serialized.size)

        val deserializer = MessageDeserializer(firstHalf)
        assertFalse(deserializer.hasAllBytes)

        deserializer.additionalPayload(secondHalf)
        assertTrue(deserializer.hasAllBytes)

        val deserialized = deserializer.deserialize()
        assertEquals(456, deserialized.id)
        assertEquals("This is a longer message to ensure fragmentation", deserialized.message)
    }

    @Test(expected = SerializationException::class)
    fun testDeserializeIncompleteMessage() {
        val message = Message(789, "Another test message")
        val serialized = message.serialize()

        // Only use the first few bytes
        val partial = serialized.sliceArray(0 until 10)
        val deserializer = MessageDeserializer(partial)

        // This should throw a SerializationException
        deserializer.deserialize()
    }

    @Test
    fun testReadSizePrefixAndRemainingBytes() {
        val testBytes = byteArrayOf(5, 10, 15, 20, 25)
        val (sizePrefix, remaining) = MessageDeserializer.readSizePrefix(testBytes)

        assertEquals(5, sizePrefix)
        assertArrayEquals(byteArrayOf(10, 15, 20, 25), remaining)
    }

    @Test
    fun testCurrentReceivedBytes() {
        val message = Message(42, "Test")
        val serialized = message.serialize()

        // Split into two parts
        val part1 = serialized.sliceArray(0 until 5)
        val part2 = serialized.sliceArray(5 until serialized.size)

        val deserializer = MessageDeserializer(part1)
        assertEquals(part1.size - 1, deserializer.currentReceivedBytes) // -1 for the size prefix

        deserializer.additionalPayload(part2)
        assertEquals(serialized.size - 1, deserializer.currentReceivedBytes) // -1 for the size prefix
    }
}

class UnsignedIntSerializationTest {
    @Test
    fun testSerializeUnsignedInt() {
        // Test small values
        assertArrayEquals(byteArrayOf(0), serializeUnsignedInt(0))
        assertArrayEquals(byteArrayOf(1), serializeUnsignedInt(1))
        assertArrayEquals(byteArrayOf(127), serializeUnsignedInt(127))

        // Test values that require multiple bytes
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x01), serializeUnsignedInt(128))
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0x01), serializeUnsignedInt(255))
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x03), serializeUnsignedInt(0xFFFF))

        // Test a large value
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x01),
            serializeUnsignedInt(536870911)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSerializeNegativeInt() {
        serializeUnsignedInt(-1)
    }

    @Test
    fun testReadUnsignedInt() {
        // Test small values
        assertEquals(0, readUnsignedInt(ByteArrayInputStream(byteArrayOf(0))))
        assertEquals(1, readUnsignedInt(ByteArrayInputStream(byteArrayOf(1))))
        assertEquals(127, readUnsignedInt(ByteArrayInputStream(byteArrayOf(127))))

        // Test values that require multiple bytes
        assertEquals(128, readUnsignedInt(ByteArrayInputStream(byteArrayOf(0x80.toByte(), 0x01))))
        assertEquals(255, readUnsignedInt(ByteArrayInputStream(byteArrayOf(0xFF.toByte(), 0x01))))
        assertEquals(0xFFFF, readUnsignedInt(ByteArrayInputStream(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x03))))

        // Test a large value
        assertEquals(
            0x1FFFFFFF,
            readUnsignedInt(ByteArrayInputStream(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x01)))
        )
    }

    @Test(expected = SerializationException::class)
    fun testReadUnsignedIntWithEmptyStream() {
        readUnsignedInt(ByteArrayInputStream(byteArrayOf()))
    }

    @Test(expected = SerializationException::class)
    fun testReadUnsignedIntWithTooManyBytes() {
        // Create a byte array with 6 bytes, all with MSB set to 1, which should cause an error
        val badBytes = byteArrayOf(
            0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01
        )
        readUnsignedInt(ByteArrayInputStream(badBytes))
    }
}

class SerializationExceptionTest {
    @Test
    fun testSerializationException() {
        val message = "Test error message"
        val exception = SerializationException(message)
        assertEquals(message, exception.message)
    }
}
