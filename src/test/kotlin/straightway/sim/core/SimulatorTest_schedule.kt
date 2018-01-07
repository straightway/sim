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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import straightway.testing.CallCounter
import straightway.testing.CallSequence
import straightway.units.plus

internal class SimulatorTest_schedule : SimulatorTest() {

    @Test fun doesNotCallActionImmediately() =
            sut.schedule(defaultEventDuration, Companion::doNotCall)

    @Test fun addsEventToEventQueue() {
        sut.schedule(defaultEventDuration, Companion::doNotCall)
        assertEquals(1, sut.eventQueue.size)
    }

    @Test fun schedulesEventAtProperTime() {
        sut.schedule(defaultEventDuration, Companion::doNotCall)
        val targetTime = sut.currentTime + defaultEventDuration
        assertEquals(targetTime, sut.eventQueue.first().time)
    }

    @Test fun addsEventWithSpecifiedAction() {
        val callCounter = CallCounter()
        sut.schedule(defaultEventDuration) { callCounter.action() }
        sut.eventQueue.first().action()
        assertEquals(1, callCounter.calls)
    }

    @Test fun allowsSchedulingNewEventWhileExecutingAction() {
        val callSequence = CallSequence(0, 1)
        sut.schedule(defaultEventDuration) {
            callSequence.actions[0]()
            sut.schedule(defaultEventDuration) { callSequence.actions[1]() }
        }
        sut.run()
        callSequence.assertCompleted()
    }

    private companion object {
        fun doNotCall() = fail("must not be called")
    }
}