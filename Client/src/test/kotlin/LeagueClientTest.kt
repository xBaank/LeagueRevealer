import LeagueRtmpClient.LeagueClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class LeagueClientTest {
    @Test
    fun `should connect to the league client`(): Unit = runBlocking {
        val leagueClient = LeagueClient("prod.euw1.lol.riotgames.com", 2099)

    }
}