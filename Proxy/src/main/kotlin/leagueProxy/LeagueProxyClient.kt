package leagueProxy

import arrow.core.getOrElse
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rtmpClient.amf.get
import rtmpClient.amf.set
import rtmpClient.amf.toAmf0Boolean
import rtmpClient.amf.toAmf0String
import rtmpClient.packet.RTMPPacketDecoder
import rtmpClient.packet.RtmpPacketEncoder
import simpleJson.*
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

private const val SOLOQ_ID = 420

class LeagueProxyClient internal constructor(
    val serverSocket: ServerSocket,
    private val host: String,
    private val port: Int
) {
    suspend fun start() = coroutineScope {
        while (isActive) {
            val socket = serverSocket.accept()
            println("Accepted connection from ${socket.remoteAddress}")
            launch(Dispatchers.IO) {
                runCatching {
                    handleSocket(socket)
                }.onFailure {
                    println("Error handling socket: ${socket.remoteAddress}")
                }
            }
        }
    }

    private suspend fun handleSocket(socket: Socket) = coroutineScope {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val clientSocket = aSocket(selectorManager).tcp().connect(host, port).tls(Dispatchers.IO)

        val serverReadChannel = socket.openReadChannel()
        val serverWriteChannel = socket.openWriteChannel(autoFlush = true)
        val clientReadChannel = clientSocket.openReadChannel()
        val clientWriteChannel = clientSocket.openWriteChannel(autoFlush = true)

        //read handshakes
        handshake(serverReadChannel, clientWriteChannel, clientReadChannel, serverWriteChannel)


        val lolClientByteArray = ByteArray(100_000)

        //This is lol client messages
        launch(Dispatchers.IO) {
            while (isActive) {
                val bytes = serverReadChannel.readAvailable(lolClientByteArray)

                if (bytes == -1) {
                    println("Socket ${socket.remoteAddress} closed connection")
                    socket.close()
                    return@launch
                }

                clientWriteChannel.writeFully(lolClientByteArray, 0, bytes)
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


                val node = try {
                    rtmpPacketDecoder.readPayload()
                } catch (e: Exception) {
                    listOf()
                }

                if (node.isNotEmpty()) {
                    val body = node.getOrNull(3)?.get("body")
                    val isCompressed = body?.get("compressedPayload")?.toAmf0Boolean()?.value ?: false

                    println(node)

                    if (isCompressed) {
                        val payloadGzip = body?.get("payload").toAmf0String()?.value
                        val bodyStream = payloadGzip?.base64Ungzip() ?: throw Exception("No payloadGzip")
                        val payload = JsonReader(bodyStream).read().getOrElse { throw it }

                        val queueId = payload["queueId"].asInt().getOrNull()
                        //val phaseName = payload["phaseName"].asString().getOrNull() Better to remove, just check for queueID, But its should be CHAMPION_SELECT
                        //val subPhase = payload["championSelectState"]["subphase"].asString().getOrNull() Better to remove, just check for queueID, But it should be PLANNING

                        println(payload.serialize())

                        if (queueId != null) {
                            val localCellID = payload["championSelectState"]["localPlayerCellId"].asInt().getOrNull()

                            payload["championSelectState"]["cells"]["alliedTeam"].asArray().getOrNull()?.forEach {
                                if (localCellID != it["cellId"].asInt().getOrNull()) {
                                    if (it["nameVisibilityType"].isRight()) it["nameVisibilityType"] = "UNHIDDEN"
                                }
                            }
                            body!!["payload"] = payload.serialize().base64Gzip().toAmf0String()

                            println("Spoofing packet")
                        }
                    }
                    RtmpPacketEncoder(serverWriteChannel, rtmpPacketDecoder.header).spoof(
                        node,
                        rtmpPacketDecoder.originalPayloadData
                    )
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

fun LeagueProxyClient(proxyPort: Int, host: String, port: Int): LeagueProxyClient {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socketServer = aSocket(selectorManager).tcp().bind(port = proxyPort)

    return LeagueProxyClient(socketServer, host, port)
}


fun String.base64Ungzip(): String {
    val gzipped: ByteArray = Base64.getDecoder().decode(this.toByteArray(UTF_8))
    val `in` = GZIPInputStream(gzipped.inputStream())
    return `in`.bufferedReader(UTF_8).use { it.readText() }
}

fun String.base64Gzip(): String {
    val output = ByteArrayOutputStream()

    GZIPOutputStream(output).use {
        it.write(this.toByteArray(UTF_8))
        it.flush()
    }

    val encoded = Base64.getEncoder().encode(output.toByteArray())
    return String(encoded, UTF_8)
}

