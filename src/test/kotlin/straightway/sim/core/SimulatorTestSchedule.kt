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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import straightway.testing.CallCounter
import straightway.testing.CallSequence
import straightway.testing.flow.Equal
import straightway.testing.flow.expect
import straightway.testing.flow.is_
import straightway.testing.flow.to_
import straightway.units.plus

internal class SimulatorTestSchedule : SimulatorTest() {

    @Test
    fun doesNotCallActionImmediately() =
            sut.schedule(defaultEventDuration, DESCRIPTION, Companion::doNotCall)

    @Test
    fun addsEventToEventQueue() {
        sut.schedule(defaultEventDuration, DESCRIPTION, Companion::doNotCall)
        assertEquals(1, sut.eventQueue.size)
    }

    @Test
    fun schedulesEventAtProperTime() {
        sut.schedule(defaultEventDuration, DESCRIPTION, Companion::doNotCall)
        val targetTime = sut.now + defaultEventDuration
        assertEquals(targetTime, sut.eventQueue.first().time)
    }

    @Test
    fun addsEventWithSpecifiedAction() {
        val callCounter = CallCounter()
        sut.schedule(defaultEventDuration, DESCRIPTION) { callCounter.action() }
        sut.eventQueue.first().action()
        assertEquals(1, callCounter.calls)
    }

    @Test
    fun allowsSchedulingNewEventWhileExecutingAction() {
        val callSequence = CallSequence(0, 1)
        sut.schedule(defaultEventDuration, DESCRIPTION) {
            callSequence.actions[0]()
            sut.schedule(defaultEventDuration, DESCRIPTION) { callSequence.actions[1]() }
        }
        sut.run()
        callSequence.assertCompleted()
    }

    @Test
    fun `description is added to event`() {
        sut.schedule(defaultEventDuration, DESCRIPTION, Companion::doNotCall)
        expect(sut.eventQueue.single().description is_ Equal to_ DESCRIPTION)
    }

    @Test
    fun `two events at the same time are both included`() {
        sut.schedule(defaultEventDuration, "Event0") {}
        sut.schedule(defaultEventDuration, "Event1") {}
        expect(sut.eventQueue.size is_ Equal to_ 2)
    }

    @Test
    fun `int overflow does not cause exception`() {
        var x = Int.MAX_VALUE
        x++
        expect(x is_ Equal to_ Int.MIN_VALUE)
    }

    private companion object {
        const val DESCRIPTION = "Description"
        fun doNotCall() = fail<Unit>("must not be called")!!
    }
}