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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import straightway.testing.CallCounter
import straightway.testing.CallSequence
import straightway.units.get
import straightway.units.minute
import straightway.units.plus

internal class SimulatorTest_run : SimulatorTest() {

    @Test fun executesEvent() {
        val callCounter = CallCounter()
        sut.schedule(defaultEventDuration) { callCounter.action() }
        sut.run()
        Assertions.assertEquals(1, callCounter.calls)
    }

    @Test fun executesEventAtProperTime() {
        sut.schedule(defaultEventDuration) {
            Assertions.assertEquals(initialTime + defaultEventDuration, sut.currentTime)
        }
        sut.run()
    }

    @Test fun consumesEvent() {
        sut.schedule(defaultEventDuration) {}
        sut.run()
        Assertions.assertEquals(0, sut.eventQueue.size)
    }

    @Test fun executesAllEvents() {
        val callSequence = CallSequence(0, 2, 1)
        for (i in 0..2) {
            val execTime = callSequence.expectedActionOrder[i][minute]
            val action = callSequence.actions[i]
            sut.schedule(execTime) { action() }
        }
        sut.run()
        callSequence.assertCompleted()
    }
}