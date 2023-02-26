package leagueProxy

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rtmpClient.packet.RTMPPacketDecoder
import rtmpClient.packet.RtmpPacketEncoder
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds


val mutex = Mutex()

class LeagueProxyClient internal constructor(private val serverSocket: ServerSocket, private val clientSocket: Socket) {
    private val writer = run {
        val file = File("C:\\Users\\Roberto\\Documents\\logs_rtmp\\log-${UUID.randomUUID()}.txt")
        file.delete()
        file.writer()
    }

    suspend fun start() = coroutineScope {
        while (isActive) {
            val socket = serverSocket.accept()
            println("Accepted connection from ${socket.remoteAddress}")
            launch(Dispatchers.IO) { handleSocket(socket) }
        }
    }

    private suspend fun handleSocket(socket: Socket) = coroutineScope {
        val serverReadChannel = socket.openReadChannel()
        val serverWriteChannel = socket.openWriteChannel(autoFlush = true)
        val clientReadChannel = clientSocket.openReadChannel()
        val clientWriteChannel = clientSocket.openWriteChannel(autoFlush = true)

        var isConnected = true

        launch {
            delay(10.seconds)
            isConnected = true
        }

        //read handshakes
        handshake(serverReadChannel, clientWriteChannel, clientReadChannel, serverWriteChannel)

        //This is lol client messages
        launch(Dispatchers.IO) {
            while (isActive) {
                val byteArray = ByteArray(10_000)
                val bytes = serverReadChannel.readAvailable(byteArray)



                if (bytes == -1) {
                    println("Server closed connection")
                    socket.close()
                    return@launch
                }

                mutex.withLock {
                    writer.write("Lol client : ")
                    writer.write(byteArray.copyOfRange(0, bytes).toHexString())
                    writer.write("\n")
                    writer.write("\n")
                    writer.write("Decoded : ")
                    writer.write(byteArray.copyOfRange(0, bytes).decodeToString())
                    writer.write("\n")
                    writer.write("\n")
                    writer.flush()
                }

                println("Received from lol client $bytes bytes")
                clientWriteChannel.writeFully(byteArray, 0, bytes)
            }
        }

        //This is backend messages
        launch(Dispatchers.IO) {
            while (isActive) {

                val bytes: Int
                val byteArray: ByteArray


                val rtmpPacketDecoder = RTMPPacketDecoder(clientReadChannel)

                bytes = rtmpPacketDecoder.originalData.size
                byteArray = rtmpPacketDecoder.originalData

                val node = rtmpPacketDecoder.readPayload()

                if (bytes == -1) {
                    println("Client closed connection")
                    socket.close()
                    return@launch
                }

                mutex.withLock {
                    writer.write("Backend : ")
                    writer.write(byteArray.copyOfRange(0, bytes).toHexString())
                    writer.write("\n")
                    writer.write("\n")
                    writer.write("Decoded : ")
                    writer.write(byteArray.copyOfRange(0, bytes).decodeToString())
                    writer.write("\n")
                    writer.write("\n")
                    writer.write("Node : ")
                    writer.write(node.toString())
                    writer.write("\n")
                    writer.write("\n")
                    writer.flush()
                }

                println("Received from server $bytes bytes")
                if (node.isNotEmpty()) {
                    RtmpPacketEncoder(serverWriteChannel, rtmpPacketDecoder.header, node).encode()
                    /*                    //get summoner data
                                        val node = listOf(
                                            Amf0Null,
                                            Amf0Null,
                                            Amf0Null,
                                            Amf0TypedObject(
                                                "flex.messaging.messages.RemotingMessage", mapOf(
                                                    "operation" to "getAllPublicSummonerDataByAccount".toAmf0String(),
                                                    "source" to Amf0Null,
                                                    "messageId" to UUID.randomUUID().toString().toAmf0String(),
                                                    "timestamp" to 0.0.toAmf0Number(),
                                                    "timeToLive" to 0.0.toAmf0Number(),
                                                    "clientId" to UUID.randomUUID().toString().toAmf0String(),
                                                    "destination" to "summonerService".toAmf0String(),
                                                    "body" to listOf("19376420".toAmf0String()).toAmf0StrictArray(),
                                                    "headers" to mapOf<String, Amf0Node>().toAmf0Object()
                                                )
                                            )
                                        )
                                        val channelMemory = ByteChannel(autoFlush = true)
                                        val encoder = Amf0Encoder(channelMemory)
                                        node.forEach { encoder.write(it) }
                                        val bytes = channelMemory.availableForRead
                                        val byteArray = ByteArray(3)
                                        ByteBuffer.wrap(byteArray).putShort(1, bytes.toShort())
                                        val encode = channelMemory.readAvailable(ByteArray(bytes))

                                        val rtmpPacketHeader0 = RTMPPacketHeader0(
                                            20,
                                            0,
                                            byteArrayOf(0, 0, 0),
                                            byteArray,
                                            0,
                                            byteArrayOf(0, 0, 0, 0),
                                            byteArrayOf(20, 0) + byteArray + byteArrayOf(0, 0, 0) + byteArrayOf(0, 0, 0, 0)
                                        )
                                        channelMemory.close()

                                        serverWriteChannel.writeFully(rtmpPacketHeader0.headerData + channelMemory.toByteArray())*/


                } else
                    serverWriteChannel.writeFully(byteArray, 0, bytes)
            }
        }
    }

    private suspend fun handshake(
        serverReadChannel: ByteReadChannel,
        clientWriteChannel: ByteWriteChannel,
        clientReadChannel: ByteReadChannel,
        serverWriteChannel: ByteWriteChannel
    ) {
        val c0 = serverReadChannel.readByte()
        clientWriteChannel.writeByte(c0)
        val c1 = ByteArray(1536)
        serverReadChannel.readFully(c1, 0, c1.size)
        clientWriteChannel.writeFully(c1, 0, c1.size)

        val s0 = clientReadChannel.readByte()
        serverWriteChannel.writeByte(s0)
        val s1 = ByteArray(1536)
        clientReadChannel.readFully(s1, 0, s1.size)
        serverWriteChannel.writeFully(s1, 0, s1.size)

        if (s0 != c0) throw IllegalStateException("c0 and s0 are not equal")

        val s0Echo = ByteArray(1536)
        clientReadChannel.readFully(s0Echo, 0, s0Echo.size)
        serverWriteChannel.writeFully(s0Echo, 0, s0Echo.size)

        val c1Echo = ByteArray(1536)
        serverReadChannel.readFully(c1Echo, 0, c1Echo.size)
        clientWriteChannel.writeFully(c1Echo, 0, c1Echo.size)
    }
}

suspend fun LeagueProxyClient(host: String, port: Int, proxyHost: String, proxyPort: Int): LeagueProxyClient {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socketServer = aSocket(selectorManager).tcp().bind(host, port)
    val socketClient = aSocket(selectorManager).tcp().connect(proxyHost, proxyPort).tls(Dispatchers.IO)

    return LeagueProxyClient(socketServer, socketClient)
}


fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

