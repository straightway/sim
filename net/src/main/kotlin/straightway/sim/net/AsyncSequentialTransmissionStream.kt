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

import straightway.error.Panic
import straightway.units.Bandwidth
import straightway.units.UnitValue
import straightway.units.div
import straightway.units.get
import straightway.units.plus
import straightway.units.second
import straightway.utils.TimeProvider

/**
 * TransmissionStream transmitting a message to a receiver channel. When the request is accepted,
 * its notifyReceive date is determined by the slowest of both channels. It never changes.
 */
class AsyncSequentialTransmissionStream(
        private val bandwidth: UnitValue<Int, Bandwidth>,
        private val timeProvider: TimeProvider
) : TransmissionStream {

    override var isOnline = true

    override fun requestTransmission(request: TransmitRequest): TransmitOffer {
        if (!isOnline) throw Panic("Stream is offline")
        discardExpiredTransmissions()
        return request.createOffer()
    }

    override fun accept(offer: TransmitOffer) {
        if (!isOnline) throw Panic("Stream is offline")
        if (offer.isMyOwn) acceptOwn(offer) else acceptForeign(offer)
    }

    val scheduledTransmissions get() = _scheduledTransmissions

    private fun discardExpiredTransmissions() {
        _scheduledTransmissions = _scheduledTransmissions.dropWhile {
            it.endTime < timeProvider.now
        }
    }

    private fun acceptOwn(offer: TransmitOffer) {
        _scheduledTransmissions = offer.transmissions
    }

    private fun acceptForeign(offer: TransmitOffer) {
        val splitScheduledTransmissions = _scheduledTransmissions.splitAt(offer.finishTime)
        val scheduled = scheduleForeignTransmissionOffer(splitScheduledTransmissions, offer)
        _scheduledTransmissions = scheduled mergeWith splitScheduledTransmissions.second
    }

    private fun scheduleForeignTransmissionOffer(
            splitScheduledTransmissions: Pair<List<TransmissionRecord>,
                    List<TransmissionRecord>>, offer: TransmitOffer
    ): List<TransmissionRecord> {
        val reverseFirst = splitScheduledTransmissions.first.reverse
        val scheduler =
                TransmissionScheduler(reverseFirst, offer.finishTime, -offer.request.duration)
        return scheduler.transmissions.reverse
    }

    private fun TransmitRequest.createOffer(): TransmitOffer {
        val newSchedule = scheduledTransmissionsWithNewRequest
        return TransmitOffer(
                issuer = this@AsyncSequentialTransmissionStream,
                finishTime = newSchedule.first().endTime,
                request = this,
                memento = newSchedule)
    }

    private val TransmitRequest.scheduledTransmissionsWithNewRequest
        get() = TransmissionScheduler(
                _scheduledTransmissions,
                timeProvider.now,
                duration).transmissions

    private val TransmitRequest.duration get() = (message.size / bandwidth)[second]

    @Suppress("UNCHECKED_CAST")
    private val TransmitOffer.transmissions
        get() = memento as List<TransmissionRecord>

    private val TransmitOffer.isMyOwn get() = issuer === this@AsyncSequentialTransmissionStream

    private val List<TransmissionRecord>.reverse: List<TransmissionRecord>
        get() =
            if (isEmpty()) this else drop(1).reverse + first().reverse

    private val TransmissionRecord.reverse
        get() =
            TransmissionRecord(startTime + duration, -duration)

    private var _scheduledTransmissions = listOf<TransmissionRecord>()
}