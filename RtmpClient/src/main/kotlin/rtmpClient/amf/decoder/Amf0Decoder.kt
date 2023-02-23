package rtmpClient.amf.decoder

import io.ktor.utils.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import rtmpClient.amf.Amf0Node
import rtmpClient.amf.Amf0Node.*

class Amf0Decoder(val input: ByteReadChannel) {
    suspend fun read(): Amf0Node = coroutineScope {
        val type = input.readByte()
        when (type.toInt()) {
            Amf0Number.type -> readNumber()
            Amf0Boolean.type -> readBoolean()
            Amf0String.type -> readString()
            Amf0Object.type -> readObject()
            Amf0Null.type -> readNull()
            else -> throw Exception("Invalid AMF0 type: $type")
        }
    }

    fun readNull() = Amf0Null

    suspend fun readObject(): Amf0Object = coroutineScope {
        val mutableMapOf = mutableMapOf<String, Amf0Node>()
        val obj = Amf0Object(mutableMapOf)
        while (isActive) {
            val key = read() as? Amf0String ?: break
            mutableMapOf[key.value] = read()
        }
        // skip the last three bytes of object end marker
        input.discardExact(3)
        obj
    }

    suspend fun readString(): Amf0String {
        val length = input.readShort()
        val byteArray = ByteArray(length.toInt())
        input.readFully(byteArray)
        return Amf0String(byteArray.decodeToString())
    }

    suspend fun readBoolean() = Amf0Boolean(input.readByte() != 0.toByte())

    suspend fun readNumber() = Amf0Number(input.readDouble())
}