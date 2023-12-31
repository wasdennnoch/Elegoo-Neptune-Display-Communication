package n3p

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import java.util.*

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

    suspend fun sendCommand(command: DisplayCommand) = sendCommand(command.command)

    suspend fun sendCommand(command: String) {
        val bytes = command.toByteArray()
        bytes.forEach { serial.writer.send(it.toUByte()) }
        eol.forEach { serial.writer.send(it) }
    }

    fun destroy() {
        scope.cancel()
    }

}

open class DisplayCommand(val command: String)

enum class DisplayPage(val pageName: String) {
    BOOT("boot"),
    MAIN("main"),

    // File List
    FILE_LIST_PAGE_1("file1"),
    FILE_LIST_PAGE_2("file2"),
    FILE_LIST_PAGE_3("file3"),
    FILE_LIST_PAGE_4("file4"),
    FILE_LIST_PAGE_5("file5"),
    NO_SD_CARD("nosdcard"),
    NO_FILAMENT("nofilament"),

    // Printing
    PRINT_CONFIRMATION("askprint"),
    PRINTING("printpause"),
    PRINT_COMPLETE("printfinish"),
    PAUSE_CONFIRMATION("pauseconfirm"),
    RESUME_CONFIRMATION("resumeconfirm"),
    FILAMENT_REFILL("filamentresume"),
    ADJUST_PRINT_TEMPERATURE("adjusttemp"),
    ADJUST_PRINT_SPEED("adjustspeed"),
    ADJUST_Z_OFFSET("adjustzoffset"),

    // Manual Control
    PREHEAT("pretemp"),
    MANUAL_MOVEMENT("premove"),
    MANUAL_EXTRUSION("prefilament"),

    // Settings
    SETTINGS("set"),
    FACTORY_SETTINGS("factorysetting"),
    LANGUAGE_SETTINGS("language"),
    FILAMENT_PRESET_SETTINGS("tempset"),
    FILAMENT_PRESET_ADJUST("tempsetvalue"),
    ABOUT_MAHINE("information"),

    // Leveling
    AUTOLEVEL_CONFIRMATION("tips_level"),
    AUTOLEVEL_PREHEATING("leveling"),
    AUTOLEVEL_MEASUREMENTS_16("leveldata"),

    // Misc
    WAIT("wait"),
    AUTOHOMING("autohome"),
    POWER_LOSS_CONTINUE_PRINT("continueprint"),

    // Unused
    FILAMENT_CHECK("filamentcheck"),
    LANGUAGE_SETTINGS_2("languageset"),
    LED_CONTROL("ledcontrl"),
}
