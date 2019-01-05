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

package straightway.sim

import org.junit.jupiter.api.Test
import straightway.testing.bdd.Given
import straightway.testing.flow.Equal
import straightway.testing.flow.expect
import straightway.testing.flow.is_
import straightway.testing.flow.to_
import straightway.testing.testAutoGeneratedDataClassMethods
import java.time.LocalDateTime

class EventTest {

    private companion object {
        const val DESCRIPTON = "Description"
        val eventTime = LocalDateTime.of(2001, 11, 3, 14, 35, 47)
    }

    private val test get() = Given { Event(eventTime, DESCRIPTON) {} }

    @Test
    fun `Test auto-generated methods`() =
        Event(eventTime, DESCRIPTON) {}.testAutoGeneratedDataClassMethods()

    @Test
    fun `Events are comparable by time, equal`() =
            test when_ {
                compareTo(Event(eventTime, DESCRIPTON) {})
            } then {
                expect(it.result is_ Equal to_ 0)
            }

    @Test
    fun `Events are comparable by time, less`() =
            test when_ {
                compareTo(Event(eventTime.plusDays(1), DESCRIPTON) {})
            } then {
                expect(it.result is_ Equal to_ -1)
            }

    @Test
    fun `time is accessible`() =
            test when_ {
                time
            } then {
                expect(it.result is_ Equal to_ time)
            }

    @Test
    fun `description is accessible`() =
            test when_ {
                description
            } then {
                expect(it.result is_ Equal to_ DESCRIPTON)
            }

    @Test
    fun `action is accessible`() {
        var actionExecuted = false
        Given {
            Event(eventTime, DESCRIPTON) { actionExecuted = true }
        } when_ {
            action()
        } then {
            expect(actionExecuted)
        }
    }

    @Test
    fun `string representation is as expected`() =
            test when_ {
                toString()
            } then {
                expect(it.result is_ Equal to_ "$eventTime: $DESCRIPTON")
            }
}