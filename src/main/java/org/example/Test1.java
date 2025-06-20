/**
 * Test1.java
 *
 * Author: Vera Shchukina (@codebyvera)
 * Created: [09-05-2025]
 *
 * Description:
 * This Java application connects to a Pfeiffer TPG-361 vacuum gauge via a selected COM port.
 * It provides three operational modes:
 *   1 - Fixed interval of 100 milliseconds
 *   2 - Fixed interval of 1 second
 *   3 - Manual interval (must be greater than 1100 milliseconds for safe operation)
 *
 * The application logs pressure readings to a text file and visualizes them in real time.
 *
 * Note:
 * - The application prints all available serial ports and allows the user to choose one.
 * - It is recommended to use mode 3 only with an interval >= 1100 ms to avoid buffer issues.
 */

package org.example;
import com.fazecast.jSerialComm.SerialPort;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class Test1 {
    public static void main(String[] args) throws IOException, InterruptedException {

        Scanner scanner = new Scanner(System.in);
        AtomicInteger interval= new AtomicInteger(0);
        AtomicInteger mode = new AtomicInteger(0);
        AtomicLong startTime = new AtomicLong(0);

        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            System.out.println("Available ports: " + port.getSystemPortName());
        }

        System.out.println("Enter the port number: ");
        String port = "COM";
        port = port + scanner.nextLine();
        System.out.println(port);

        SerialPort port1 = SerialPort.getCommPort(port);

        port1.setBaudRate(9600);
        port1.setNumDataBits(8);
        port1.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port1.setParity(SerialPort.NO_PARITY);
        port1.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000,0);


        System.out.println("Select mode: \n" +
                "1 - interval 100ms \n" +
                "2 - interval 1s \n" +
                "3 - manual interval (more 1s)");
        int inputMode = scanner.nextInt();
        mode.set(inputMode);

        if (mode.get() !=1 && mode.get() !=2 && mode.get() !=3){
            System.out.println("Error: an incorrect mode has been selected. Valid values are: 1, 2, 3.");
            return;
        }

        if(port1.isOpen()){
            port1.closePort();
        }
        if (port1.openPort()) {

            System.out.println("Port: " + port1.getSystemPortName() + " is open ");

            try (FileWriter writer1 = new FileWriter("output.txt", true)) {
                writer1.write("#" + "    [DateTime]"  + "             Pressure [mBar]" + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (mode.get() == 1){
                enableMode1(port1);
            } else if (mode.get() == 2){
                enableMode2(port1);
            } else if (mode.get() == 3){
                System.out.println("Enter interval in milliseconds: ");
                int input = scanner.nextInt();
                if (input < 1100){
                    System.out.println("Error: The interval is too short");
                    return;
                }
                try{
                    interval.set(input);
                }catch (NumberFormatException e){
                    System.out.println("Error: enter an integer number");
                }
                enableMode3(port1);
            }


            PressureChartTest chart = new PressureChartTest("Life pressure chart", "pressure, mBar");

            InputStream inputStream = port1.getInputStream();
            System.out.println("Available bytes before reading: " + port1.bytesAvailable());

            while (port1.bytesAvailable() > 0) {
                port1.getInputStream().read();
            }

            Thread rederThread = new Thread(()-> {
                try {

                    StringBuilder stringBuilder = new StringBuilder();
                    while (true) {

                        LocalDateTime dateTime = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                        String formattedDateTime = dateTime.format(formatter);

                        if (mode.get() == 1 || mode.get() == 2) {
                            int b = inputStream.read();
                            if (b == -1) {
                                System.out.println("end of stream reached");
                                break;
                            }

                            if (b == '\n') {
                                String line = stringBuilder.toString().trim();
                                stringBuilder.setLength(0);
                                System.out.println("Line complete: " + line);

                                if (!line.isEmpty() && !line.equalsIgnoreCase("0x06")) {
                                    System.out.println("[" + formattedDateTime + "]   " + line + "\n");

                                    try (FileWriter writer = new FileWriter("output.txt", true)) {
                                        writer.write("[" + formattedDateTime + "]   " + line + "\n");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    String[] parts = line.split(",");
                                    if (parts.length >= 2) {
                                        try {
                                            double value = Double.parseDouble(parts[1].trim());
                                            chart.addData(value);
                                        } catch (NumberFormatException e) {
                                            System.out.println("Failed to parse value: " + parts[1]);
                                        }
                                    } else {
                                        System.out.println("Invalid line format: " + line);
                                    }
                                }

                            } else {
                                stringBuilder.append((char) b);
                            }


                        } else if (mode.get() == 3) {
                            long newStartTime = System.currentTimeMillis();
                            startTime.set(newStartTime);

                            clearBuffer(port1);

                            while (true) {

                                int b = inputStream.read();
                                if (b == -1) {
                                    System.out.println("end of stream reached");
                                    break;
                                }

                                if (b == '\n') {
                                    String line = stringBuilder.toString().trim();
                                    stringBuilder.setLength(0);
                                    System.out.println("Line complete: " + line);

                                    if (!line.isEmpty() && !line.trim().equalsIgnoreCase("0x06")) {
                                        System.out.println("[" + formattedDateTime + "]   " + "Measurement:   " + line);
                                        try (FileWriter writer = new FileWriter("output.txt", true)) {
                                            writer.write("[" + formattedDateTime + "]   " + line + "\n");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        String[] parts = line.split(",");
                                        if (parts.length >= 2) {
                                            try {
                                                double value = Double.parseDouble(parts[1].trim());
                                                chart.addData(value);
                                            } catch (NumberFormatException e) {
                                                System.out.println("Failed to parse value: " + parts[1]);
                                            }
                                        } else {
                                            System.out.println("Invalid format: " + line);
                                        }
                                        break;
                                    }
                                } else {
                                    stringBuilder.append((char) b);
                                }
                            }
                            long operationsTime = System.currentTimeMillis() - startTime.get();
                            long sleepTime = interval.get() - operationsTime;
                            System.out.println(operationsTime);
                            System.out.println(sleepTime);
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            rederThread.start();

            System.out.println("Port: " + port1.getSystemPortName() + " is closed");
        } else {
            System.out.println("Port could not be found");
        }
    }


    public static void enableMode1 (SerialPort comPort) throws IOException, InterruptedException {
        String command = "COM,0\r";//waiting 100ms
        comPort.getOutputStream().write(command.getBytes());
        comPort.getOutputStream().flush();

        InputStream inputStream = comPort.getInputStream();

        boolean ackReceive = false;
        long startTime = System.currentTimeMillis();
        while (!ackReceive && System.currentTimeMillis() - startTime < 3000){
            if (inputStream.available() > 0){
                int c = inputStream.read();
                if (c == 0x06){
                    System.out.println("Received ACK (0x06)");
                    ackReceive = true;
                    break;
                } else {
                    System.out.println("Skipped byte: " + String.format("0x%02X", c));
                }
            }
        }
        if (!ackReceive) {
            System.out.println("ACK not received within timeout");
            return;
        }

        Thread.sleep(100);
        while (inputStream.available() > 0){
            int skipped = inputStream.read();
            System.out.println("Clearing: " + String.format("0x%02X", skipped));
        }


    }
    public static void enableMode2 (SerialPort comPort) throws IOException, InterruptedException {
        String command = "COM,1\r";//waiting 1s
        comPort.getOutputStream().write(command.getBytes());
        comPort.getOutputStream().flush();

        InputStream inputStream = comPort.getInputStream();

        while (inputStream.available() > 0){
            inputStream.read();
        }

        int ackByte = comPort.getInputStream().read();
        if (ackByte == 0x06) {
            System.out.println("Received ACK (0x06)");
        } else {
            System.out.println("ACK was not received");
        }
        while (inputStream.available() > 0){
            inputStream.read();
        }
    }
    public static void enableMode3 (SerialPort comPort) throws IOException, InterruptedException {
        String command = "COM\r"; //waiting with parameter
        comPort.getOutputStream().write(command.getBytes());
        comPort.getOutputStream().flush();
        Thread.sleep(500);
    }

    public static void clearBuffer(SerialPort serialPort) throws IOException {
        InputStream inputStream = serialPort.getInputStream();
        while (inputStream.available() >0){
            inputStream.read();
        }
        System.out.println("Buffer cleared");
    }

}
