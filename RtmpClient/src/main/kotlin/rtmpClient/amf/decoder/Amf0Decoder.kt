package rtmpClient.amf.decoder

import java.io.DataInputStream
import java.io.IOException
import java.util.*

class AMF0Decoder(data: ByteArray) {

    private val input = DataInputStream(data.inputStream())
    fun decodeAll(): List<Any?> {
        val result = mutableListOf<Any?>()
        while (true) {
            try {
                val node = decode()
                result.add(node)
            } catch (e: IOException) {
                println(e)
                break
            }
        }
        return result
    }

    @Throws(IOException::class)
    fun decode(): Any? {
        val type = input.readByte()
        return when (type.toInt()) {
            AMF0Types.NUMBER -> input.readDouble()
            AMF0Types.BOOLEAN -> input.readByte() == 0x01.toByte()
            AMF0Types.STRING -> readAMF0String()
            AMF0Types.OBJECT -> readAMF0Object()
            AMF0Types.NULL -> null
            AMF0Types.UNDEFINED -> null
            AMF0Types.ECMA_ARRAY -> readAMF0EcmaArray()
            AMF0Types.STRICT_ARRAY -> readAMF0StrictArray()
            AMF0Types.DATE -> readAMF0Date()
            AMF0Types.TYPED_OBJECT -> readTypedObject()
            else -> throw IOException("Unsupported AMF0 type: $type")
        }
    }

    private fun readTypedObject(): Any? {
        val objectName = readAMF0String()
        val objectValue = readAMF0Object()
        return objectName to objectValue
    }

    @Throws(IOException::class)
    private fun readAMF0String(): String {
        val length = input.readShort()
        val buffer = ByteArray(length.toInt())
        input.readFully(buffer)
        return String(buffer, Charsets.UTF_8)
    }

    @Throws(IOException::class)
    private fun readAMF0Object(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        while (true) {
            val propertyName = readAMF0String()
            if (propertyName.isEmpty()) {
                val nextType = input.readByte().toInt()
                if (nextType == AMF0Types.OBJECT_END) {
                    break
                } else {
                    throw IOException("Invalid AMF0 object format")
                }
            } else {
                val value = decode()
                result[propertyName] = value
            }
        }
        return result
    }

    @Throws(IOException::class)
    private fun readAMF0EcmaArray(): Map<String, Any?> {
        val length = input.readInt()
        val result = mutableMapOf<String, Any?>()
        for (i in 0 until length) {
            val propertyName = readAMF0String()
            val value = decode()
            result[propertyName] = value
        }
        return result
    }

    @Throws(IOException::class)
    private fun readAMF0StrictArray(): List<Any?> {
        val length = input.readInt()
        val result = mutableListOf<Any?>()
        for (i in 0 until length) {
            val value = decode()
            result.add(value)
        }
        return result
    }

    @Throws(IOException::class)
    private fun readAMF0Date(): Date {
        val milliseconds = input.readDouble().toLong()
        val timezoneOffset = input.readShort().toInt()
        return Date(milliseconds)
    }
}

object AMF0Types {
    const val NUMBER = 0
    const val BOOLEAN = 1
    const val STRING = 2
    const val OBJECT = 3
    const val NULL = 5
    const val UNDEFINED = 6
    const val ECMA_ARRAY = 8
    const val STRICT_ARRAY = 10
    const val DATE = 11
    const val OBJECT_END = 9
    const val TYPED_OBJECT = 16
}