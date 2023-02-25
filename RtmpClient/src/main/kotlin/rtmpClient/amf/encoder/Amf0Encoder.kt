package rtmpClient.amf.encoder

import io.ktor.utils.io.*
import rtmpClient.amf.Amf0Node
import rtmpClient.amf.Amf0Node.*
import rtmpClient.amf.toAmf0String
import java.nio.ByteBuffer

class Amf0Encoder(val output: ByteWriteChannel) {
    suspend fun writeNumber(number: Amf0Number) {
        output.writeByte(Amf0Number.TYPE)

        val array = ByteArray(8)
        ByteBuffer.wrap(array).putDouble(number.value)
        output.writeFully(array)
    }

    suspend fun writeBoolean(boolean: Amf0Boolean) {
        output.writeByte(Amf0Boolean.TYPE)
        output.writeByte(if (boolean.value) 0x01 else 0x00)
    }

    suspend fun writeString(string: Amf0String) {
        output.writeByte(Amf0String.TYPE)

        val stringArray = string.value.toByteArray()
        output.writeShort(stringArray.size)
        output.writeFully(stringArray)
    }

    suspend fun writeObject(obj: Amf0Object) {
        output.writeByte(Amf0Object.TYPE)
        for (entry in obj.value) {
            writeString(entry.key.toAmf0String())
            write(entry.value)
        }
        output.writeByte(0x00)
        output.writeByte(0x00)
        output.writeByte(0x09)
    }

    suspend fun writeNull() {
        output.writeByte(Amf0Null.TYPE)
    }

    suspend fun write(node: Amf0Node) {
        when (node) {
            is Amf0Boolean -> writeBoolean(node)
            is Amf0ECMAArray -> TODO()
            Amf0Null -> writeNull()
            is Amf0Number -> writeNumber(node)
            is Amf0Object -> writeObject(node)
            is Amf0String -> writeString(node)
            else -> TODO()
        }
    }
}