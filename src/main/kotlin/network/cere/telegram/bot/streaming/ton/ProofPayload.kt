package network.cere.telegram.bot.streaming.ton

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "proof_payloads")
data class ProofPayload(
    @Id
    val payload: String,
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<ProofPayload, String> {
        fun exists(payload: String) = findById(payload) != null
    }
}
