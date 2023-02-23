package leagueRtmpClient.amf0

import io.ktor.utils.io.*

class Amf0Encoder(val output: ByteWriteChannel) {
    suspend fun writeNumber(number: Double) {
        output.writeByte(0x00)
        val size =
            output.writeDouble(number)
    }

    suspend fun writeBoolean(boolean: Boolean) {
        output.writeByte(0x01)
        output.writeByte(if (boolean) 0x01 else 0x00)
    }

    suspend fun writeString(string: String) {
        output.writeByte(0x02)
        output.writeStringUtf8(string)
        output.writeByte(0x00)
    }

    suspend fun writeObject(value: Map<String, Any>) {
        output.writeByte(0x03)
        for (entry in value) {
            writeString(entry.key)
            when (entry.value) {
                is Double -> writeNumber(entry.value)
                is Boolean -> writeBoolean(entry.value)
                is String -> writeString(entry.value)
                is Map<*, *> -> writeObject(entry.value as Map<String, Any>)
            }
        }
        output.writeByte(0x00)
        output.writeByte(0x00)
        output.writeByte(0x09)
    }

    suspend fun writeNull() {
        output.writeByte(0x05)
    }

    suspend fun writeECMAArray(array: Map<String, Any>) {
        output.writeByte(0x08)
        output.writeInt(array.size)
        for (entry in array) {
            writeString(entry.key)
            when (entry.value) {
                is Double -> writeNumber(entry.value)
                is Boolean -> writeBoolean(entry.value)
                is String -> writeString(entry.value)
                is Map<*, *> -> writeObject(entry.value as Map<String, Any>)
            }
        }
        output.writeByte(0x00)
        output.writeByte(0x00)
        output.writeByte(0x09)
    }

    suspend fun writeStrictArray(array: List<Any>) {
        output.writeByte(0x0a)
        output.writeInt(array.size)
        for (entry in array) {
            when (entry) {
                is Double -> writeNumber(entry)
                is Boolean -> writeBoolean(entry)
                is String -> writeString(entry)
                is Map<*, *> -> writeObject(entry as Map<String, Any>)
            }
        }
    }

    suspend fun writeDate(date: Long) {
        output.writeByte(0x0b)
        output.writeDouble(date.toDouble())
        output.writeShort(0x00)
    }
}