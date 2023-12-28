package n3p

import n3p.NextionDatagramAddressKey.*

abstract class ReadAction(
    address: NextionDatagramAddressKey,
    data: List<UShort>,
) : NextionAction(
    NextionDatagramCommand.VAR_ADDR_READ,
    address,
    data,
)

abstract class SingleWordAction(
    address: NextionDatagramAddressKey,
    data: UShort,
) : ReadAction(
    address,
    listOf(data),
) {
    override fun toString(): String {
        return "${javaClass.simpleName}()"
    }
}

abstract class NumericInputAction(
    address: NextionDatagramAddressKey,
    data: UShort,
) : ReadAction(
    address,
    listOf(data),
) {
    val value: UShort = swapEndianess(data)

    override fun toString(): String {
        return "${javaClass.simpleName}(input=$value)"
    }
}

/* --- MAIN_PAGE --- */

class OpenFileListAction : SingleWordAction(
    MAIN_PAGE,
    1u,
)

class RemoveFileListMultilineFlagAction : SingleWordAction(
    MAIN_PAGE,
    8u,
)

class SetFileListMultilineFlagAction : SingleWordAction(
    MAIN_PAGE,
    9u,
)

/* --- TEMP_SCREEN --- */

// 1°C / 0.1mm
class SetUnitMultiplier1Action : SingleWordAction(
    TEMP_SCREEN,
    5u,
)

// 5°C / 1mm
class SetUnitMultiplier2Action : SingleWordAction(
    TEMP_SCREEN,
    6u,
)

// 10°C / 10mm
class SetUnitMultiplier3Action : SingleWordAction(
    TEMP_SCREEN,
    7u,
)

/* --- COOL_SCREEN --- */

class DisableNozzle0HeatingAction : SingleWordAction(
    COOL_SCREEN,
    1u,
)

class DisableBedHeatingAction : SingleWordAction(
    COOL_SCREEN,
    2u,
)

class PreheatPlaAction : SingleWordAction(
    COOL_SCREEN,
    9u,
)

class PreheatAbsAction : SingleWordAction(
    COOL_SCREEN,
    10u,
)

class PreheatPetgAction : SingleWordAction(
    COOL_SCREEN,
    11u,
)

class PreheatTpuAction : SingleWordAction(
    COOL_SCREEN,
    12u,
)


/* --- HEATER_0_TEMP_ENTER --- */

class SetNozzle0TemperatureAction(
    data: UShort,
) : NumericInputAction(
    HEATER_0_TEMP_ENTER,
    data,
)

/* --- HOT_BED_TEMP_ENTER --- */

class SetBedTemperatureAction(
    data: UShort,
) : NumericInputAction(
    HOT_BED_TEMP_ENTER,
    data,
)

/* --- SETTING_SCREEN --- */

class TriggerAutohomeAction : SingleWordAction(
    SETTING_SCREEN,
    1u,
)

class StopMotorsAction : SingleWordAction(
    SETTING_SCREEN,
    6u,
)

class NavigateToPretempPageAction : SingleWordAction(
    SETTING_SCREEN,
    9u,
)

class NavigateToPrefilamentPageAction : SingleWordAction(
    SETTING_SCREEN,
    10u,
)

/* --- SETTING_BACK --- */

class SaveSettingsAction : SingleWordAction(
    SETTING_BACK,
    5u,
)

// 0x07
class SetLcdVersionAction(
    data: List<UShort>,
) : ReadAction(
    SETTING_BACK,
    data,
) {
    val version: UShort = data[1]

    override fun toString(): String {
        return "${javaClass.simpleName}(version=$version)"
    }
}

/* --- BED_LEVEL_FUN --- */

class RequestTemperaturesAction : SingleWordAction(
    BED_LEVEL_FUN,
    11u,
)

class LcdRecoveryAction : SingleWordAction(
    BED_LEVEL_FUN,
    12u,
)

/* --- AXIS_PAGE_SELECT --- */

class HomeAllAction : SingleWordAction(
    AXIS_PAGE_SELECT,
    4u,
)

class HomeXAction : SingleWordAction(
    AXIS_PAGE_SELECT,
    5u,
)

class HomeYAction : SingleWordAction(
    AXIS_PAGE_SELECT,
    6u,
)

class HomeZAction : SingleWordAction(
    AXIS_PAGE_SELECT,
    7u,
)

/* --- X_AXIS_MOVE --- */

class MoveXAxisPlusAction : SingleWordAction(
    X_AXIS_MOVE,
    1u,
)

class MoveXAxisMinusAction : SingleWordAction(
    X_AXIS_MOVE,
    2u,
)

/* --- Y_AXIS_MOVE --- */

class MoveYAxisPlusAction : SingleWordAction(
    Y_AXIS_MOVE,
    1u,
)

class MoveYAxisMinusAction : SingleWordAction(
    Y_AXIS_MOVE,
    2u,
)

/* --- Z_AXIS_MOVE --- */

class MoveZAxisPlusAction : SingleWordAction(
    Z_AXIS_MOVE,
    1u,
)

class MoveZAxisMinusAction : SingleWordAction(
    Z_AXIS_MOVE,
    2u,
)

/* --- HEATER_0_LOAD_ENTER --- */

class SetPrefilamentLoadLengthAction(
    data: UShort,
) : NumericInputAction(
    HEATER_0_LOAD_ENTER,
    data,
)

/* --- FILAMENT_LOAD --- */

class UnloadFilamentAction : SingleWordAction(
    FILAMENT_LOAD,
    1u,
)

class LoadFilamentAction : SingleWordAction(
    FILAMENT_LOAD,
    2u,
)

/* --- HEATER_1_LOAD_ENTER --- */

class SetPrefilamentLoadSpeedAction(
    data: UShort,
) : NumericInputAction(
    HEATER_1_LOAD_ENTER,
    data,
)

/* --- HARDWARE_TEST --- */

class DetectHardwareTestAction : SingleWordAction(
    HARDWARE_TEST,
    15u,
)
