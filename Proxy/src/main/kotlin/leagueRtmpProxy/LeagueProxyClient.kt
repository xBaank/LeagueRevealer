package leagueRtmpProxy

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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


        launch(Dispatchers.IO) {
            while (isActive) {
                val byteArray = ByteArray(10000)
                val bytes = serverReadChannel.readAvailable(byteArray, 0, byteArray.size)
                if (bytes == -1) {
                    println("Server closed connection")
                    socket.close()
                    return@launch
                }
                println(byteArray.decodeToString(0, bytes))
                println("Received from lol client $bytes bytes")
                clientWriteChannel.writeFully(byteArray, 0, bytes)
            }
        }

        launch(Dispatchers.IO) {
            while (isActive) {
                val byteArray = ByteArray(10000)
                val bytes = clientReadChannel.readAvailable(byteArray, 0, byteArray.size)

                if (bytes == -1) {
                    println("Client closed connection")
                    socket.close()
                    return@launch
                }
                println(byteArray.decodeToString(0, bytes))
                println("Received from server $bytes bytes")
                serverWriteChannel.writeFully(byteArray, 0, bytes)
            }
        }
    }
}

suspend fun LeagueProxyClient(host: String, port: Int, proxyHost: String, proxyPort: Int): LeagueProxyClient {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socketServer = aSocket(selectorManager).tcp().bind(host, port)
    val socketClient = aSocket(selectorManager).tcp().connect(proxyHost, proxyPort).tls(Dispatchers.IO)

    return LeagueProxyClient(socketServer, socketClient)
}

