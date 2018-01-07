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
package straightway.sim.core

import straightway.sim.Controller
import straightway.sim.Event
import straightway.sim.Scheduler
import straightway.sim.TimeProvider
import straightway.units.Time
import straightway.units.UnitValue
import straightway.units.plus
import java.time.LocalDateTime

/**
 * Run an event driven simulation by executing actions at given simulated time points.
 */
class Simulator : TimeProvider, Controller, Scheduler {

    override var currentTime: LocalDateTime = LocalDateTime.of(0, 1, 1, 0, 0)
        private set

    val eventQueue: List<Event> get() = _eventQueue

    override fun schedule(duration: UnitValue<*, Time>, action: () -> Unit) {
        val newEvent = Event(currentTime + duration, action)
        val insertPos = findInsertPosFor(newEvent)
        _eventQueue.add(insertPos, newEvent)
    }

    override fun run() {
        isRunning = true
        while (isRunning && _eventQueue.any()) {
            val nextEvent = popNextEvent()
            execute(nextEvent)
        }
    }

    override fun pause() {
        isRunning = false
    }

    override fun reset() =
        _eventQueue.clear()

    //<editor-fold desc="Private">

    private fun findInsertPosFor(newEvent: Event): Int =
        _eventQueue.indexOfFirst { newEvent.time < it.time }
                .let { if (it < 0) _eventQueue.size else it }

    private fun popNextEvent(): Event {
        val nextEvent = _eventQueue.first()
        _eventQueue.removeAt(0)
        return nextEvent
    }

    private fun execute(event: Event) {
        currentTime = event.time
        event.action()
    }

    private val _eventQueue: MutableList<Event> = mutableListOf()
    private var isRunning = false

    //</editor-fold>
}