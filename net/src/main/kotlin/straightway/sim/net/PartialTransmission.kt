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

package straightway.sim.net

/**
 * A transmission builder class, allowing to specify a transmission using a sequence of statements.
 */
class PartialTransmission(private val receiver: Node) {

    infix fun from(sender: Node) {
        receiver.notifyReceive(sender, message)
    }

    infix fun received(message: Message): PartialTransmission {
        this.message = message; return this
    }

    private lateinit var message: Message
}

fun notify(receiver: Node) = PartialTransmission(receiver)