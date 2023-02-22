package leagueRtmpClient

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import kotlinx.coroutines.Dispatchers

class LeagueClient internal constructor(private val rtmpClient: RtmpClient) {
    suspend fun connect() {
        rtmpClient.handshake()
        rtmpClient.readMessage()
    }

}

suspend fun LeagueClient(host: String, port: Int): LeagueClient {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socket = aSocket(selectorManager).tcp().connect(host, port).tls(Dispatchers.IO)
    val rtmpClient = RtmpClient(socket)
    return LeagueClient(rtmpClient)
}



