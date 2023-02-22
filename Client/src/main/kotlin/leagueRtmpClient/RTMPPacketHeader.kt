package leagueRtmpClient

class RTMPPacketHeader(
    val format: Int,
    val timeStamp: Int,
    val length: Int,
    val messageTypeId: Int,
    val streamId: Int
)
