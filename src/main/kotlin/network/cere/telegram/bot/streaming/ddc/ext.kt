package network.cere.telegram.bot.streaming.ddc

import com.google.protobuf.ByteString
import dev.sublab.base58.StringBase58

fun ByteArray.toByteString() = ByteString.copyFrom(this)

fun StringBase58.toByteString(): ByteString = ByteString.copyFrom(this.decode())