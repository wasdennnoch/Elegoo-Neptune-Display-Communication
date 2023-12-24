package n3p

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

class SerialConnection(
    port: String,
    baudRate: Int = 115200,
) : SerialInterface {

    private val _reader: Channel<UByte> = Channel()
    override val reader: ReceiveChannel<UByte> = _reader

    private val _writer: Channel<UByte> = Channel()
    override val writer: SendChannel<UByte> = _writer

    private val serial: SerialPort = SerialPort.getCommPort(port).apply {
        setBaudRate(baudRate)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun start() {
        serial.openPort()
        serial.addDataListener(object : SerialPortDataListener {
            override fun getListeningEvents(): Int = SerialPort.LISTENING_EVENT_DATA_RECEIVED
            override fun serialEvent(event: SerialPortEvent) {
                if (event.eventType != SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
                    return
                }
                val data = event.receivedData

                runBlocking {
                    data.forEach {
                        _reader.send(it.toUByte())
                    }
                }
            }
        })
        scope.launch {
            serial.outputStream.buffered().use { output ->
                for (byte in _writer) {
                    output.write(byte.toInt())
                    output.flush()
                }
            }
        }
    }

    fun destroy() {
        scope.cancel()
        _reader.close()
        _writer.close()
    }

}
