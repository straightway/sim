package straightway.sim.net

class TransmitRequest(val message: Message, val sender: TransmissionStream) {
    val receiver: TransmissionStream get() = _receiver!!
    infix fun to(receiver: TransmissionStream): TransmitRequest {
        this._receiver = receiver
        return this
    }

    private var _receiver: TransmissionStream? = null
}

infix fun Message.from(sender: TransmissionStream) = TransmitRequest(this, sender)
fun transmit(request: TransmitRequest) = request.run {
    val sendOffer = sender.requestTransmission(request)
    val receiveOffer = receiver.requestTransmission(request)
    val slowerOffer = if (sendOffer.finishTime < receiveOffer.finishTime) receiveOffer else sendOffer

    sender.accept(slowerOffer)
    receiver.accept(slowerOffer)

    slowerOffer.finishTime
}
