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
package straightway.sim.net

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import straightway.dsl.minus
import straightway.sim.TimeProvider
import straightway.sim.core.Simulator
import straightway.testing.TestBase
import straightway.testing.flow.*
import straightway.units.*
import java.time.LocalDateTime

class AsyncSequentialTransmissionStreamTest : TestBase<AsyncSequentialTransmissionStreamTest.Environment>() {

    class Environment {

        fun channel(bandwidth: UnitValue<Int, Bandwidth>) =
            channels.getOrPut(bandwidth) { AsyncSequentialTransmissionStream(bandwidth, timeProvider) }

        var currentTime: LocalDateTime = LocalDateTime.of(0, 1, 1, 0, 0)

        private val timeProvider = mock<TimeProvider> {
            on { currentTime } doAnswer { currentTime }
        }
        private val channels = mutableMapOf<UnitValue<Int, Bandwidth>, AsyncSequentialTransmissionStream>()
    }

    @BeforeEach
    fun setup() {
        sut = Environment()
    }

    @Test
    fun receiverIsNoAsyncSequentialChannel_doesNotThrow() = sut.run {
        val otherChannel = TransmissionStreamMock("other", TimeLog(Simulator()))
        expect({ transmit(message(100[bit]) from channel(10[bit / second]) to otherChannel) } does not - _throw - exception)
    }

    @Test
    fun senderIsNoAsyncSequentialChannel_doesNotThrow() = sut.run {
        val otherChannel = TransmissionStreamMock("other", TimeLog(Simulator()))
        expect({ transmit(message(100[bit]) from otherChannel to channel(10[bit / second])) } does not - _throw - exception)
    }

    @Test
    fun lowerBandwidthDeterminesTransmissionTime() = sut.run {
        val time = transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue _is equal _to 10[second])
    }

    @Test
    fun secondTransmissionGoesOnTop_sameChannels_sameDirection() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        val time = transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue _is equal _to 20[second])
    }

    @Test
    fun secondTransmissionGoesOnTop_sameChannels_oppositeDirection() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        val time = transmit(message(100[bit]) from channel(100[bit / second]) to channel(10[bit / second]))
        expect(time.unitValue _is equal _to 20[second])
    }

    @Test
    fun secondTransmissionComesFirst_ifGapIsLargeEnough() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        val time = transmit(message(100[bit]) from channel(100[bit / second]) to channel(1000[bit / second]))
        expect(time.unitValue _is equal _to 1[second])
    }

    @Test
    fun secondTransmissionOverlapsFirstTransmission() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        val time = transmit(message(1000[bit]) from channel(100[bit / second]) to channel(200[bit / second]))
        expect(time.unitValue _is equal _to 11[second])
    }

    @Test
    fun thirdTransmissionOverlapsTwoPreviousTransmissions() = sut.run {
        transmit(message(100[bit]) from channel(50[bit / second]) to channel(100[bit / second]))
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        val time = transmit(message(1000[bit]) from channel(100[bit / second]) to channel(200[bit / second]))
        expect(time.unitValue _is equal _to 12[second])
    }

    @Test
    fun twoFilledOverlayGaps() = sut.run {
        transmit(message(100[bit]) from channel(50[bit / second]) to channel(100[bit / second]))
        transmit(message(100[bit]) from channel(20[bit / second]) to channel(100[bit / second]))
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        transmit(message(600[bit]) from channel(100[bit / second]) to channel(200[bit / second]))
        val time = transmit(message(400[bit]) from channel(100[bit / second]) to channel(300[bit / second]))
        expect(time.unitValue _is equal _to 13[second])
    }

    @Test
    fun foreignOfferIstScheduledAtEndTime() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(9[second], 1[second])
        ))

        var time = transmit(message(899[bit]) from channel(200[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue _is equal _to 8.99[second])
        time = transmit(message(1[bit]) from channel(200[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue _is equal _to 10[second])
    }

    @Test
    fun secondForeignOfferWithSameEndTimeIstScheduledBeforeFirstOne() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(9[second], 1[second])
        ))
        transmit(message(200[bit]) from channel(20[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(7[second], 3[second])
        ))

        var time = transmit(message(699[bit]) from channel(200[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue _is equal _to 6.99[second])
        time = transmit(message(1[bit]) from channel(200[bit / second]) to channel(100[bit / second]))
        expect(time.unitValue _is equal _to 10[second])
    }

    @Test
    fun foreignOfferEndsInTheMiddleOfExistingTransmissionAndIsScheduledBefore() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(9[second], 1[second])
        ))
        transmit(message(190[bit]) from channel(20[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(7.1[second], 2.9[second])
        ))
    }

    @Test
    fun foreignOffersNotOverlappingAddedInBetween() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(9[second], 1[second])
        ))

        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(9[second], 1[second]),
            transmissionBlock(19[second], 1[second])
        ))

        transmit(message(300[bit]) from channel(20[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(9[second], 1[second]),
            transmissionBlock(12[second], 3[second]),
            transmissionBlock(19[second], 1[second])
        ))
    }

    @Test
    fun oldTransmissionsAreCleanedUp_onTransmissionRequest() = sut.run {
        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(9[second], 1[second])
        ))
        expect(channel(10[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(0[second], 10[second])
        ))

        currentTime += 100[second]

        transmit(message(100[bit]) from channel(10[bit / second]) to channel(100[bit / second]))
        expect(channel(100[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(109[second], 1[second])
        ))
        expect(channel(10[bit / second]).scheduledTransmissions _is equal _to listOf(
            transmissionBlock(100[second], 10[second])
        ))
    }

    private fun transmissionBlock(startTime: UnitNumber<Time>, duration: UnitNumber<Time>) =
        AsyncSequentialTransmissionStream.TransmissionRecord(LocalDateTime.of(0, 1, 1, 0, 0) + startTime, duration)

    private companion object {
        fun message(size: UnitValue<Int, AmountOfData> = 100[byte]) = createMessage(size)
    }
}