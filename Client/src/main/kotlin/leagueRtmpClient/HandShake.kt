package leagueRtmpClient

import java.nio.ByteBuffer
import kotlin.random.Random

internal const val C0: Byte = 0x03
internal const val C1_SIZE = 1536
internal const val S1_SIZE = 1536
internal const val C2_SIZE = 1536
internal const val chunkSize = 128

internal fun generateC1(): ByteArray {
    val c1 = ByteArray(C1_SIZE)
    val timestamp = (System.currentTimeMillis() / 1000).toInt()
    ByteBuffer.wrap(c1, 0, 4).putInt(timestamp)
    ByteBuffer.wrap(c1, 5, 4).put(0)
    Random.nextBytes(c1, 9)
    return c1
}

internal fun generateS1Echo(s1: ByteArray): ByteArray {
    val timestamp = (System.currentTimeMillis() / 1000).toInt()
    ByteBuffer.wrap(s1, 5, 4).putInt(timestamp)
    return s1
}