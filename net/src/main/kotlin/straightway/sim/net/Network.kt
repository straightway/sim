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

import straightway.sim.Scheduler
import straightway.units.Time
import straightway.units.UnitNumber
import straightway.units.minus
import straightway.units.plus
import straightway.utils.TimeProvider

/**
 * A simulated network consisting of nodes.
 */
class Network(
        private val simScheduler: Scheduler,
        private val timeProvider: TimeProvider,
        val latency: UnitNumber<Time>
) : TransmissionRequestHandler {

    override fun transmit(transmission: Transmission) {
        transmission.apply {
            val transmissionFinishedTime = scheduleTransmission(request)
            val transmissionDuration = transmissionFinishedTime - timeProvider.currentTime
            simScheduler.schedule(transmissionDuration + latency) {
                notify(receiver) received message from sender
            }
        }
    }
}