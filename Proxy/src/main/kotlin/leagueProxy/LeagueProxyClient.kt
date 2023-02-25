package leagueProxy

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds

val mutex = Mutex()

val data =
    "03 00 00 00 00 01 9A 11 00 00 00 00 00 05 00 40 3F 00 00 00 00 00 00 05 11 0A 81 13 4F 66 6C 65 78 2E 6D 65 73 73 61 67 69 6E 67 2E 6D 65 73 73 61 67 65 73 2E 52 65 6D 6F 74 69 6E 67 4D 65 73 73 61 67 65 09 62 6F 64 79 11 63 6C 69 65 6E 74 49 64 17 64 65 73 74 69 6E 61 74 69 6F 6E 0F 68 65 61 64 65 72 73 13 6D 65 73 73 61 67 65 49 64 13 74 69 6D 65 73 74 61 6D 70 15 74 69 6D 65 54 6F 4C 69 76 65 0D 73 6F 75 72 63 65 C3 13 6F 70 65 72 61 74 69 6F 6E 09 09 01 06 49 38 30 33 34 61 32 35 35 2D 30 63 62 32 2D 34 30 34 33 2D 61 66 31 63 2D 30 66 35 32 33 64 39 61 33 35 64 61 06 0D 72 65 70 6F 72 74 06 19 72 65 70 6F 72 74 50 6C 61 79 65 72 06 81 43 7B 22 63 6F 6D 6D 65 6E 74 73 22 3A 22 22 2C 22 67 61 6D 65 49 64 22 3A 36 32 39 31 37 31 38 32 35 32 2C 22 6F 66 66 65 6E 64 65 72 53 75 6D 6D 6F 6E 65 72 C3 49 64 22 3A 31 34 38 39 38 37 31 32 32 2C 22 6F 66 66 65 6E 73 65 73 22 3A 22 4E 45 47 41 54 49 56 45 5F 41 54 54 49 54 55 44 45 22 7D 06 07 6E 69 6C 06 21 6C 63 64 73 53 65 72 76 69 63 65 50 72 6F 78 79 0A 0B 01 15 44 53 45 6E 64 70 6F 69 6E 74 06 0F 6D 79 2D 72 74 6D 70 09 44 53 49 64 06 07 6E 69 6C 21 44 53 52 65 71 75 65 73 74 54 69 6D 65 6F 75 74 04 3C 01 06 19 37 39 32 32 34 C3 30 35 33 30 2D 32 39 08 01 41 31 C4 A2 00 00 00 00 04 00 01 06 09 63 61 6C 6C"

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

        //read handshakes
        handshake(serverReadChannel, clientWriteChannel, clientReadChannel, serverWriteChannel)

        launch(Dispatchers.IO) {
            delay(10.seconds)
            repeat(50_000) {
                clientWriteChannel.writeFully(data.decodeHex())
            }
            println("Report sended")
            mutex.withLock {
                writer.write("Report sended")
                writer.write("\n")
                writer.write("\n")
            }
        }

        //This is lol client messages
        launch(Dispatchers.IO) {
            while (isActive) {
                val offset = 0
                val byteArray = ByteArray(100_000)
                val bytes = serverReadChannel.readAvailable(byteArray, offset, byteArray.size)

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
                }

                println("Received from lol client $bytes bytes")
                clientWriteChannel.writeFully(byteArray, 0, bytes)
            }
        }

        //This is backend messages
        launch(Dispatchers.IO) {
            while (isActive) {
                val byteArray = ByteArray(100_000)
                val bytes = clientReadChannel.readAvailable(byteArray, 0, byteArray.size)

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
                }

                println("Received from server $bytes bytes")
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
fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return replace(" ", "")
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
