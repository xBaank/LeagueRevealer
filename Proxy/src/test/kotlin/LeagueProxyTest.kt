import kotlinx.coroutines.runBlocking
import leagueProxy.LeagueProxyClient
import org.junit.jupiter.api.Test

class LeagueProxyTest {

    @Test
    fun `should proxy`(): Unit = runBlocking {
        val proxy =
            LeagueProxyClient("127.0.0.1", 8484, "prod.euw1.lol.riotgames.com", 2099)
        proxy.start()
    }
}
