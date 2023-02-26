import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import leagueProxy.LeagueProxyClient
import org.junit.jupiter.api.Test
import rtmpClient.packet.RTMPPacketDecoder
import rtmpClient.packet.RtmpPacketEncoder

class LeagueProxyTest {

    @Test
    fun `should decode`(): Unit = runBlocking {
        val data =
            "0300000000014114000000000200075f726573756c740040220000000000000510002a666c65782e6d6573736167696e672e6d657373616765732e41636b6e6f776c656467654d65737361676500076865616465727303001244534d6573736167696e6756657273696f6e003ff000000000000000044453496402002436384337393533372d383731442d39c34534432d323030422d383644353645393334383144000009000a74696d65546f4c6976650000000000000000000008636c69656e7449640200036e696c000b64657374696e6174696f6e0500096d657373616765496402002436384337393534382d394530432d333946412d454536412d443944323245464134423033000d63c36f7272656c6174696f6e496402000b3739323234303533302d370004626f647902000773756363657373000974696d657374616d7000427868435e57e000000009"
        val dataAsArray = data.decodeHex()
        println(dataAsArray.size)
        val rtmpPacketDecoder = RTMPPacketDecoder(dataAsArray.inputStream().toByteReadChannel())
        val payload = rtmpPacketDecoder.readPayload()

        val byteChannel = ByteChannel(autoFlush = true)


        val rtmpPacketEncoder = RtmpPacketEncoder(byteChannel, rtmpPacketDecoder.header, payload)
        rtmpPacketEncoder.encode()

        byteChannel.close()
        val byteArray: ByteArray = byteChannel.toByteArray()

        assert(byteArray.contentEquals(dataAsArray))
    }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    @Test
    fun `should proxy`(): Unit = runBlocking {
        val proxy =
            LeagueProxyClient("127.0.0.1", 8484, "prod.euw1.lol.riotgames.com", 2099)
        proxy.start()
    }
}
