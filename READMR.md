# TPG-361 Pressure Sensor Reader

This is a Java application for real-time data acquisition and visualization from the Pfeiffer TPG 36X pressure gauge. The program supports communication via COM port.

## Features

- Supports three device modes:
    - COM,0 — measurement interval: 100 ms
    - COM,1 — measurement interval: 1 second
    - COM — manual mode: user-defined interval (e.g., more than 1,1 second)
- Real-time pressure graph plotting using JFreeChart
- Data logging to a file (`output.txt`)
- Ability to switch the graph's Y-axis between linear and logarithmic scale

### Manual Mode Note

In manual mode, the minimum safe interval is 1100 milliseconds.  
Setting a lower interval may cause data processing issues or buffer overflows due to the time required for communication and data handling.

## Connection

- Serial (COM) port: specify the correct port name (e.g., `COM3`)

## Requirements

- Java 11 or higher
- [JFreeChart](https://github.com/jfree/jfreechart)
- [jSerialComm](https://fazecast.github.io/jSerialComm/)

## How to Use

1. Launch the application
2. Set the desired mode
3. Toggle Y-axis scale between linear and logarithmic as needed

## File Output

- All readings are appended to output.txt with timestamps
- The first line of the file contains column headers

---

*Developed by Vera Shchukina (@codebyvera)*