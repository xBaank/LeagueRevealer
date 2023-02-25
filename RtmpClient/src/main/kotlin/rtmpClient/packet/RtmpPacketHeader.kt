package rtmpClient.packet

import java.nio.ByteBuffer

sealed interface RTMPPacketHeader {
    /**
     * Data that forms the header
     */
    val headerData: ByteArray
}

class RTMPPacketHeader0(
    val chunkHeaderType: Byte,
    val channelId: Byte,
    val timeStamp: ByteArray,
    val length: ByteArray,
    val messageTypeId: Byte,
    val streamId: ByteArray,
    override val headerData: ByteArray
) : RTMPPacketHeader

class RTMPPacketHeader1(
    val chunkHeaderType: Byte,
    val channelId: Byte,
    val timeStamp: ByteArray,
    val length: ByteArray,
    val messageTypeId: Byte,
    override val headerData: ByteArray
) : RTMPPacketHeader

class RTMPPacketHeader2(
    val chunkHeaderType: Byte,
    val channelId: Byte,
    val timeStamp: ByteArray,
    override val headerData: ByteArray
) : RTMPPacketHeader

class RTMPPacketHeader3(
    val chunkHeaderType: Byte,
    val channelId: Byte,
    override val headerData: ByteArray,
) : RTMPPacketHeader

val RTMPPacketHeader.lengthAsInt: Int
    get() {
        return when (this) {
            is RTMPPacketHeader0 -> {
                val lengthArray = ByteArray(4)
                length.copyInto(lengthArray, 1, 0, 3)
                ByteBuffer.wrap(lengthArray).int
            }

            is RTMPPacketHeader1 -> {
                val lengthArray = ByteArray(4)
                length.copyInto(lengthArray, 1, 0, 3)
                ByteBuffer.wrap(lengthArray).int
            }

            is RTMPPacketHeader2 -> 0
            is RTMPPacketHeader3 -> 0
        }
    }