package rtmpClient.packet

import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rtmpClient.amf.Amf0Node
import rtmpClient.amf.encoder.Amf0Encoder

class RtmpPacketEncoder(val writeChannel: ByteWriteChannel, val header: RTMPPacketHeader) {
    suspend fun encode(payload: List<Amf0Node>) {
        writeChannel.writeFully(header.headerData)
        val encoder = Amf0Encoder(writeChannel)
        payload.forEach {
            encoder.write(it)
        }
    }

    suspend fun spoof(payload: List<Amf0Node>, original: ByteArray) = coroutineScope {
        val memoChannel = ByteChannel()
        val encoder = Amf0Encoder(memoChannel)

        val buffer = ByteArray(4096 * 4)
        var data = ByteArray(0)

        val job = launch(Dispatchers.IO) {
            while (isActive) {
                val read = memoChannel.readAvailable(buffer)
                data += buffer.sliceArray(0 until read)
                if (read == -1) {
                    break
                }
            }
        }

        val job2 = launch(Dispatchers.IO) {
            payload.forEach {
                encoder.write(it)
            }
            memoChannel.close()
        }

        job2.join()
        job.join()

        val chunkSize = original.size / (original.size - header.lengthAsInt)

        val newHeader = when (header) {
            is RTMPPacketHeader0 -> spoofHeader(data, header, chunkSize)
            is RTMPPacketHeader1 -> TODO()
            is RTMPPacketHeader2 -> TODO()
            is RTMPPacketHeader3 -> TODO()
        }




        writeChannel.writeFully(newHeader.headerData)
        data.forEach {
            writeChannel.writeByte(it)
        }
    }

    private fun spoofHeader(data: ByteArray, original: RTMPPacketHeader0, chunkSize: Int): RTMPPacketHeader0 {
        val lengthArray = ByteArray(3)
        val readlDataSize = (data.size - (data.size / chunkSize)) * 256
        val b3 = (readlDataSize shr 8 and 0xFF).toByte()
        val b2 = (readlDataSize shr 16 and 0xFF).toByte()
        val b1 = (readlDataSize shr 24 and 0xFF).toByte()
        //ByteBuffer.wrap(lengthArray).putShort(1, readlDataSize.toShort())
        lengthArray[0] = b1
        lengthArray[1] = b2
        lengthArray[2] = b3
        val rtmpPacketHeader0 = RTMPPacketHeader0(
            chunkHeaderType = original.chunkHeaderType,
            channelId = original.channelId,
            timeStamp = original.timeStamp,
            length = lengthArray,
            messageTypeId = original.messageTypeId,
            streamId = original.streamId,
            headerData = byteArrayOf(original.headerData.first()) + original.headerData.sliceArray(1..3)
                    + lengthArray + original.headerData.sliceArray(7..11)
        )
        return rtmpPacketHeader0
    }

}