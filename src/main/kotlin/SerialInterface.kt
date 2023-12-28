package n3p

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

interface SerialInterface {

    val reader: ReceiveChannel<UByte>
    val writer: SendChannel<UByte>

    companion object {

        fun senderOnly(serial: SerialConnection) = object : SerialInterface {
            override val reader: ReceiveChannel<UByte> = Channel()
            override val writer: SendChannel<UByte> = serial.writer
        }

        fun receiverOnly(serial: SerialConnection) = object : SerialInterface {
            override val reader: ReceiveChannel<UByte> = serial.reader
            override val writer: SendChannel<UByte> = Channel()
        }

        fun crossover(sender: SerialConnection, receiver: SerialConnection) = object : SerialInterface {
            override val reader: ReceiveChannel<UByte> = receiver.reader
            override val writer: SendChannel<UByte> = sender.writer
        }

    }

}
