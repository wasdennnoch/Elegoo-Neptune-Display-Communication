package n3p

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun main() = runBlocking {
    println("Starting")
    val serial = SerialConnection("COM5")

    val robinNanoAdapter = ElegooRobinNanoAdapter(SerialInterface.senderOnly(serial))
//    launch {
//        robinNanoAdapter.commandReader.consumeAsFlow().collect {
//            print("[FROM PRINTER] $it")
//        }
//    }
    launch(Dispatchers.IO) {
        while (true) {
            print("> ")
            val line = readlnOrNull() ?: break
            println("> Sending $line")
            robinNanoAdapter.sendCommand(line)
        }
    }

    val nextionAdapter = ElegooNextionAdapter(SerialInterface.receiverOnly(serial))
    launch {
        nextionAdapter.commandReader.consumeAsFlow().collect {
            printWithDate("[FROM DISPLAY] $it")
        }
    }

    println("Available serial adapters:")
    SerialPort.getCommPorts().forEach {
        println("$it - ${it.descriptivePortName}")
    }
    println("---")

    serial.start()
    println("Started!")
}

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
private fun printWithDate(message: String) = println("[${dateFormatter.format(Instant.now())}] $message")
