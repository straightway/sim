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

import straightway.sim.Scheduler
import straightway.units.Time
import straightway.units.UnitDouble

/**
 * Event scheduler implementation wrapping another scheduler and allowing intercepting
 * scheduling and execution of events with registered actions.
 */
class InterceptingScheduler(private val wrapped: Scheduler) : Scheduler {

    private var onScheduledAction: (UnitDouble<Time>, String) -> Unit = { _, _ -> }
    private var onExecutingAction: (String) -> Unit = {}
    private var onExecutedAction: (String) -> Unit = {}

    fun onScheduled(action: (UnitDouble<Time>, String) -> Unit) =
            apply { onScheduledAction = action }
    fun onExecuting(action: (String) -> Unit) =
            apply { onExecutingAction = action }
    fun onExecuted(action: (String) -> Unit) =
            apply { onExecutedAction = action }

    override fun schedule(
            relativeStartTime: UnitDouble<Time>, description: String, action: () -> Unit
    ) {
        wrapped.schedule(relativeStartTime, description) {
            onExecutingAction(description)
            action()
            onExecutedAction(description)
        }
        onScheduledAction(relativeStartTime, description)
    }
}