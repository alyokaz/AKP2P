package com.alyokaz.aktorrent.cli;

import com.alyokaz.aktorrent.AKTorrent;
import com.alyokaz.aktorrent.FileInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class CLI {

    public static final String WELCOME_MESSAGE = "Welcome to AKTorrent";
    public static final String MAIN_MENU = "1: seed, 2: see files, 3: add peer";
    public static final String INPUT_PROMPT = "Input Path:";
    public static final String DOWNLOAD_INPUT_PROMPT = "Input file number: ";
    public static final String NOT_A_NUMBER_ERROR = "Please select a menu number.";
    public static final String NON_EXISTENT_MENU_OPTION = " is not an available option";
    public static final String FILE_NOT_FOUND = "Could not locate file: ";
    public static final String INPUT_PEER_ADDRESS_PROMPT = "Input peer hostname and port: ";
    public static final String PEER_CONNECTED_MESSAGE = "Peer Connected";
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
        outputStream.println(MAIN_MENU);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            int selection;
            try {
                selection = Integer.parseInt(line);
                switch (selection) {
                    case 1 -> processSeedFile(reader, outputStream);
                    case 2 -> processDisplayFiles(reader, outputStream);
                    case 3 -> processAddPeer(reader, outputStream);
                    default -> processDefault(outputStream, selection);
                }
            } catch (NumberFormatException e) {
                outputStream.println(NOT_A_NUMBER_ERROR);
            }
            outputStream.println(MAIN_MENU);
        }
        node.shutDown();
    }

    private void processAddPeer(BufferedReader reader, PrintStream outputStream) throws IOException {
        outputStream.println(CLI.INPUT_PEER_ADDRESS_PROMPT);
        String[] address = reader.readLine().split(" ");
        node.addPeer(address[0], Integer.parseInt(address[1]));
        outputStream.println(CLI.PEER_CONNECTED_MESSAGE);
    }

    private void processDefault(PrintStream outputStream, Integer selection) {
        outputStream.println(selection + NON_EXISTENT_MENU_OPTION);
    }

    private void processDisplayFiles(BufferedReader reader, PrintStream outputStream) throws IOException {
        List<FileInfo> files = new ArrayList<>(node.getAvailableFiles());
        IntStream.range(0, files.size()).forEach(i -> outputStream.println((i + 1) + ": " + files.get(i).getFilename()));
        outputStream.println(DOWNLOAD_INPUT_PROMPT);
        String line = reader.readLine();
        if (line == null) return;
        int fileNumber = Integer.parseInt(line) - 1;
        node.downloadFile(files.get(fileNumber));
    }

    private void processSeedFile(BufferedReader reader, PrintStream out) throws IOException {
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