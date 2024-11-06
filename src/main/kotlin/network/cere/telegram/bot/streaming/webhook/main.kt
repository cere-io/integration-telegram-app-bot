package network.cere.telegram.bot.streaming.webhook

import com.google.common.net.UrlEscapers
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

fun main() {
    val pureUrl = "https://cdn.dragon.cere.network/81/baear4ieupzxqgdwuy4f24el6nsnswdqd2mua56x5rcylwskqagjjxbiwfy/Screenshot 2024-11-01 at 07.45.18.png?token=abc "
    val escapedUrl = pureUrl

    val uri = URI.create(escapedUrl)
    val url = uri
        .let { "${it.scheme}://${it.host}${it.path}" }
    println(UrlEscapers.urlFragmentEscaper().escape(url))
}
