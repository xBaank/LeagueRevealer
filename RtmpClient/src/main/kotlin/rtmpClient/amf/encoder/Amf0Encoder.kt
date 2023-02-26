package rtmpClient.amf.encoder

import io.ktor.utils.io.*
import rtmpClient.amf.*
import rtmpClient.packet.CHUNK_SIZE
import java.nio.ByteBuffer

class OwnWriteChannel(val output: ByteWriteChannel) {
    var position = 0
    suspend fun writeWithC3(bytes: ByteArray) = bytes.forEach { byte ->
        if (position != 0 && position % CHUNK_SIZE == 0) {
            output.writeByte(0xC3.toByte())
        }
        output.writeByte(byte)
        position++
    }

    suspend fun writeByte(b: Int) {
        writeWithC3(byteArrayOf(b.toByte()))
    }

    suspend fun writeDouble(d: Double) {
        val array = ByteArray(8)
        ByteBuffer.wrap(array).putDouble(d)
        writeWithC3(array)
    }

    suspend fun writeShort(s: Int) {
        val array = ByteArray(2)
        ByteBuffer.wrap(array).putShort(s.toShort())
        writeWithC3(array)
    }

    suspend fun writeInt(i: Int) {
        val array = ByteArray(4)
        ByteBuffer.wrap(array).putInt(i)
        writeWithC3(array)
    }

    suspend fun writeFully(src: ByteArray, offset: Int, length: Int) {
        writeWithC3(src.sliceArray(offset until offset + length))
    }

    suspend fun writeFully(src: ByteArray) = writeFully(src, 0, src.size)


}

class Amf0Encoder(channel: ByteWriteChannel) {
    private val output = OwnWriteChannel(channel)

    suspend fun writeNumber(number: Amf0Number) {
        output.writeByte(Amf0Number.TYPE)
        output.writeDouble(number.value)
    }

    suspend fun writeBoolean(boolean: Amf0Boolean) {
        output.writeByte(Amf0Boolean.TYPE)
        output.writeByte(if (boolean.value) 0x01 else 0x00)
    }

    suspend fun writeString(string: Amf0String) {
        output.writeByte(Amf0String.TYPE)
        val stringArray = string.value.toByteArray(Charsets.UTF_8)
        output.writeShort(stringArray.size)
        output.writeFully(stringArray)
    }

    suspend fun writeStringKey(string: Amf0String) {
        val stringArray = string.value.toByteArray()
        output.writeShort(stringArray.size)
        output.writeFully(stringArray)
    }

    suspend fun writeObject(obj: Amf0Object) {
        output.writeByte(Amf0Object.TYPE)

        for (entry in obj.value) {
            writeStringKey(entry.key.toAmf0String())
            write(entry.value)
        }

        output.writeByte(0x00)
        output.writeByte(0x00)
        output.writeByte(0x09)
    }

    suspend fun writeTypedObject(obj: Amf0TypedObject) {
        output.writeByte(Amf0TypedObject.TYPE)

        writeStringKey(obj.name.toAmf0String())
        for (entry in obj.value) {
            if (entry.key == "configs")
                println()
            writeStringKey(entry.key.toAmf0String())
            write(entry.value)
        }
        output.writeByte(0x00)
        output.writeByte(0x00)
        output.writeByte(0x09)
    }

    suspend fun writeNull() {
        output.writeByte(Amf0Null.TYPE)
    }

    suspend fun writeUndefined() {
        output.writeByte(Amf0Undefined.TYPE)
    }

    suspend fun writeECMAArray(array: Amf0ECMAArray) {
        output.writeByte(Amf0ECMAArray.TYPE)
        output.writeInt(array.value.size)
        for (entry in array.value) {
            writeStringKey(entry.key.toAmf0String())
            write(entry.value)
        }
        output.writeByte(0x00)
        output.writeByte(0x00)
        output.writeByte(0x09)
    }

    suspend fun writeStrictArray(array: Amf0StrictArray) {
        output.writeByte(Amf0StrictArray.TYPE)
        output.writeInt(array.value.size)
        for (entry in array.value) {
            write(entry)
        }
    }


    suspend fun write(node: Amf0Node) {
        when (node) {
            is Amf0Boolean -> writeBoolean(node)
            is Amf0ECMAArray -> writeECMAArray(node)
            Amf0Null -> writeNull()
            is Amf0Number -> writeNumber(node)
            is Amf0Object -> writeObject(node)
            is Amf0String -> writeString(node)
            is Amf0Date -> TODO()
            is Amf0StrictArray -> writeStrictArray(node)
            is Amf0TypedObject -> writeTypedObject(node)
            Amf0Undefined -> writeUndefined()
        }
    }
}