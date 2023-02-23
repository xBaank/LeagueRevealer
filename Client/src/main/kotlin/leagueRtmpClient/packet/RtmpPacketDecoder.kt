package leagueRtmpClient.packet

import io.ktor.utils.io.*
import java.nio.ByteBuffer.wrap

const val CHUNCK_HEADER_TYPE_0: Byte = 0x00
const val CHUNCK_HEADER_TYPE_1: Byte = 0x01
const val CHUNCK_HEADER_TYPE_2: Byte = 0x02
const val CHUNCK_HEADER_TYPE_3: Byte = 0x03

const val TIMESTAMP_SIZE = 3
const val LENGTH_SIZE = 3
const val MESSAGE_ID_SIZE = 4

class RTMPPacketDecoder(private val input: ByteReadChannel) {

    suspend fun readHeader(): RTMPPacketHeader {
        val firstByte = input.readByte()

        if (firstByte == CHUNCK_HEADER_TYPE_3) {
            return RTMPPacketHeader3(firstByte)
        }

        if (firstByte == CHUNCK_HEADER_TYPE_2) {

            val timeStampArray = ByteArray(TIMESTAMP_SIZE)
            input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

            return RTMPPacketHeader2(
                firstByte,
                timeStampArray
            )
        }

        if (firstByte == CHUNCK_HEADER_TYPE_1) {
            val timeStampArray = ByteArray(TIMESTAMP_SIZE)
            input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

            val lengthArray = ByteArray(LENGTH_SIZE)
            input.readFully(lengthArray, 0, LENGTH_SIZE)

            val messageIdType = input.readByte()

            return RTMPPacketHeader1(
                firstByte,
                timeStampArray,
                lengthArray,
                messageIdType
            )
        }

        if (firstByte == CHUNCK_HEADER_TYPE_0) {
            val timeStampArray = ByteArray(TIMESTAMP_SIZE)
            input.readFully(timeStampArray, 0, TIMESTAMP_SIZE)

            val lengthArray = ByteArray(LENGTH_SIZE)
            input.readFully(lengthArray, 0, LENGTH_SIZE)

            val messageIdType = input.readByte()

            val streamIdArray = ByteArray(MESSAGE_ID_SIZE)
            input.readFully(streamIdArray, 0, MESSAGE_ID_SIZE)

            return RTMPPacketHeader0(
                firstByte,
                timeStampArray,
                lengthArray,
                messageIdType,
                streamIdArray
            )
        }

        throw Exception("Invalid RTMP Packet Header")
    }

    suspend fun readPayload(header: RTMPPacketHeader): ByteArray = when (header) {
        is RTMPPacketHeader0 -> {
            val length = wrap(header.length).int
            val payload = ByteArray(length)
            input.readFully(payload, 0, payload.size)
            payload
        }

        is RTMPPacketHeader1 -> {
            val length = wrap(header.length).int
            val payload = ByteArray(length)
            input.readFully(payload, 0, payload.size)
            payload
        }

        is RTMPPacketHeader2 -> {
            TODO()
        }

        is RTMPPacketHeader3 -> {
            TODO()
        }
    }
}