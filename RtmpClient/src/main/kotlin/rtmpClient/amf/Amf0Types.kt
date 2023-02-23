package rtmpClient.amf

import rtmpClient.amf.Amf0Node.*

const val AMF0_START_MARKER = 0x03
const val AMF0_END_MARKER = 0x09


sealed interface Amf0Node {
    @JvmInline
    value class Amf0Number(val value: Double) : Amf0Node {
        companion object {
            val type = 0x00
        }
    }

    @JvmInline
    value class Amf0Boolean(val value: Boolean) : Amf0Node {
        companion object {
            val type = 0x01
        }
    }

    @JvmInline
    value class Amf0String(val value: String) : Amf0Node {
        companion object {
            val type = 0x02
        }
    }

    @JvmInline
    value class Amf0Object(val value: Map<String, Amf0Node>) : Amf0Node {
        companion object {
            val type = 0x03
        }
    }

    object Amf0Null : Amf0Node {
        val type = 0x05
    }

    @JvmInline
    value class Amf0ECMAArray(val value: Map<String, Amf0Node>) : Amf0Node {
        companion object {
            val type = 0x08
        }
    }
}


//extension functions for types
fun Number.toAmf0Number(): Amf0Number = Amf0Number(this.toDouble())
fun Boolean.toAmf0Boolean(): Amf0Boolean = Amf0Boolean(this)
fun String.toAmf0String(): Amf0String = Amf0String(this)
fun Map<String, Amf0Node>.toAmf0Object(): Amf0Object = Amf0Object(this)
fun Map<String, Amf0Node>.toAmf0ECMAArray(): Amf0ECMAArray = Amf0ECMAArray(this)