package leagueRtmpClient

import io.ktor.network.sockets.*


class RtmpClient(val socket: Socket) {
    private val readChannel = socket.openReadChannel()
    private val writeChannel = socket.openWriteChannel(autoFlush = true)
    suspend fun handshake() {
        //https://en.wikipedia.org/wiki/Real-Time_Messaging_Protocol#Handshake
        writeChannel.writeByte(C0)

        val c1 = generateC1()
        writeChannel.writeFully(c1, 0, c1.size)

        val s0 = readChannel.readByte()

        if (s0 != C0) throw Exception("Invalid Protocol Version")

        val s1 = ByteArray(S1_SIZE)
        readChannel.readFully(s1, 0, s1.size)

        val s0Echo = generateS1Echo(s1)

        writeChannel.writeFully(s0Echo, 0, s0Echo.size)

        val c1Echo = ByteArray(C1_SIZE)
        readChannel.readFully(c1Echo, 0, c1Echo.size)
    }

    suspend fun readMessage() {
        val decoder = RTMPPacketDecoder(readChannel)
        val header = decoder.readHeader()
        val packet = decoder.readPayload(header)
    }


}