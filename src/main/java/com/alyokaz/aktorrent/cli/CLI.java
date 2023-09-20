package com.alyokaz.aktorrent.cli;

import com.alyokaz.aktorrent.AKTorrent;
import com.alyokaz.aktorrent.fileservice.FileInfo;
import com.alyokaz.aktorrent.fileservice.exceptions.SeedFileException;
import com.alyokaz.aktorrent.peerservice.exceptions.PingPeerException;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class CLI {

    public static final String WELCOME_MESSAGE = "Welcome to AKTorrent";
    public static final String MAIN_MENU = "1: seed, 2: see files, 3: add peer, 4: exit";
    public static final String INPUT_PROMPT = "Input Path:";
    public static final String DOWNLOAD_INPUT_PROMPT = "Input file number: ";
    public static final String NOT_A_NUMBER_ERROR = "Please select a menu number.";
    public static final String NON_EXISTENT_MENU_OPTION = " is not an available option";
    public static final String FILE_NOT_FOUND = "Could not locate file: ";
    public static final String INPUT_PEER_ADDRESS_PROMPT = "Input peer hostname and port: ";
    public static final String PEER_CONNECTED_MESSAGE = "Peer Connected";
    public static final String PEER_CONNECTION_FAILED = "Failed to connect to peer";
    public static final String SEED_FILE_EXCEPTION = "Failed to seed file";
    public static final String BAD_ADDRESS_FORMAT_ERROR = "Bad address format. Please enter <Hostname> <port>";
    public static final String NO_FILES_AVAILABLE = "No files are available for download";
    public static final String DOWNLOAD_PROGRESS = "Downloading %5.2f%%\r";
    public static final String DOWNLOAD_COMPLETE = "Downloading of %s complete%n";
    public static final String SERVER_STARTUP_MESSAGE = "Server started on port: %s%n";

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
        outputStream.println(MAIN_MENU);
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
            }
            outputStream.println(MAIN_MENU);
        }
        node.shutDown();
    }

    private void processAddPeer(BufferedReader reader, PrintStream outputStream) throws IOException, PingPeerException {
        outputStream.println(CLI.INPUT_PEER_ADDRESS_PROMPT);
        String[] address = reader.readLine().split(" ");
        if(address.length == 2) {
            if (node.addPeer(new InetSocketAddress(address[0], Integer.parseInt(address[1]))))
                outputStream.println(CLI.PEER_CONNECTED_MESSAGE);
            else
                outputStream.println(CLI.PEER_CONNECTION_FAILED);
        } else {
            outputStream.println(CLI.BAD_ADDRESS_FORMAT_ERROR);
        }
    }

    private void processDefault(PrintStream outputStream, Integer selection) {
        outputStream.println(selection + NON_EXISTENT_MENU_OPTION);
    }

    private void processDisplayFiles(BufferedReader reader, PrintStream outputStream) throws IOException, InterruptedException {
        List<FileInfo> files = new ArrayList<>(node.getAvailableFiles());
        IntStream.range(0, files.size()).forEach(i -> outputStream.println((i + 1) + ": " + files.get(i).getFilename()));
        if(files.size() > 0) {
            outputStream.println(DOWNLOAD_INPUT_PROMPT);
            String line = reader.readLine();
            if (line == null) return;
            int fileNumber = Integer.parseInt(line) - 1;
            if(fileNumber + 1 > files.size())
                outputStream.println(CLI.NON_EXISTENT_MENU_OPTION);
            else {
                FileInfo fileInfo = files.get(fileNumber);
                node.downloadFile(fileInfo);
                double progress = 0;
                while(progress < 1) {
                    progress = node.getProgressOfDownload(fileInfo.getFilename());
                    outputStream.printf(DOWNLOAD_PROGRESS, progress * 100);
                    Thread.sleep(100);
                }
                outputStream.printf(DOWNLOAD_COMPLETE, fileInfo.getFilename());
            }
        } else {
            outputStream.println(CLI.NO_FILES_AVAILABLE);
        }
    }

    private void processSeedFile(BufferedReader reader, PrintStream out) throws IOException, SeedFileException {
        out.println(INPUT_PROMPT);
        String filename = reader.readLine().trim();
        File file = new File(filename);
        if (file.exists()) {
            node.seedFile(file);
        } else {
            outputStream.println(CLI.FILE_NOT_FOUND + filename);
        }
    }
}
