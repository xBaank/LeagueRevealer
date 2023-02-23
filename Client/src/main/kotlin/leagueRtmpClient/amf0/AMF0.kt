package leagueRtmpClient.amf0

val Amf0SupportedTypes = mapOf(
    0x00 to Amf0Type.Number,
    0x01 to Amf0Type.Boolean,
    0x02 to Amf0Type.String,
    0x03 to Amf0Type.Object,
    0x05 to Amf0Type.Null,
    0x08 to Amf0Type.ECMAArray,
    0x0a to Amf0Type.StrictArray,
    0x0b to Amf0Type.Date,
    0x0c to Amf0Type.LongString,
    0x0f to Amf0Type.XmlDocument,
    0x10 to Amf0Type.TypedObject,
    0x11 to Amf0Type.AvmPlusObject
)

const val AMF0_START_MARKER = 0x03
const val AMF0_END_MARKER = 0x09


sealed interface Amf0Type {
    object Number : Amf0Type
    object Boolean : Amf0Type
    object String : Amf0Type
    object Object : Amf0Type
    object Null : Amf0Type
    object ECMAArray : Amf0Type
    object StrictArray : Amf0Type
    object Date : Amf0Type
    object LongString : Amf0Type
    object XmlDocument : Amf0Type
    object TypedObject : Amf0Type
    object AvmPlusObject : Amf0Type
}