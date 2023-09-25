package com.alyokaz.aktorrent.cli;

import com.alyokaz.aktorrent.AKTorrent;
import com.alyokaz.aktorrent.fileservice.FileInfo;
import com.alyokaz.aktorrent.fileservice.exceptions.SeedFileException;
import com.alyokaz.aktorrent.peerservice.exceptions.ContactBeaconException;
import com.alyokaz.aktorrent.peerservice.exceptions.PingPeerException;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class CLI {

    public static final String INDENT = "    ";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RESET = "\u001B[0m";

    public static final String WELCOME_MESSAGE = GREEN + "Welcome to AKTorrent" + RESET;
    public static final String MAIN_MENU = """
                        Options:\040\040
                                    1 - Seed File\040\040\040
                                    2 - Display Available Files\040\040\040
                                    3 - Connect to Peer\040\040\040
                                    4 - Exit""";
    public static final String INPUT_PROMPT = "Enter path of file to seed>";
    public static final String DOWNLOAD_INPUT_PROMPT = "Select number to download>" ;
    public static final String NOT_A_NUMBER_ERROR = "Please select a menu number";
    public static final String NON_EXISTENT_MENU_OPTION = RED + "%s is not an available option" + RESET + "%n" ;
    public static final String FILE_NOT_FOUND = RED + "Could not locate file: " + RESET;
    public static final String INPUT_PEER_ADDRESS_PROMPT = "Enter hostname and port of peer>";
    public static final String PEER_CONNECTED_MESSAGE = GREEN + "Peer Connection Successful at " + YELLOW + "%s:%s" + RESET + "%n" ;
    public static final String PEER_CONNECTION_FAILED = RED + "Failed to connect to peer" + RESET;
    public static final String SEED_FILE_EXCEPTION = RED + "Failed to seed file" + RESET;
    public static final String BAD_ADDRESS_FORMAT_ERROR = RED + "Bad address format. Please enter <hostname> <port number>" + RESET;
    public static final String NO_FILES_AVAILABLE = RED + "No files are available for download" + RESET;
    public static final String DOWNLOAD_PROGRESS = YELLOW + "Downloading %5.2f%%" + RESET + "\r" ;
    public static final String DOWNLOAD_COMPLETE = GREEN + "Downloading of " + YELLOW + "%s " + YELLOW + GREEN + "complete"  + RESET + "%n";
    public static final String SERVER_STARTUP_MESSAGE = "Server started on port: " + YELLOW + "%s" + RESET + "%n";
    public static final String BEACON_EXCEPTION = RED + "Could not contact Beacon" + RESET;
    public static final String COMMAND_PROMPT = YELLOW + "AKTorrent:>" + RESET;
    public static final String OPTION_PROMPT = "Select Option>";
    public static final String AVAILABLE_FILES_BANNER = YELLOW + "*** AVAILABLE FILES ***" + RESET;
    public static final String SUCCESSFUL_SEED_MESSAGE = GREEN + "File " + YELLOW + "%s " + GREEN + " seeded successfully" + RESET + "%n";


    private final InputStream inputStream;
    private final PrintStream outputStream;
    private final AKTorrent node;


    public CLI(InputStream inputStream, PrintStream outputStream, AKTorrent node) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.node = node;
    }

    public void start() throws IOException {
        outputStream.println(WELCOME_MESSAGE);
        outputStream.printf(SERVER_STARTUP_MESSAGE, node.getAddress().getPort());
        outputStream.println("");
        outputStream.println(MAIN_MENU);
        outputStream.print(OPTION_PROMPT);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        boolean running = true;
        while (running && (line = reader.readLine()) != null) {
            int selection;
            try {
                selection = Integer.parseInt(line);
                switch (selection) {
                    case 1 -> processSeedFile(reader, outputStream);
                    case 2 -> processDisplayFiles(reader, outputStream);
                    case 3 -> processAddPeer(reader, outputStream);
                    case 4 -> running = false;
                    default -> processDefault(outputStream, selection);
                }
            } catch (NumberFormatException e) {
                outputStream.println(NOT_A_NUMBER_ERROR);
            } catch (SeedFileException e) {
                outputStream.println(SEED_FILE_EXCEPTION);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ContactBeaconException e) {
                outputStream.println(BEACON_EXCEPTION);
            }
            if(running) {
                outputStream.println("");
                outputStream.println(MAIN_MENU);
                outputStream.print(OPTION_PROMPT);
            } else {
                outputStream.println("Bye.");
            }
        }
        node.shutDown();
    }

    private void processAddPeer(BufferedReader reader, PrintStream outputStream) throws IOException, PingPeerException {
        outputStream.print(CLI.INPUT_PEER_ADDRESS_PROMPT);
        String[] address;
        while(!(address = reader.readLine().split(" "))[0].equals("")) {
            if (address.length == 2) {
                Thread spinnerThread = new Thread(new Spinner(outputStream));
                try {
                    spinnerThread.start();
                    if (node.addPeer(new InetSocketAddress(address[0], Integer.parseInt(address[1])))) {
                        spinnerThread.interrupt();
                        outputStream.printf("\r" + CLI.PEER_CONNECTED_MESSAGE, address[0], address[1]);
                        break;
                    } else {
                        spinnerThread.interrupt();
                        outputStream.println("\r" + CLI.PEER_CONNECTION_FAILED);
                    }
                } catch (IllegalArgumentException e) {
                    spinnerThread.interrupt();
                    outputStream.println("\r" + CLI.BAD_ADDRESS_FORMAT_ERROR + " : " + e.getMessage());
                }
            } else {
                outputStream.println(CLI.BAD_ADDRESS_FORMAT_ERROR);
            }
            outputStream.print(CLI.INPUT_PEER_ADDRESS_PROMPT);
        }
    }


    public static class Spinner implements Runnable {

        private final PrintStream outputStream;
        private final int SPEED = 100;
        private final List<Character> blades = List.of('/', '-', '\\', '-');

        public Spinner(PrintStream outputStream) {this.outputStream = outputStream;}

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    for(char blade: blades) {
                        spin(blade);
                    }
                }
            } catch(InterruptedException e) {
                //no action needed
            }
        }

        private void spin(char blade) throws InterruptedException {
            outputStream.printf("\r" + GREEN + "Connecting " + blade + RESET);
            Thread.sleep(SPEED);
        }
    }

    private void processDefault(PrintStream outputStream, Integer selection) {
        outputStream.println(selection + NON_EXISTENT_MENU_OPTION);
    }

    private void processDisplayFiles(BufferedReader reader, PrintStream outputStream) throws IOException, InterruptedException {
        List<FileInfo> files = new ArrayList<>(node.getAvailableFiles());
        if(files.size() > 0) {
            outputStream.println(AVAILABLE_FILES_BANNER);
            IntStream.range(0, files.size()).forEach(i -> outputStream.println((i + 1) + ": " + files.get(i).getFilename()));
            String line;
            int fileNumber;
            outputStream.print(DOWNLOAD_INPUT_PROMPT);
            while(!(line = reader.readLine()).equals("")) {
                fileNumber = Integer.parseInt(line) - 1;
                if (fileNumber + 1 > files.size()) {
                    outputStream.printf(CLI.NON_EXISTENT_MENU_OPTION, fileNumber);
                    outputStream.print(DOWNLOAD_INPUT_PROMPT);
                } else {
                    FileInfo fileInfo = files.get(fileNumber);
                    node.downloadFile(fileInfo);
                    double progress = 0;
                    while (progress < 1) {
                        progress = node.getProgressOfDownload(fileInfo.getFilename());
                        outputStream.printf(DOWNLOAD_PROGRESS, progress * 100);
                        Thread.sleep(100);
                    }
                    outputStream.printf(DOWNLOAD_COMPLETE, fileInfo.getFilename());
                    break;
                }
            }
        } else {
            outputStream.println(CLI.NO_FILES_AVAILABLE);
        }
    }

    private void processSeedFile(BufferedReader reader, PrintStream out) throws IOException, SeedFileException {
        out.print(INPUT_PROMPT);
        String filename;
        while(!(filename = reader.readLine().trim()).equals("")) {
            File file = new File(filename);
            if (file.exists()) {
                node.seedFile(file);
                out.printf(CLI.SUCCESSFUL_SEED_MESSAGE, filename);
                break;
            } else {
                outputStream.println(CLI.FILE_NOT_FOUND + filename);
                out.print(INPUT_PROMPT);
            }
        }
    }
}
