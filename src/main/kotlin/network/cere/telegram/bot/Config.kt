package network.cere.telegram.bot

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "agent-service")
interface Config {
    fun appId(): String

    fun groups(): Map<String, Group>

    fun privateKey(): String

    interface Group {
        fun groupId(): Long

        fun orgId(): Int

        fun campaignId(): String
    }
}