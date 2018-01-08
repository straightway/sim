/****************************************************************************
Copyright 2016 github.com/straightway

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 ****************************************************************************/
package straightway.sim.net

import straightway.sim.TimeProvider
import straightway.units.*
import java.time.LocalDateTime

/**
 * TransmissionStream transmitting a message to a receiver channel. When the request is accepted,
 * its notifyReceive date is determined by the slowest of both channels. It never changes.
 */
class AsyncSequentialTransmissionStream(
    private val bandwidth: UnitValue<Int, Bandwidth>,
    private val timeProvider: TimeProvider) : TransmissionStream {

    data class TransmissionRecord(val startTime: LocalDateTime, val duration: UnitNumber<Time>) {
        val endTime: LocalDateTime by lazy { startTime + duration }
        override fun equals(other: Any?) =
            if (other is TransmissionRecord)
                abs(startTime - other.startTime) < 10[nano(second)] &&
                    abs(duration - other.duration) < 10[nano(second)]
            else
                super.equals(other)

        override fun hashCode() =
            (startTime.nano / 10).hashCode() xor startTime.second.hashCode() xor duration.hashCode()
    }

    override fun requestTransmission(request: TransmitRequest): TransmitOffer {
        discardExpiredTransmissions()
        return request.createOffer()
    }

    override fun accept(offer: TransmitOffer) =
        if (offer.isMyOwn) acceptOwn(offer) else acceptForeign(offer)

    val scheduledTransmissions get() = _scheduledTransmissions

    private fun discardExpiredTransmissions() {
        _scheduledTransmissions = _scheduledTransmissions.dropWhile { it.endTime < timeProvider.currentTime }
    }

    private fun acceptOwn(offer: TransmitOffer) {
        _scheduledTransmissions = offer.transmissions
    }

    private fun acceptForeign(offer: TransmitOffer) {
        val splitScheduledTransmissions = _scheduledTransmissions.splitAt(offer.finishTime)
        val reverseFirst = splitScheduledTransmissions.first.reverse
        val scheduler = TransmissionScheduler(reverseFirst, offer.finishTime, -offer.request.duration)
        val reverseScheduled = scheduler.transmissions
        _scheduledTransmissions = reverseScheduled.reverse mergeWith splitScheduledTransmissions.second
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
        get() = TransmissionScheduler(_scheduledTransmissions, timeProvider.currentTime, duration).transmissions

    private class TransmissionScheduler(private val scheduledTransmissions: List<TransmissionRecord>,
                                        private val startTime: LocalDateTime,
                                        private val duration: UnitNumber<Time>) {

        val transmissions: List<TransmissionRecord>
            get() =
                if (canBeEntirelyTransmittedFirst)
                    listOf(newCompleteTransmission) + scheduledTransmissions
                else
                    listOf(mergedFirstTransmission) + restSchedule.drop(1)

        private val canBeEntirelyTransmittedFirst
            get() =
                abs(duration) < abs(firstGapSize) || scheduledTransmissions.isEmpty()

        private val newCompleteTransmission
            get() =
                TransmissionRecord(startTime, duration)

        private val mergedFirstTransmission
            get() =
                TransmissionRecord(startTime, firstGapSize + firstTransmission.duration + restSchedule.first().duration)

        private val restSchedule by lazy {
            TransmissionScheduler(scheduledTransmissions.drop(1), firstTransmission.endTime, duration - firstGapSize).transmissions
        }

        private val firstGapSize by lazy {
            firstTransmission.startTime - startTime
        }

        private val firstTransmission by lazy {
            scheduledTransmissions.firstOrNull() ?: TransmissionRecord(startTime, 0[second])
        }
    }

    private val TransmitRequest.duration get() = (message.size / bandwidth)[second]

    @Suppress("UNCHECKED_CAST")
    private val TransmitOffer.transmissions
        get() = memento as List<TransmissionRecord>
    private val TransmitOffer.isMyOwn get() = issuer === this@AsyncSequentialTransmissionStream

    private fun List<TransmissionRecord>.splitAt(time: LocalDateTime): Pair<List<TransmissionRecord>, List<TransmissionRecord>> =
        when {
            isEmpty() -> Pair(listOf(), listOf())
            time.isBefore(first().startTime) -> Pair(listOf(), this)
            last().endTime.isBefore(time) -> Pair(this, listOf())
            time.isBefore(first().endTime) -> Pair(
                listOf(TransmissionRecord(first().startTime, time - first().startTime)),
                listOf(TransmissionRecord(time, first().endTime - time)) + drop(1))
            else -> {
                val restSplit = drop(1).splitAt(time)
                Pair(listOf(first()) + restSplit.first, restSplit.second)
            }
        }

    private infix fun List<TransmissionRecord>.mergeWith(tail: List<TransmissionRecord>) = when {
        isEmpty() -> tail
        tail.isEmpty() -> this
        last().endTime == tail.first().startTime -> dropLast(1) + TransmissionRecord(last().startTime, last().duration + tail.first().duration) + tail.drop(1)
        else -> this + tail
    }

    private val List<TransmissionRecord>.reverse: List<TransmissionRecord>
        get() =
            if (isEmpty()) this else drop(1).reverse + first().reverse

    private val TransmissionRecord.reverse get() = TransmissionRecord(startTime + duration, -duration)

    private var _scheduledTransmissions = listOf<TransmissionRecord>()
}