package leagueRtmpClient

import io.ktor.utils.io.*


class RTMPPacketDecoder(private val input: ByteReadChannel) {

    suspend fun readHeader(): RTMPPacketHeader {
        val firstByte = readByte().toInt()
        println(firstByte)

        if (firstByte == 3) {
            val timeStamp = readInt24()
            println(timeStamp)
            return RTMPPacketHeader(
                firstByte, // formatream id
                0, 0, 0, 0
            )
        }

        if (firstByte == 2) {
            val timeStamp = readInt24()
            println(timeStamp)
            return RTMPPacketHeader(
                firstByte, // format
                timeStamp, // channel id
                0, 0, 0// stream id
            )
        }

        if (firstByte == 1) {
            val timeStamp = readInt24()
            println(timeStamp)
            return RTMPPacketHeader(
                firstByte, // format
                timeStamp, // channel id
                readInt24(), // length
                0, 0
            )
        }

        if (firstByte == 0) {
            val timeStamp = readInt24()
            println(timeStamp)
            return RTMPPacketHeader(
                firstByte, // format
                timeStamp, // channel id
                readInt24(), // length
                readByte().toInt(), // message type id
                readInt32() // stream id
            )
        }

        throw Exception("Invalid RTMP Packet Header")
    }

    suspend fun readPayload(header: RTMPPacketHeader): ByteArray {
        val payload = ByteArray(header.length)
        input.readFully(payload)
        return payload
    }

    private suspend fun readByte(): Byte = input.readByte()

    private suspend fun readInt24(): Int {
        val byteArray = ByteArray(3)
        input.readFully(byteArray)
        return byteArray[0].toInt() shl 16 or (byteArray[1].toInt() shl 8) or byteArray[2].toInt()
    }

    private suspend fun readInt32(): Int = input.readInt()
}