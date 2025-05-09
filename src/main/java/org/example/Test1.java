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


public class Test1 {
    public static void main(String[] args) throws IOException, InterruptedException {

        Scanner scanner = new Scanner(System.in);
        int interval=0;


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
        int mode = scanner.nextInt();

        if (mode !=1 && mode !=2 && mode !=3){
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

            if (mode == 1){
                enableMode1(port1);
            } else if (mode == 2){
                enableMode2(port1);
            } else if (mode == 3){
                System.out.println("Enter interval in milliseconds: ");
                interval = scanner.nextInt();
                if (interval < 1100){
                    System.out.println("Error: The interval is too short");
                    return;
                }
                enableMode3(port1);
            }


            PressureChartTest chart = new PressureChartTest("Life pressure chart", "pressure, mBar");

            InputStream inputStream = port1.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (true) {

                LocalDateTime dateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                String formattedDateTime = dateTime.format(formatter);

                if (mode == 1 || mode == 2) {

                    LocalDateTime dateTime1 = LocalDateTime.now();
                    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                    String formattedDateTime1 = dateTime1.format(formatter1);

                    String line = reader.readLine();

                    if (line != null && !line.isEmpty() && !line.trim().equalsIgnoreCase("ACK")) {
                        System.out.println(formattedDateTime1);
                        System.out.println("[" + formattedDateTime + "]   " + "Measurement:   " + line);
                        try (FileWriter writer = new FileWriter("output.txt", true)) {
                            writer.write("[" + formattedDateTime + "]   " + line + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String[] parts = line.trim().split(",");
                        if (parts.length >= 2) {
                            String valueStr = parts[1].trim();
                            double value = Double.parseDouble(valueStr);
                            chart.addData(value);
                        } else {
                            System.out.println("Неверный формат строки: " + line);
                        }
                    }
                } else if (mode == 3) {

                    long startTime = System.currentTimeMillis();

                    clearBuffer(port1);

                    String line = reader.readLine();
                    if (line != null && !line.isEmpty() && !line.trim().equalsIgnoreCase("ACK")) {
                        System.out.println("[" + formattedDateTime + "]   " + "Measurement:   " + line);
                        try (FileWriter writer = new FileWriter("output.txt", true)) {
                            writer.write("[" + formattedDateTime + "]   " + line + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String[] parts = line.trim().split(",");
                        if (parts.length >= 2) {
                            String valueStr = parts[1].trim();
                            double value = Double.parseDouble(valueStr);
                            chart.addData(value);
                        } else {
                            System.out.println("Неверный формат строки: " + line);
                        }
                    }
                    long operationsTime = System.currentTimeMillis() - startTime;
                    long sleepTime = interval - operationsTime;
//                        Debug: operation and sleep timing (used for performance analysis)
//                    System.out.println(operationsTime);
//                    System.out.println(sleepTime);
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }

            port1.closePort();
            System.out.println("Port: " + port1.getSystemPortName() + " is closed");
        } else {
            System.out.println("Port could not be found");
        }
    }


    public static void enableMode1 (SerialPort comPort) throws IOException, InterruptedException {
        String command = "COM,0\r";//waiting 100ms
        comPort.getOutputStream().write(command.getBytes());
        comPort.getOutputStream().flush();
        Thread.sleep(500);
    }
    public static void enableMode2 (SerialPort comPort) throws IOException, InterruptedException {
        String command = "COM,1\r";//waiting 1s
        comPort.getOutputStream().write(command.getBytes());
        comPort.getOutputStream().flush();
        Thread.sleep(500);
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
