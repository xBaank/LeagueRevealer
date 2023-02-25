package rtmpClient.amf

import rtmpClient.amf.Amf0Node.*
import java.util.*

sealed interface Amf0Node {
    @JvmInline
    value class Amf0Number(val value: Double) : Amf0Node {
        companion object {
            const val TYPE = 0x00
        }
    }

    @JvmInline
    value class Amf0Boolean(val value: Boolean) : Amf0Node {
        companion object {
            const val TYPE = 0x01
        }
    }

    @JvmInline
    value class Amf0String(val value: String) : Amf0Node, CharSequence by value {
        companion object {
            const val TYPE = 0x02
        }
    }

    @JvmInline
    value class Amf0Object(val value: Map<String, Amf0Node>) : Amf0Node {
        companion object {
            const val TYPE = 0x03
            const val OBJECT_END = 0x09
        }
    }

    class Amf0TypedObject(val name: String, val value: Map<String, Amf0Node>) : Amf0Node {
        companion object {
            const val TYPE = 0x10
        }

        override fun toString(): String {
            return "Amf0TypedObject(name='$name', value=$value)"
        }
    }

    object Amf0Null : Amf0Node {
        const val TYPE = 0x05
    }

    object Amf0Undefined : Amf0Node {
        const val TYPE = 0x06
    }

    @JvmInline
    value class Amf0ECMAArray(val value: Map<String, Amf0Node>) : Amf0Node {
        companion object {
            const val TYPE = 0x08
        }
    }

    @JvmInline
    value class Amf0Date(val date: Date) : Amf0Node {
        companion object {
            const val TYPE = 0x0B
        }
    }

    @JvmInline
    value class Amf0StrictArray(val value: List<Amf0Node>) : Amf0Node {
        companion object {
            const val TYPE = 0x0A
        }
    }
}


//extension functions for types
fun Number.toAmf0Number(): Amf0Number = Amf0Number(this.toDouble())
fun Boolean.toAmf0Boolean(): Amf0Boolean = Amf0Boolean(this)
fun String.toAmf0String(): Amf0String = Amf0String(this)
fun Map<String, Amf0Node>.toAmf0Object(): Amf0Object = Amf0Object(this)
fun Map<String, Amf0Node>.toAmf0ECMAArray(): Amf0ECMAArray = Amf0ECMAArray(this)