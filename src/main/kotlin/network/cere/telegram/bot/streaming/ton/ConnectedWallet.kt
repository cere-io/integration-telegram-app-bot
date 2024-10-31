package network.cere.telegram.bot.streaming.ton

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "connected_wallets")
data class ConnectedWallet(
    @EmbeddedId
    val id: ConnectedWalletKey,

    val connectedAt: LocalDateTime,

    ) : PanacheEntityBase {
    companion object : PanacheCompanionBase<ConnectedWallet, ConnectedWalletKey>
}
