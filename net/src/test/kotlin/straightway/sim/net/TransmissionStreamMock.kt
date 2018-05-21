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

import java.time.LocalDateTime

class TransmissionStreamMock(private val id: String, private val log: LogList) :
        TransmissionStream {

    override var isOnline = true

    override fun requestTransmission(request: TransmitRequest): TransmitOffer {
        return TransmitOffer(this, receiveTime, request)
    }

    override fun accept(offer: TransmitOffer) {
        log.add(
                "$this: Transmit ${offer.request.message} " +
                        "from ${offer.request.sender} " +
                        "to ${offer.request.receiver}")
    }

    var receiveTime = LocalDateTime.of(0, 1, 1, 0, 0)!!

    override fun toString() = id
}