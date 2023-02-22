package LeagueRtmpClient

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers

class LeagueClient internal constructor(private val rtmpClient: RtmpClient) {

}

suspend fun LeagueClient(host: String, port: Int): LeagueClient {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socket = aSocket(selectorManager).tcp().connect(host, port)
    val rtmpClient = RtmpClient(socket)
    return LeagueClient(rtmpClient)
}



