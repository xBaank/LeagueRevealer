package rtmpClient

import io.ktor.network.sockets.*
import rtmpClient.packet.header.RTMPPacketDecoder
import java.nio.ByteBuffer
import kotlin.random.Random

internal const val C0: Byte = 0x03
internal const val C1_SIZE = 1536
internal const val S1_SIZE = 1536

class RtmpClient(socket: Socket) {
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

        //read first 4 bytes of c1 echo
        val c1EchoTimestamp = ByteBuffer.wrap(c1Echo, 0, 4).int
        val current = System.currentTimeMillis() / 1000
        //Allow 1 sec of difference
        if (current - c1EchoTimestamp > 1000) throw Exception("Invalid Timestamp")
    }

    suspend fun readMessage() {
        val decoder = RTMPPacketDecoder(readChannel)
        val packet = decoder.readPayload()
    }


    private fun generateC1(): ByteArray {
        val c1 = ByteArray(C1_SIZE)
        val timestamp = (System.currentTimeMillis() / 1000).toInt()
        ByteBuffer.wrap(c1, 0, 4).putInt(timestamp)
        ByteBuffer.wrap(c1, 5, 4).put(0)
        Random.nextBytes(c1, 9)
        return c1
    }

    private fun generateS1Echo(s1: ByteArray): ByteArray {
        val timestamp = (System.currentTimeMillis() / 1000).toInt()
        ByteBuffer.wrap(s1, 5, 4).putInt(timestamp)
        return s1
    }

}