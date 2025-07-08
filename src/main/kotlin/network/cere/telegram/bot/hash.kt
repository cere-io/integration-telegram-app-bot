package network.cere.telegram.bot

import org.bouncycastle.jcajce.provider.digest.Blake2b.Blake2b512

fun String.hash() = Blake2b512().digest(toByteArray()).hex(false)