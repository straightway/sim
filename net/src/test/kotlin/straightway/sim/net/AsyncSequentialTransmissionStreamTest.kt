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

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import straightway.error.Panic
import straightway.expr.minus
import straightway.sim.core.Simulator
import straightway.testing.TestBase
import straightway.testing.flow.Not
import straightway.testing.flow.does
import straightway.testing.flow.Equal
import straightway.testing.flow.expect
import straightway.testing.flow.is_
import straightway.testing.flow.Throw
import straightway.testing.flow.True
import straightway.testing.flow.to_
import straightway.units.AmountOfData
import straightway.units.Bandwidth
import straightway.units.Time
import straightway.units.UnitNumber
import straightway.units.UnitValue
import straightway.units.bit
import straightway.units.byte
import straightway.units.div
import straightway.units.get
import straightway.units.plus
import straightway.units.second
import straightway.units.unitValue
import straightway.utils.TimeProvider
import java.time.LocalDateTime

class AsyncSequentialTransmissionStreamTest :
        TestBase<AsyncSequentialTransmissionStreamTest.Environment>() {

    class Environment {

        fun channel(bandwidth: UnitValue<Int, Bandwidth>) =
                channels.getOrPut(bandwidth) {
                    AsyncSequentialTransmissionStream(bandwidth, timeProvider)
                }

        var currentTime: LocalDateTime = LocalDateTime.of(0, 1, 1, 0, 0)

        private val timeProvider = mock<TimeProvider> {
            on { currentTime } doAnswer { currentTime }
        }
        private val channels =
                mutableMapOf<UnitValue<Int, Bandwidth>, AsyncSequentialTransmissionStream>()
    }

    @BeforeEach
    fun setup() {
        sut = Environment()
    }

    @Test
    fun `is initially online`() = sut.run {
        expect(channel(10[bit/second]).isOnline is_ True)
    }

    @Test
    fun `requesting a transmission on an offline stream panics`() = sut.run {
        val testChannel = channel(10[bit/second])
        testChannel.isOnline = false
        val request = TransmitRequest(message(), channel(12[bit/second]))
        expect({ testChannel.requestTransmission(request) } does Throw.type<Panic>())
    }

    @Test
    fun `accepting a transmission on an offline stream panics`() = sut.run {
        val testChannel = channel(10[bit/second])
        testChannel.isOnline = false
        expect({ testChannel.accept(
                TransmitOffer(
                    issuer = channel(12[bit/second]),
                    finishTime = LocalDateTime.of(2000, 1, 1, 0, 0),
                    request = TransmitRequest(message(), channel(12[bit/second]))))
               } does Throw.type<Panic>())
    }

    @Test
    fun receiverIsNoAsyncSequentialChannel_doesNotThrow() = sut.run {
        val otherChannel = TransmissionStreamMock("other", TimeLog(Simulator()))
        expect({ scheduleTransmission(message(100[bit])
                      from channel(10[bit / second]) to otherChannel) }
                      does Not - Throw.exception)
    }

    @Test
    fun senderIsNoAsyncSequentialChannel_doesNotThrow() = sut.run {
        val otherChannel = TransmissionStreamMock("other", TimeLog(Simulator()))
        expect({ scheduleTransmission(message(100[bit])
                      from otherChannel to channel(10[bit / second])) }
                      does Not - Throw.exception)
    }

    @Test
    fun lowerBandwidthDeterminesTransmissionTime() = sut.run {
        val time = scheduleTransmission(
                message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue is_ Equal to_ 10[second])
    }

    @Test
    fun secondTransmissionGoesOnTop_sameChannels_sameDirection() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        val time = scheduleTransmission(
                message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue is_ Equal to_ 20[second])
    }

    @Test
    fun secondTransmissionGoesOnTop_sameChannels_oppositeDirection() = sut.run {
        scheduleTransmission(message(100[bit])
                 from channel(10[bit / second]) to channel(100[bit / second]))
        val time = scheduleTransmission(
                message(100[bit]) from channel(100[bit / second]) to channel(10[bit / second]))
        expect(time.unitValue is_ Equal to_ 20[second])
    }

    @Test
    fun secondTransmissionComesFirst_ifGapIsLargeEnough() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                 to channel(100[bit / second]))
        val time = scheduleTransmission(
                message(100[bit]) from channel(100[bit / second]) to channel(1000[bit / second]))
        expect(time.unitValue is_ Equal to_ 1[second])
    }

    @Test
    fun secondTransmissionOverlapsFirstTransmission() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                 to channel(100[bit / second]))
        val time = scheduleTransmission(
                 message(1000[bit]) from channel(100[bit / second]) to channel(200[bit / second]))
        expect(time.unitValue is_ Equal to_ 11[second])
    }

    @Test
    fun thirdTransmissionOverlapsTwoPreviousTransmissions() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(50[bit / second])
                                     to channel(100[bit / second]))
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        val time = scheduleTransmission(
                message(1000[bit]) from channel(100[bit / second])
                        to channel(200[bit / second]))
        expect(time.unitValue is_ Equal to_ 12[second])
    }

    @Test
    fun twoFilledOverlayGaps() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(50[bit / second])
                                     to channel(100[bit / second]))
        scheduleTransmission(message(100[bit]) from channel(20[bit / second])
                                     to channel(100[bit / second]))
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        scheduleTransmission(message(600[bit]) from channel(100[bit / second])
                                     to channel(200[bit / second]))
        val time = scheduleTransmission(
                message(400[bit]) from channel(100[bit / second])
                        to channel(300[bit / second]))
        expect(time.unitValue is_ Equal to_ 13[second])
    }

    @Test
    fun foreignOfferIstScheduledAtEndTime() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_
                        listOf(transmissionBlock(9[second], 1[second])))

        var time = scheduleTransmission(
                message(899[bit]) from channel(200[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue is_ Equal to_ 8.99[second])
        time = scheduleTransmission(
                message(1[bit]) from channel(200[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue is_ Equal to_ 10[second])
    }

    @Test
    fun secondForeignOfferWithSameEndTimeIstScheduledBeforeFirstOne() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(9[second], 1[second])))
        scheduleTransmission(message(200[bit]) from channel(20[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(7[second], 3[second])))

        var time = scheduleTransmission(
                message(699[bit]) from channel(200[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue is_ Equal to_ 6.99[second])
        time = scheduleTransmission(
                message(1[bit]) from channel(200[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue is_ Equal to_ 10[second])
    }

    @Test
    fun foreignOfferEndsInTheMiddleOfExistingTransmissionAndIsScheduledBefore() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(9[second], 1[second])))
        scheduleTransmission(message(190[bit]) from channel(20[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(7.1[second], 2.9[second])))
    }

    @Test
    fun foreignOffersNotOverlappingAddedInBetween() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(9[second], 1[second])))

        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(9[second], 1[second]),
                        transmissionBlock(19[second], 1[second])))

        scheduleTransmission(message(300[bit]) from channel(20[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(9[second], 1[second]),
                        transmissionBlock(12[second], 3[second]),
                        transmissionBlock(19[second], 1[second])))
    }

    @Test
    fun oldTransmissionsAreCleanedUp_onTransmissionRequest() = sut.run {
        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(9[second], 1[second])))
        expect(
                channel(10[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(0[second], 10[second])))

        currentTime += 100[second]

        scheduleTransmission(message(100[bit]) from channel(10[bit / second])
                                     to channel(100[bit / second]))
        expect(
                channel(100[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(109[second], 1[second])))
        expect(
                channel(10[bit / second]).scheduledTransmissions is_ Equal to_ listOf(
                        transmissionBlock(100[second], 10[second])))
    }

    private fun transmissionBlock(startTime: UnitNumber<Time>, duration: UnitNumber<Time>) =
            TransmissionRecord(
                    LocalDateTime.of(0, 1, 1, 0, 0) + startTime,
                    duration)

    private companion object {
        fun message(size: UnitValue<Int, AmountOfData> = 100[byte]) = createMessage(size)
    }
}