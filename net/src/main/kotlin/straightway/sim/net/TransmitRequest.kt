/*
 * Copyright 2016 github.com/straightway
 *
 *  Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
    val slowerOffer =
            if (sendOffer.finishTime < receiveOffer.finishTime) receiveOffer
            else sendOffer

    sender.accept(slowerOffer)
    receiver.accept(slowerOffer)

    slowerOffer.finishTime
}
