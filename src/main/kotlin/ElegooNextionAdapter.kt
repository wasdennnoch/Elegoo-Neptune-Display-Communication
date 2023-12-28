package n3p

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import java.util.*

/**
 * Reads and writes commands from the Elegoo Neptune 3 Pro display (Nextion/TJC).
 */
class ElegooNextionAdapter(
    private val serial: SerialInterface,
) {

    private val _commandReader: Channel<NextionAction> = Channel()
    val commandReader: ReceiveChannel<NextionAction> = _commandReader

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val buffer = LinkedList<UByte>()
    private var readState = ReadState.IDLE

    init {
        scope.launch {
            serial.reader.consumeAsFlow().collect {
                processByte(it)
            }
        }
    }

    private suspend fun processByte(byte: UByte) {
        // println("Processing byte: ${byte.toString(16)}, state is $readState")
        when (readState) {
            ReadState.IDLE -> {
                if (byte == HEADER_1) {
                    readState = ReadState.HEADER_1_SEEN
                    buffer.add(byte)
                }
            }

            ReadState.HEADER_1_SEEN -> {
                if (byte == HEADER_2) {
                    readState = ReadState.HEADER_2_SEEN
                    buffer.add(byte)
                } else {
                    buffer.clear()
                    readState = ReadState.IDLE
                }
            }

            ReadState.HEADER_2_SEEN -> {
                // A telegram has at least a length of 3 bytes (command + one word (2 bytes) of payload).
                if (byte >= 3u) {
                    readState = ReadState.WAIT_TELEGRAM
                    buffer.add(byte)
                } else {
                    buffer.clear()
                    readState = ReadState.IDLE
                }
            }

            ReadState.WAIT_TELEGRAM -> {
                buffer.add(byte)
                val targetLengthInWords = buffer[2].toInt()
                val targetLengthInBytes = (targetLengthInWords - 3) * 2
                // println("Buffer size is ${buffer.size}, target length is $targetLengthInBytes")
                if (buffer.size == 3 + targetLengthInBytes) {
                    prepareAndDispatchIncomingAction()
                    buffer.clear()
                    readState = ReadState.IDLE
                }
            }
        }
    }

    private suspend fun prepareAndDispatchIncomingAction() {
        val command = buffer[3]
        val address = littleToBigEndian(buffer[4], buffer[5])
        val data = buffer.subList(7, buffer.size).chunked(2).map { littleToBigEndian(it[0], it[1]) }
        // println("Assembling action with data $data")
        val action = try {
            assembleIncomingAction(
                NextionDatagramCommand.from(command),
                NextionDatagramAddressKey.from(address),
                data,
            )
        } catch (e: Exception) {
            println("Failed to assemble incoming action: $e")
            return
        }
        _commandReader.send(action)
    }

    private fun assembleIncomingAction(
        command: NextionDatagramCommand,
        address: NextionDatagramAddressKey,
        data: List<UShort>,
    ): NextionAction {
        if (command != NextionDatagramCommand.VAR_ADDR_READ) {
            throw IllegalArgumentException("Only VAR_ADDR_READ is supported for incoming actions, but got command=$command, address=$address, data=$data")
        }

        if (data.isEmpty()) {
            throw IllegalArgumentException("Incoming action doesn't have any data: command=$command, address=$address, data=$data")
        }

        val action = ActionRegistry.getAction(address, data)
        requireNotNull(action) { "No action found for command=$command, address=$address, data=$data" }
        return action
    }

    fun destroy() {
        scope.cancel()
    }

    private fun littleToBigEndian(first: UByte, second: UByte): UShort {
        return ((first.toInt() shl 8) or second.toInt()).toUShort()
    }

}

private const val HEADER_1: UByte = 0x5Au
private const val HEADER_2: UByte = 0xA5u

private enum class ReadState {
    IDLE,
    HEADER_1_SEEN,
    HEADER_2_SEEN,
    WAIT_TELEGRAM,
}


/*
Command Struct:

  typedef struct DataBuf
  {
    unsigned char head[2];
    unsigned char len;
    unsigned char command;
    unsigned long addr;
    unsigned long bytelen;
    unsigned short data[32];
    unsigned char reserv[4];
  } DB;

           AutoUpload, (and answer to) Command 0x83 :
        |    data[ 0  1  2  3  4  5  6  7  8 …… ]
        | Example 5A A5 06 83 20 01 01 78 01 ……
        |          / /  |  |   \ /   |  \     \
        |        Header |  |    |    |   \_____\_ DATA (Words!)
        |     DatagramLen  /  VPAdr  |
        |           Command          DataLen (in Words)
        One Word = 2 Bytes in Little Endian Format. DataLen = 2 means 4 bytes of data.

data[0] = 0x5A
data[1] = 0xA5
data[2] = 3 + DataLen (* 2)
data[3] = cmd
data[4] = addr >> 8
data[5] = addr & 0xFF
*/

abstract class NextionAction(
    val command: NextionDatagramCommand,
    val address: NextionDatagramAddressKey,
    val data: List<UShort>,
) {

    override fun toString(): String {
        return "${javaClass.simpleName}(command=$command, address=$address, data=$data)"
    }

}

enum class NextionDatagramCommand(val command: UByte) {
    // NOTE: Writing to address 0x4F4B appears to be a hardcoded noop (0x4F 0x4B = 'OK').
    //  But even for other addresses, the command type is never actually checked anywhere, apart from during parsing.
    //  See https://github.com/NARUTOfzr/Elegoo-Neptune-marlin2.1.1/blob/be2bc9fb3ffd91c45e4ca36fe1473679f99cd708/Marlin/src/lcd/extui/dgus/elegoo/DGUSDisplayDef.cpp#L1165
    //  Fun fact, the function the above code resides in is never called anywhere.
    //  RTS_HandleData (the main datagram handler) is only called for VAR_ADDR_READ messages,
    //  See https://github.com/NARUTOfzr/Elegoo-Neptune-marlin2.1.1/blob/be2bc9fb3ffd91c45e4ca36fe1473679f99cd708/Marlin/src/lcd/extui/dgus/DGUSDisplay.cpp#L334
    REG_ADDR_WRITE(0x80u), // unused?
    REG_ADDR_READ(0x81u), // unused?
    VAR_ADDR_WRITE(0x82u), // only used for OKs?
    VAR_ADDR_READ(0x83u);

    companion object {

        fun from(command: UByte): NextionDatagramCommand {
            return entries.find { it.command == command } ?: throw IllegalArgumentException("Unknown command: $command")
        }

    }
}

enum class NextionDatagramAddressKey(val address: UShort) {
    MAIN_PAGE(0x1002u),
    ADJUSTMENT(0x1004u),
    PRINT_SPEED(0x1006u),
    STOP_PRINT(0x1008u),
    PAUSE_PRINT(0x100Au),
    RESUME_PRINT(0x100Cu),
    Z_OFFSET(0x1026u),
    TEMP_SCREEN(0x1030u),
    COOL_SCREEN(0x1032u),
    HEATER_0_TEMP_ENTER(0x1034u),
    HEATER_1_TEMP_ENTER(0x1038u),
    HOT_BED_TEMP_ENTER(0x103Au),
    SETTING_SCREEN(0x103Eu),
    SETTING_BACK(0x1040u),
    BED_LEVEL_FUN(0x1044u),
    AXIS_PAGE_SELECT(0x1046u),
    X_AXIS_MOVE(0x1048u),
    Y_AXIS_MOVE(0x104Au),
    Z_AXIS_MOVE(0x104Cu),
    SELECT_EXTRUDER(0x104Eu),
    HEATER_0_LOAD_ENTER(0x1054u),
    FILAMENT_LOAD(0x1056u),
    HEATER_1_LOAD_ENTER(0x1058u),
    SELECT_LANGUAGE(0x105Cu),
    FILAMENT_CHECK(0x105Eu),
    POWER_CONTINUE_PRINT(0x105Fu),
    PRINT_SELECT_MODE(0x1090u),
    X_HOTEND_OFFSET(0x1092u),
    Y_HOTEND_OFFSET(0x1094u),
    Z_HOTEND_OFFSET(0x1096u),
    STORE_MEMORY(0x1098u),
    PRINT_FILE(0x2198u),
    SELECT_FILE(0x2199u),
    CHANGE_PAGE(0x110Eu),
    SET_PRE_NOZZLE_TEMP(0x2200u),
    SET_PRE_BED_TEMP(0x2201u),
    HARDWARE_TEST(0x2202u),
    ERR_CONTROL(0x2203u),
    PRINT_FILES(0x2204u),
    PRINT_CONFIRM(0x2205u);

    companion object {

        private val addressMap = entries.associateBy(NextionDatagramAddressKey::address)
        fun from(address: UShort): NextionDatagramAddressKey {
            return addressMap[address] ?: throw IllegalArgumentException("Unknown address: $address")
        }

    }
}

private object ActionRegistry {

    private val singleWordActions: MutableMap<NextionDatagramAddressKey, MutableMap<UShort, NextionAction>> =
        EnumMap(NextionDatagramAddressKey::class.java)

    init {
        // MAIN_PAGE
        preregisterAction(OpenFileListAction())
        preregisterAction(RemoveFileListMultilineFlagAction())
        preregisterAction(SetFileListMultilineFlagAction())

        // TEMP_SCREEN
        preregisterAction(SetUnitMultiplier1Action())
        preregisterAction(SetUnitMultiplier2Action())
        preregisterAction(SetUnitMultiplier3Action())

        // COOL_SCREEN
        preregisterAction(DisableNozzle0HeatingAction())
        preregisterAction(DisableBedHeatingAction())
        preregisterAction(PreheatPlaAction())
        preregisterAction(PreheatAbsAction())
        preregisterAction(PreheatPetgAction())
        preregisterAction(PreheatTpuAction())

        // SETTING_SCREEN
        preregisterAction(TriggerAutohomeAction())
        preregisterAction(StopMotorsAction())
        preregisterAction(NavigateToPretempPageAction())
        preregisterAction(NavigateToPrefilamentPageAction())

        // SETTING_BACK
        preregisterAction(SaveSettingsAction())

        // BED_LEVEL_FUN
        preregisterAction(RequestTemperaturesAction())
        preregisterAction(LcdRecoveryAction())

        // AXIS_PAGE_SELECT
        preregisterAction(HomeAllAction())
        preregisterAction(HomeXAction())
        preregisterAction(HomeYAction())
        preregisterAction(HomeZAction())

        // X_AXIS_MOVE
        preregisterAction(MoveXAxisPlusAction())
        preregisterAction(MoveXAxisMinusAction())

        // Y_AXIS_MOVE
        preregisterAction(MoveYAxisPlusAction())
        preregisterAction(MoveYAxisMinusAction())

        // Z_AXIS_MOVE
        preregisterAction(MoveZAxisPlusAction())
        preregisterAction(MoveZAxisMinusAction())

        // FILAMENT_LOAD
        preregisterAction(UnloadFilamentAction())
        preregisterAction(LoadFilamentAction())

        // HARDWARE_TEST
        preregisterAction(DetectHardwareTestAction())
    }

    private fun preregisterAction(action: NextionAction) {
        require(action.data.size == 1) { "Only actions with a single data word can be preregistered" }
        singleWordActions.computeIfAbsent(action.address) { HashMap() }[action.data.first()] = action
    }

    fun getAction(address: NextionDatagramAddressKey, data: List<UShort>): NextionAction? {
        if (data.size == 1) {
            singleWordActions[address]?.get(data.first())?.let {
                return it
            }
        }

        if (data.size == 1) {
            val dataWord = data.first()
            return when (address) {
                NextionDatagramAddressKey.HEATER_0_TEMP_ENTER -> SetNozzle0TemperatureAction(dataWord)
                NextionDatagramAddressKey.HOT_BED_TEMP_ENTER -> SetBedTemperatureAction(dataWord)
                NextionDatagramAddressKey.HEATER_0_LOAD_ENTER -> SetPrefilamentLoadLengthAction(dataWord)
                NextionDatagramAddressKey.HEATER_1_LOAD_ENTER -> SetPrefilamentLoadSpeedAction(dataWord)
                else -> null
            }
        }
        return null
    }

}

fun swapEndianess(word: UShort): UShort {
    return (((word and 0xFF00u).toInt() ushr 8) or ((word and 0x00FFu).toInt() shl 8)).toUShort()
}
