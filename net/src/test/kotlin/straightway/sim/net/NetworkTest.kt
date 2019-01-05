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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import straightway.sim.core.Simulator
import straightway.testing.TestBase
import straightway.testing.flow.Equal
import straightway.testing.flow.expect
import straightway.testing.flow.is_
import straightway.testing.flow.to_
import straightway.units.get
import straightway.units.second
import java.time.LocalDateTime

class NetworkTest : TestBase<NetworkTest.Environment>() {

    inner class Environment {
        val simulator = Simulator()
        val senderLog = TimeLog(simulator)
        val receiverLog = TimeLog(simulator)
        val network = Network(
                simScheduler = simulator,
                timeProvider = simulator,
                latency = 2.0[second],
                offlineDetectionTime = 1.0[second])
        var sender = NodeMock("sender", senderLog)
        var receiver = NodeMock("receiver", receiverLog)
        var message = createMessage()
    }

    @BeforeEach
    fun setup() {
        sut = Environment()
    }

    @Test
    fun `transmit triggers transmission on channels`() =
            sut.run {
                network.transmit(Transmission(sender, receiver, message))
                expect(senderLog.entries is_ Equal to_ listOf(
                        "00:00:00: sender_upload: Transmit $message from " +
                                "sender_upload to receiver_download"))
                expect(receiverLog.entries is_ Equal to_ listOf(
                        "00:00:00: receiver_download: Transmit $message from " +
                                "sender_upload to receiver_download"))
            }

    @Test
    fun `transmit schedules receive call`() =
            sut.run {
                sender.uploadStream.receiveTime = LocalDateTime.of(0, 1, 1, 0, 3)
                network.transmit(Transmission(sender, receiver, message))
                receiverLog.entries.clear()
                simulator.run()
                expect(receiverLog.entries is_ Equal to_ listOf(
                        "00:03:02: Receive $message from sender to receiver"))
            }

    @Test
    fun `transmit notifies about successful transmission`() =
            sut.run {
                sender.uploadStream.receiveTime = LocalDateTime.of(0, 1, 1, 0, 3)
                network.transmit(Transmission(sender, receiver, message))
                senderLog.entries.clear()
                simulator.run()
                expect(senderLog.entries is_ Equal to_ listOf(
                        "00:03:02: Successfully sent from sender to receiver"))
            }

    @Test
    fun `send notifies about failed transmission because receiver is offline`() =
            sut.run {
                receiver.isOnline = false
                network.transmit(Transmission(sender, receiver, message))
                senderLog.entries.clear()
                simulator.run()
                expect(senderLog.entries is_ Equal to_ listOf(
                        "00:00:01: Failure sending from sender to receiver"))
            }

    @Test
    fun `send notifies about failed transmission because sender is offline`() =
            sut.run {
                sender.isOnline = false
                network.transmit(Transmission(sender, receiver, message))
                expect(senderLog.entries is_ Equal to_ listOf(
                        "00:00:00: Failure sending from sender to receiver"))
            }

    @Test
    fun `transmission end is scheduled with description`() =
            sut.run {
                network.transmit(Transmission(sender, receiver, message))
                val expectedDescription = "transmission finished: ${sender.id} -> ${receiver.id}"
                expect(simulator.eventQueue.single().description is_ Equal to_ expectedDescription)
            }
}