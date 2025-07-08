package network.cere.telegram.bot

fun ByteArray.hex(withPrefix: Boolean = true) = (if (withPrefix) "0x" else "") + joinToString("") { "%02x".format(it) }

fun String.decodeHex() = substringAfter('x').chunked(2).map { it.toInt(16).toByte() }.toByteArray()