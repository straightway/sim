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
package straightway.sim.core

import straightway.sim.Controller
import straightway.sim.Event
import straightway.sim.Scheduler
import straightway.utils.TimeProvider
import straightway.units.Time
import straightway.units.UnitDouble
import straightway.units.plus
import java.time.LocalDateTime
import java.util.TreeSet

/**
 * Run an event driven simulation by executing actions at given simulated time points.
 */
class Simulator : TimeProvider, Controller, Scheduler {

    override var now: LocalDateTime = LocalDateTime.of(0, 1, 1, 0, 0)
        private set

    val eventQueue: List<Event> get() = _eventQueue.toList()

    override fun schedule(
            relativeStartTime: UnitDouble<Time>, description: String, action: () -> Unit
    ) {
        val newEvent = Event(now + relativeStartTime, eventSequenceNumber++, description, action)
        _eventQueue.add(newEvent)
    }

    override fun run() {
        isRunning = true
        while (isRunning && !_eventQueue.isEmpty()) {
            val nextEvent = popNextEvent()
            execute(nextEvent)
        }
    }

    override fun pause() { isRunning = false }

    override fun reset() = _eventQueue.clear()

    // <editor-fold desc="Private">

    private fun popNextEvent() = _eventQueue.first().apply { _eventQueue.remove(this) }

    private fun execute(event: Event) {
        now = event.time
        event.action()
    }

    private val _eventQueue: TreeSet<Event> = TreeSet()
    private var isRunning = false
    private var eventSequenceNumber = Int.MIN_VALUE

    // </editor-fold>
}