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

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.jupiter.api.Test
import straightway.expr.minus
import straightway.sim.Scheduler
import straightway.testing.bdd.Given
import straightway.testing.flow.Equal
import straightway.testing.flow.False
import straightway.testing.flow.Not
import straightway.testing.flow.Throw
import straightway.testing.flow.True
import straightway.testing.flow.does
import straightway.testing.flow.expect
import straightway.testing.flow.is_
import straightway.testing.flow.to_
import straightway.units.Time
import straightway.units.UnitDouble
import straightway.units.get
import straightway.units.hour

class InterceptingSchedulerTest {

    private class Environment {
        val wrappedScheduler = mock<Scheduler> {
            on { schedule(any(), any(), any()) }.thenAnswer {
                args -> args.getArgument<() -> Unit>(2)()
            }
        }
        val sut = InterceptingScheduler(wrappedScheduler)
        lateinit var eventTime: UnitDouble<Time>
        lateinit var eventDescription: String
        var isExecuted = false
        fun intercept(time: UnitDouble<Time>, description: String) {
            eventTime = time
            eventDescription = description
        }
        fun intercept(description: String) {
            eventDescription = description
        }
    }

    private fun test(init: Environment.() -> Unit = {}) =
        Given {
            Environment()
        } while_ {
            init()
        } when_ {
            sut.schedule(10.0[hour], "description") { isExecuted = true }
        }

    @Test
    fun `scheduled event is forwarded to wrapped scheduler`() =
            test() then {
                verify(wrappedScheduler).schedule(eq(10.0[hour]), eq("description"), any())
            }

    @Test
    fun `scheduled event is intercepted by onScheduled interceptor`() =
            test {
                sut.onScheduled(this::intercept)
            } then {
                expect(eventTime is_ Equal to_ 10.0[hour])
                expect(eventDescription is_ Equal to_ "description")
            }

    @Test
    fun `executing event is intercepted by onExecuting interceptor`() =
            test {
                sut.onExecuting(this::intercept)
            } then {
                expect(eventDescription is_ Equal to_ "description")
            }

    @Test
    fun `onExecuting action is called before action is executed`() =
            test {
                sut.onExecuting { expect(isExecuted is_ False) }
            } then {
                expect({ it.result } does Not - Throw.exception)
                expect(isExecuted is_ True)
            }

    @Test
    fun `executed event is intercepted by onExecuted interceptor`() =
            test {
                sut.onExecuted(this::intercept)
            } then {
                expect(eventDescription is_ Equal to_ "description")
            }

    @Test
    fun `onExecuted action is called after action is executed`() =
            test {
                sut.onExecuted { expect(isExecuted is_ True) }
            } then {
                expect({ it.result } does Not - Throw.exception)
                expect(isExecuted is_ True)
            }
}