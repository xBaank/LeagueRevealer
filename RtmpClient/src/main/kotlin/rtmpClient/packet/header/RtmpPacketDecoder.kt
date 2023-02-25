package rtmpClient.packet.header

import io.ktor.utils.io.*
import rtmpClient.amf.decoder.AMF0Decoder
import java.io.DataInputStream

internal const val CHUNCK_HEADER_TYPE_0: Byte = 0x00
internal const val CHUNCK_HEADER_TYPE_1: Byte = 0x01
internal const val CHUNCK_HEADER_TYPE_2: Byte = 0x02
internal const val CHUNCK_HEADER_TYPE_3: Byte = 0x03


internal const val TIMESTAMP_SIZE = 3
internal const val LENGTH_SIZE = 3
internal const val MESSAGE_ID_SIZE = 4

class RTMPPacketDecoder internal constructor(data: ByteArray, private val header: RTMPPacketHeader) {
    private val input = DataInputStream(data.inputStream())


    fun readPayload() {
        return when (header) {
            is RTMPPacketHeader0 -> {
                if (header.messageTypeId.toInt() != 0x14) return

                val length = header.lengthAsInt
                val data = ByteArray(length)
                input.read(data, 0, length)
                val node = AMF0Decoder(data).decodeAll()
                println(node)
            }

            is RTMPPacketHeader1 -> {
                if (header.messageTypeId.toInt() != 0x14) return

                val length = header.lengthAsInt
                val data = ByteArray(length)
                input.read(data, 0, length)
                val node = AMF0Decoder(data).decodeAll()
                println(node)
            }

            is RTMPPacketHeader2 -> {
                TODO()

            }

            is RTMPPacketHeader3 -> {
                TODO()

            }
        }
    }
}

suspend fun RTMPPacketDecoder(channel: ByteReadChannel): RTMPPacketDecoder {
    val header = readHeader(channel)
    val payloadData = ByteArray(header.lengthAsInt)
    channel.readFully(payloadData, 0, header.lengthAsInt)
    return RTMPPacketDecoder(payloadData, header)
}

private suspend fun readHeader(input: ByteReadChannel): RTMPPacketHeader {
    val firstByte = input.readByte().toInt()
    val chunkHeaderType = (firstByte shr 6 and 0b11).toByte()
    val channelId = (firstByte and 0b00111111).toByte()


    if (chunkHeaderType == CHUNCK_HEADER_TYPE_3) {
        return RTMPPacketHeader3(chunkHeaderType, channelId)
    }

    if (chunkHeaderType == CHUNCK_HEADER_TYPE_2) {

        val timeStampArray = ByteArray(TIMESTAMP_SIZE)
        input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

        return RTMPPacketHeader2(
            chunkHeaderType,
            channelId,
            timeStampArray
        )
    }

    if (chunkHeaderType == CHUNCK_HEADER_TYPE_1) {
        val timeStampArray = ByteArray(TIMESTAMP_SIZE)
        input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

        val lengthArray = ByteArray(LENGTH_SIZE)
        input.readFully(lengthArray, 0, LENGTH_SIZE)

        val messageIdType = input.readByte()

        return RTMPPacketHeader1(
            chunkHeaderType,
            channelId,
            timeStampArray,
            lengthArray,
            messageIdType
        )
    }

    if (chunkHeaderType == CHUNCK_HEADER_TYPE_0) {
        val timeStampArray = ByteArray(TIMESTAMP_SIZE)
        input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

        val lengthArray = ByteArray(LENGTH_SIZE)
        input.readFully(lengthArray, 0, LENGTH_SIZE)

        val messageIdType = input.readByte()

        val streamIdArray = ByteArray(MESSAGE_ID_SIZE)
        input.readFully(streamIdArray, 0, MESSAGE_ID_SIZE)

        return RTMPPacketHeader0(
            chunkHeaderType,
            channelId,
            timeStampArray,
            lengthArray,
            messageIdType,
            streamIdArray
        )
    }

    throw Exception("Invalid RTMP Packet Header")
}