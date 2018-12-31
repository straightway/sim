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

import straightway.units.Time
import straightway.units.UnitValue
import straightway.units.abs
import straightway.units.get
import straightway.units.minus
import straightway.units.nano
import straightway.units.plus
import straightway.units.second
import java.time.LocalDateTime

/**
 * A transmission with a start time and a duration.
 */
data class TransmissionRecord(val startTime: LocalDateTime, val duration: UnitValue<Time>) {
    val endTime: LocalDateTime by lazy { startTime + duration }
    override fun equals(other: Any?) =
            if (other is TransmissionRecord)
                abs(startTime - other.startTime) < equalityThreshold &&
                        abs(duration - other.duration) < equalityThreshold
            else super.equals(other)

    override fun hashCode() = (startTime.nano / EQUALITY_THRESHOLD_NS).hashCode() xor
            startTime.second.hashCode() xor
            duration.hashCode()

    companion object {
        private const val EQUALITY_THRESHOLD_NS = 10
        private val equalityThreshold = EQUALITY_THRESHOLD_NS[nano(second)]
    }
}

internal fun List<TransmissionRecord>.splitAt(time: LocalDateTime):
        Pair<List<TransmissionRecord>, List<TransmissionRecord>> =
        if (isEmpty()) Pair(listOf(), listOf()) else splitNonEmptyAt(time)

internal infix fun List<TransmissionRecord>.mergeWith(tail: List<TransmissionRecord>) = when {
    isEmpty() -> tail
    tail.isEmpty() -> this
    last().endTime == tail.first().startTime -> dropLast(1) +
            TransmissionRecord(last().startTime, last().duration + tail.first().duration) +
            tail.drop(1)
    else -> this + tail
}

private fun List<TransmissionRecord>.splitNonEmptyAt(time: LocalDateTime) =
        when {
            time.isBefore(startTime) -> Pair(listOf(), this)
            endTime.isBefore(time) -> Pair(this, listOf())
            time.isBefore(first().endTime) -> splitFirstRecordAt(time)
            else -> splitInTheMiddleAt(time)
        }

private fun List<TransmissionRecord>.splitInTheMiddleAt(time: LocalDateTime):
        Pair<List<TransmissionRecord>, List<TransmissionRecord>> {
    val restSplit = drop(1).splitAt(time)
    return Pair(listOf(first()) + restSplit.first, restSplit.second)
}

private fun List<TransmissionRecord>.splitFirstRecordAt(time: LocalDateTime) =
        Pair(
                listOf(TransmissionRecord(first().startTime, time - first().startTime)),
                listOf(TransmissionRecord(time, first().endTime - time)) + drop(1))

private val List<TransmissionRecord>.startTime: LocalDateTime get() = first().startTime

private val List<TransmissionRecord>.endTime: LocalDateTime get() = last().endTime
