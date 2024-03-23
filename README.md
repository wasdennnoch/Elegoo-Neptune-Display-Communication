# ElegooDisplayComms

> [!NOTE]
> Archived in favor of [OpenNept4une](https://github.com/OpenNeptune3D/display_connector), which is a) written in Python for easy deployment, b) is made for the more advanced Neptune 4 series displays (you can just flash the N4 firmware onto your N3 display), and c) is way more developed and has way more features (some of them fully custom!) than this project.

An attempt at reverse-engineering and reimplementing the communication protocol
used by external displays of the Elegoo Neptune 3 (Pro) 3D Printer series.

Currently supports basic reading of commands sent from the display, and manual
typing of commands to send to the display into a command prompt.

The display is of Nextion/TJC brand and is connected to the printer using a
UART serial interface running at a baud rate of 115200. The display is powered
using 5V, but be aware that the serial communication uses 3.3V. If you want to
hijack the display communication yourself, you'll need either a serial port or
something like an FT232RL.

Printer-to-display communication uses a simple, text-based format, such as
`main.nozzletemp.txt="16 / 210"` to set variables or `page hardwaretest` to
switch pages. On the other hand, display-to-printer communication uses a binary
format. For instance, `5A A5 06 83 10 02 01 00 01` means:
- `5A A5`: Fixed Header
- `06`: The following telegram is 6 bytes long
- `83`: Command type [`VAR_ADDR_READ`](https://github.com/NARUTOfzr/Elegoo-Neptune-marlin2.1.1/blob/be2bc9fb3ffd91c45e4ca36fe1473679f99cd708/Marlin/src/lcd/extui/dgus/elegoo/DGUSDisplayDef.h#L47) (pretty much the only command type that's used)
- `10 02`: [`MainPageKey`](https://github.com/NARUTOfzr/Elegoo-Neptune-marlin2.1.1/blob/be2bc9fb3ffd91c45e4ca36fe1473679f99cd708/Marlin/src/lcd/extui/dgus/elegoo/DGUSDisplayDef.h#L326), i.e. something on the main page was pressed (sometimes, keys from other pages can be used too; it's more of a categorization and not a reliable location source)
- `01`:  One word of data will follow
- `00 01`: A one in little endian format; [will open the SD card file list](https://github.com/NARUTOfzr/Elegoo-Neptune-marlin2.1.1/blob/be2bc9fb3ffd91c45e4ca36fe1473679f99cd708/Marlin/src/lcd/extui/dgus/elegoo/DGUSDisplayDef.cpp#L2598).
