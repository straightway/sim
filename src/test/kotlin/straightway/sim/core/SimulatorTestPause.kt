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

import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import straightway.expr.minus
import straightway.sim.Controller
import straightway.testing.flow.Not
import straightway.testing.flow.Size
import straightway.testing.flow.Throw
import straightway.testing.flow.does
import straightway.testing.flow.Equal
import straightway.testing.flow.expect
import straightway.testing.flow.has
import straightway.testing.flow.is_
import straightway.testing.flow.of
import straightway.testing.flow.to_
import straightway.units.get
import straightway.units.minute

class SimulatorTestPause : SimulatorTest() {

    @Test
    fun withoutEvent_hasNoEffect() = (sut as Controller).pause()

    @Test
    fun calledWhileRunning_stopsSimulation() {
        sut.schedule(1.0[minute]) { sut.pause() }
        sut.schedule(2.0[minute]) { fail("This event must not be called") }
        expect({ sut.run() } does Not - Throw.exception)
        expect(sut.eventQueue has Size of 1)
    }

    @Test
    fun callingRunAfterPause_resumesSimulation() {
        val numCalls = mutableListOf(0, 0)
        sut.schedule(1.0[minute]) { ++numCalls[0]; sut.pause() }
        sut.schedule(2.0[minute]) { ++numCalls[1] }
        expect(numCalls is_ Equal to_ listOf(0, 0))
        sut.run()
        expect(numCalls is_ Equal to_ listOf(1, 0))
        sut.run()
        expect(numCalls is_ Equal to_ listOf(1, 1))
    }
}