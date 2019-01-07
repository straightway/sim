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
package straightway.sim

import java.time.LocalDateTime

/**
 * A simulation event.
 */
data class Event(
        val time: LocalDateTime,
        val sequenceNumber: Int,
        val description: String,
        val action: () -> Unit
) : Comparable<Event> {
    override fun compareTo(other: Event) = when (val result = time.compareTo(other.time)) {
        0 -> sequenceNumber.compareTo(other.sequenceNumber)
        else -> result
    }
    override fun toString() = "$time: $description"
}