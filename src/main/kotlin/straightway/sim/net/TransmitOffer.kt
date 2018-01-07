package straightway.sim.net

import java.time.LocalDateTime

data class TransmitOffer(
    val issuer: TransmissionStream,
    val finishTime: LocalDateTime,
    val request: TransmitRequest,
    val memento: Any = Any())