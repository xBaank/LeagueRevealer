package rtmpClient.packet

import io.ktor.utils.io.*
import rtmpClient.amf.Amf0Node
import rtmpClient.amf.decoder.AMF0Decoder
import java.io.DataInputStream

internal const val CHUNCK_HEADER_TYPE_0: Byte = 0x00
internal const val CHUNCK_HEADER_TYPE_1: Byte = 0x01
internal const val CHUNCK_HEADER_TYPE_2: Byte = 0x02
internal const val CHUNCK_HEADER_TYPE_3: Byte = 0x03
internal const val CHUNK_SIZE = 128


internal const val TIMESTAMP_SIZE = 3
internal const val LENGTH_SIZE = 3
internal const val MESSAGE_ID_SIZE = 4

class RTMPPacketDecoder internal constructor(
    val payloadData: ByteArray,
    val originalPayloadData: ByteArray,
    val header: RTMPPacketHeader
) {
    private val input = DataInputStream(payloadData.inputStream())


    val data = header.headerData + payloadData
    val originalData = header.headerData + originalPayloadData


    fun readPayload(): List<Amf0Node> {
        when (header) {
            is RTMPPacketHeader0 -> {
                if (header.messageTypeId.toInt() != 0x14) return listOf()
                return AMF0Decoder(payloadData).decodeAll()
            }

            is RTMPPacketHeader1 -> {
                if (header.messageTypeId.toInt() != 0x14) return listOf()
                return AMF0Decoder(payloadData).decodeAll()
            }

            is RTMPPacketHeader2 -> {
                return listOf()
            }

            is RTMPPacketHeader3 -> {
                return listOf()
            }
        }
    }
}

suspend fun ByteReadChannel.readChunkedPayload(length: Int): Pair<ByteArray, ByteArray> {
    var data = ByteArray(0)
    var originalData = ByteArray(0)
    val chunks = length / CHUNK_SIZE
    val remainder = length % CHUNK_SIZE

    for (i in 0 until chunks) {
        val chunk = ByteArray(CHUNK_SIZE)
        if (i != 0) readByte()
        readFully(chunk, 0, CHUNK_SIZE)

        data += chunk
        originalData += if (i == 0) chunk else byteArrayOf(0xC3.toByte()) + chunk
    }

    if (remainder > 0) {
        val chunk = ByteArray(remainder)
        if (chunks != 0) readByte()
        readFully(chunk, 0, remainder)
        data += chunk
        originalData += if (chunks == 0) chunk else byteArrayOf(0xC3.toByte()) + chunk
    }
    return data to originalData
}

suspend fun ByteReadChannel.readAllPayload(offset: Int, length: Int): Pair<ByteArray, ByteArray> {
    val data = ByteArray(length)
    readFully(data, offset, length)
    return data to data
}

suspend fun RTMPPacketDecoder(channel: ByteReadChannel): RTMPPacketDecoder {
    val header = readHeader(channel)
    val payloadData =
        when (header) {
            is RTMPPacketHeader0 -> {
                if (header.messageTypeId == 0x01.toByte())
                    println("Set chunk size")
                if (header.messageTypeId == 0x14.toByte())
                    channel.readChunkedPayload(header.lengthAsInt)
                else
                    channel.readAllPayload(0, header.lengthAsInt)
            }

            is RTMPPacketHeader1 -> {
                if (header.messageTypeId == 0x01.toByte())
                    println("Set chunk size2 ")

                if (header.messageTypeId == 0x14.toByte())
                    channel.readChunkedPayload(header.lengthAsInt)
                else
                    channel.readAllPayload(0, header.lengthAsInt)
            }

            else -> channel.readAllPayload(0, header.lengthAsInt)
        }

    return RTMPPacketDecoder(payloadData.first, payloadData.second, header)
}

private suspend fun readHeader(input: ByteReadChannel): RTMPPacketHeader {
    val firstByte = input.readByte().toInt()
    val chunkHeaderType = (firstByte shr 6 and 0b11).toByte()
    val channelId = (firstByte and 0b00111111).toByte()

    when (chunkHeaderType) {
        CHUNCK_HEADER_TYPE_0 -> {
            val timeStampArray = ByteArray(TIMESTAMP_SIZE)
            input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

            val lengthArray = ByteArray(LENGTH_SIZE)
            input.readFully(lengthArray, 0, LENGTH_SIZE)

            val messageIdType = input.readByte()

            val streamIdArray = ByteArray(MESSAGE_ID_SIZE)
            input.readFully(streamIdArray, 0, MESSAGE_ID_SIZE)

            val data =
                byteArrayOf(firstByte.toByte()) + timeStampArray + lengthArray + byteArrayOf(messageIdType) + streamIdArray

            return RTMPPacketHeader0(
                chunkHeaderType,
                channelId,
                timeStampArray,
                lengthArray,
                messageIdType,
                streamIdArray,
                data
            )
        }

        CHUNCK_HEADER_TYPE_1 -> {
            val timeStampArray = ByteArray(TIMESTAMP_SIZE)
            input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

            val lengthArray = ByteArray(LENGTH_SIZE)
            input.readFully(lengthArray, 0, LENGTH_SIZE)

            val messageIdType = input.readByte()

            val data = byteArrayOf(firstByte.toByte()) + timeStampArray + lengthArray + byteArrayOf(messageIdType)

            return RTMPPacketHeader1(
                chunkHeaderType,
                channelId,
                timeStampArray,
                lengthArray,
                messageIdType,
                data
            )
        }

        CHUNCK_HEADER_TYPE_2 -> {
            val timeStampArray = ByteArray(TIMESTAMP_SIZE)
            input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

            val data = byteArrayOf(firstByte.toByte()) + timeStampArray

            return RTMPPacketHeader2(
                chunkHeaderType,
                channelId,
                timeStampArray,
                data
            )
        }

        CHUNCK_HEADER_TYPE_3 -> {
            val data = byteArrayOf(firstByte.toByte())
            return RTMPPacketHeader3(chunkHeaderType, channelId, data)
        }

        else -> throw Exception("Unknown chunk header type")
    }
}