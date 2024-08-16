package network.cere.telegram.bot.streaming.ton

import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.arc.profile.UnlessBuildProfile
import jakarta.enterprise.context.ApplicationScoped
import java.security.SecureRandom
import java.util.*

class RandomProvider {
    @ApplicationScoped
    @UnlessBuildProfile("prod")
    fun random(): Random = Random(42)

    @ApplicationScoped
    @IfBuildProfile("prod")
    fun secureRandom(): Random = SecureRandom()
}