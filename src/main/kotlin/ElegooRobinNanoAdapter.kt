package n3p

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import java.util.*
import java.util.AbstractList

/**
 * Reads and writes commands from the Elegoo Neptune 3 Pro motherboard (ZNP Robin Nano v2.2).
 */
class ElegooRobinNanoAdapter(
    private val serial: SerialInterface,
) {

    private val _commandReader: Channel<String> = Channel()
    val commandReader: ReceiveChannel<String> = _commandReader

    private val eol = listOf<UByte>(0xFFu, 0xFFu, 0xFFu)
    private val buffer = LinkedList<UByte>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            serial.reader.consumeAsFlow().collect {
                buffer.add(it)
                if (buffer.endsWith(eol)) {
                    val command = buffer
                        .dropLast(3)
                        .map { it.toByte() }
                        .toByteArray()
                        .decodeToString()
                    _commandReader.send(command)
                    buffer.clear()
                }
            }
        }
    }

    suspend fun sendCommand(command: String) {
        val bytes = command.toByteArray()
        bytes.forEach { serial.writer.send(it.toUByte()) }
        eol.forEach { serial.writer.send(it) }
    }

    fun destroy() {
        scope.cancel()
    }

}

private fun <T> AbstractList<T>.endsWith(other: List<T>): Boolean {
    if (other.size > size) return false
    val iterator = listIterator(size)
    for (i in other.size - 1 downTo 0) {
        if (iterator.previous() != other[i]) return false
    }
    return true
}
