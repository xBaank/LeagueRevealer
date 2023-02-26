package rtmpClient.packet

import io.ktor.utils.io.*
import rtmpClient.amf.Amf0Node
import rtmpClient.amf.encoder.Amf0Encoder

class RtmpPacketEncoder(val writeChannel: ByteWriteChannel, val header: RTMPPacketHeader, val payload: List<Amf0Node>) {
    suspend fun encode() {
        writeChannel.writeFully(header.headerData)
        val encoder = Amf0Encoder(writeChannel)
        payload.forEach {
            encoder.write(it)
        }
    }

}