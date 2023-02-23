package leagueRtmpClient.packet

sealed interface RTMPPacketHeader
class RTMPPacketHeader0(
    val format: Byte,
    val timeStamp: ByteArray,
    val length: ByteArray,
    val messageTypeId: Byte,
    val streamId: ByteArray
) : RTMPPacketHeader

class RTMPPacketHeader1(
    val format: Byte,
    val timeStamp: ByteArray,
    val length: ByteArray,
    val messageTypeId: Byte
) : RTMPPacketHeader

class RTMPPacketHeader2(
    val format: Byte,
    val timeStamp: ByteArray
) : RTMPPacketHeader

class RTMPPacketHeader3(
    val format: Byte
) : RTMPPacketHeader