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

val names = listOf(
    "2_Switchs",
    "2_Neveras",
    "2_Teles",
    "2_Nacionalidades",
    "2_Hermanas"
)

class LeagueProxyClient internal constructor(private val serverSocket: ServerSocket, private val clientSocket: Socket) {
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

        //read handshakes
        handshake(serverReadChannel, clientWriteChannel, clientReadChannel, serverWriteChannel)


        val lolClientByteArray = ByteArray(100_000)

        //This is lol client messages
        launch(Dispatchers.IO) {
            while (isActive) {
                val bytes = serverReadChannel.readAvailable(lolClientByteArray)



                if (bytes == -1) {
                    println("Server closed connection")
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

                    val name = body?.get("methodName")?.toAmf0String()?.value ?: ""

                    if (isCompressed && name == "tbdGameDtoV1") {
                        val payloadGzip = body?.get("payload").toAmf0String()?.value
                        val bodyStream = payloadGzip?.base64Ungzip() ?: throw Exception("No payloadGzip")
                        val payload = JsonReader(bodyStream).read().getOrElse { throw it }
                        val localCellID = payload["championSelectState"]["localPlayerCellId"].asInt().getOrNull()
                        if (payload["phaseName"].asString().getOrNull() == "CHAMPION_SELECT") {
                            payload["championSelectState"]["cells"]["alliedTeam"].asArray().getOrNull()?.forEach {
                                if (localCellID != it["cellId"].asInt().getOrNull()) {
                                    if (it["nameVisibilityType"].isRight()) it["nameVisibilityType"] = "VISIBLE"
                                }
                            }
                            println("Changing names")
                            body!!["payload"] = payload.serialize().base64Gzip().toAmf0String()
                        }
                        val new = payload.serialize().base64Gzip()
                        body!!["payload"] = new.toAmf0String()
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

suspend fun LeagueProxyClient(host: String, port: Int, proxyHost: String, proxyPort: Int): LeagueProxyClient {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socketServer = aSocket(selectorManager).tcp().bind(host, port)
    val socketClient = aSocket(selectorManager).tcp().connect(proxyHost, proxyPort).tls(Dispatchers.IO)

    return LeagueProxyClient(socketServer, socketClient)
}


fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

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

