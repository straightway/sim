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
import straightway.units.UnitNumber
import straightway.units.abs
import straightway.units.get
import straightway.units.minus
import straightway.units.plus
import straightway.units.second
import java.time.LocalDateTime

internal class TransmissionScheduler(
        private val scheduledTransmissions: List<TransmissionRecord>,
        private val startTime: LocalDateTime,
        private val duration: UnitNumber<Time>
) {

    val transmissions: List<TransmissionRecord>
        get() =
            if (canBeEntirelyTransmittedFirst)
                listOf(newCompleteTransmission) + scheduledTransmissions
            else listOf(mergedFirstTransmission) + restSchedule.drop(1)

    private val canBeEntirelyTransmittedFirst
        get() = abs(duration) < abs(firstGapSize) || scheduledTransmissions.isEmpty()

    private val newCompleteTransmission
        get() = TransmissionRecord(startTime, duration)

    private val mergedFirstTransmission
        get() = TransmissionRecord(
                startTime,
                firstGapSize + firstTransmission.duration + restSchedule.first().duration)

    private val restSchedule by lazy {
        TransmissionScheduler(
                scheduledTransmissions.drop(1),
                firstTransmission.endTime,
                duration - firstGapSize).transmissions
    }

    private val firstGapSize by lazy {
        firstTransmission.startTime - startTime
    }

    private val firstTransmission by lazy {
        scheduledTransmissions.firstOrNull() ?: TransmissionRecord(startTime, 0[second])
    }
}