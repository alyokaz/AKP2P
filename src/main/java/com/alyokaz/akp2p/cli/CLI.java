package com.alyokaz.akp2p.cli;

import com.alyokaz.akp2p.AKP2P;
import com.alyokaz.akp2p.fileservice.FileInfo;
import com.alyokaz.akp2p.fileservice.exceptions.SeedFileException;
import com.alyokaz.akp2p.peerservice.exceptions.ContactBeaconException;
import com.alyokaz.akp2p.peerservice.exceptions.PingPeerException;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A CLI for an AKP2P non-beacon node.
 */
public class CLI {

    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RESET = "\u001B[0m";

    public static final String WELCOME_MESSAGE = GREEN + """
                        
               _____   ____  __.____________________________\s
              /  _  \\ |    |/ _|\\______   \\_____  \\______   \\
             /  /_\\  \\|      <   |     ___//  ____/|     ___/
            /    |    \\    |  \\  |    |   /       \\|    |   \s
            \\____|__  /____|__ \\ |____|   \\_______ \\____|   \s
                    \\/        \\/                  \\/        \s
                        
            """ + RESET;
    public static final String MAIN_MENU = """
            Options:\040\040
                        1 - Seed File\040\040\040
                        2 - Display Available Files\040\040\040
                        3 - Connect to Peer\040\040\040
                        4 - Exit""";
    public static final String INPUT_PROMPT = "Enter path of file to seed>";
    public static final String DOWNLOAD_INPUT_PROMPT = "Select number to download>";
    public static final String NOT_A_NUMBER_ERROR = "Please select a menu number";
    public static final String NON_EXISTENT_MENU_OPTION = RED + "%s is not an available option" + RESET + "%n";
    public static final String FILE_NOT_FOUND = RED + "Could not locate file: " + RESET;
    public static final String INPUT_PEER_ADDRESS_PROMPT = "Enter hostname and port of peer>";
    public static final String PEER_CONNECTED_MESSAGE = GREEN + "Peer Connection Successful at " + YELLOW + "%s:%s" + RESET + "%n";
    public static final String PEER_CONNECTION_FAILED = RED + "Failed to connect to peer" + RESET;
    public static final String SEED_FILE_EXCEPTION = RED + "Failed to seed file" + RESET;
    public static final String BAD_ADDRESS_FORMAT_ERROR = RED + "Bad address format. Please enter <hostname> <port number>" + RESET;
    public static final String NO_FILES_AVAILABLE = RED + "No files are available for download" + RESET;
    public static final String DOWNLOAD_PROGRESS = YELLOW + "Downloading %5.2f%%" + RESET + "\r";
    public static final String DOWNLOAD_COMPLETE = GREEN + "Downloading of " + YELLOW + "%s " + YELLOW + GREEN + "complete" + RESET + "%n";
    public static final String SERVER_STARTUP_MESSAGE = "Server started on port: " + YELLOW + "%s" + RESET + "%n";
    public static final String BEACON_EXCEPTION = RED + "Could not contact Beacon" + RESET;
    public static final String COMMAND_PROMPT = YELLOW + "AKTorrent:>" + RESET;
    public static final String OPTION_PROMPT = "Select Option>";
    public static final String AVAILABLE_FILES_BANNER = YELLOW + "*** AVAILABLE FILES ***" + RESET;
    public static final String SUCCESSFUL_SEED_MESSAGE = GREEN + "File " + YELLOW + "%s " + GREEN + " seeded successfully" + RESET + "%n";
    public static final String DISPLAY_FILE_INFO = "%d : %s - %,d bytes%n";
    public static final String CONNECTED_PEERS_MESSAGE = "Connected peers - %d%n";

    private final InputStream inputStream;
    private final PrintStream outputStream;
    private final AKP2P node;
    private final SpinnerFactory spinnerFactory;

    public CLI(InputStream inputStream, PrintStream outputStream, AKP2P node) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.node = node;
        this.spinnerFactory = new SpinnerFactory(outputStream);
    }

    public CLI(InputStream inputStream, PrintStream outputStream, AKP2P node, SpinnerFactory spinnerFactory) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.node = node;
        this.spinnerFactory = spinnerFactory;
    }

    /**
     * Starts the CLI.
     *
     * @throws IOException
     */
    public void start() throws IOException {
        outputStream.println(WELCOME_MESSAGE);
        outputStream.printf(SERVER_STARTUP_MESSAGE, node.getAddress().getPort());
        outputStream.println();
        outputStream.printf(CONNECTED_PEERS_MESSAGE, node.getLivePeers().size());
        outputStream.println();
        outputStream.println(MAIN_MENU);
        outputStream.println();
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
            if (running) {
                outputStream.println();
                outputStream.printf(CONNECTED_PEERS_MESSAGE, node.getLivePeers().size());
                outputStream.println();
                outputStream.println(MAIN_MENU);
                outputStream.println();
                outputStream.print(OPTION_PROMPT);
            } else {
                outputStream.println("Bye.");
            }
        }
        node.shutDown();
    }

    /**
     * Adds the peer at the given address to this nodes collection of live peers.
     *
     * @param reader       for input from the command line
     * @param outputStream for output to the command line
     * @throws IOException
     * @throws PingPeerException
     */
    private void processAddPeer(BufferedReader reader, PrintStream outputStream) throws IOException, PingPeerException {
        outputStream.print(CLI.INPUT_PEER_ADDRESS_PROMPT);
        String[] address;
        while (!(address = reader.readLine().split(" "))[0].equals("")) {
            if (address.length == 2) {
                Thread spinnerThread = spinnerFactory.buildSpinnerThread();
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

    /**
     * Handles menu input that doesn't correspond to an available option.
     *
     * @param outputStream for output to the command line
     * @param selection    the value entered
     */
    private void processDefault(PrintStream outputStream, Integer selection) {
        outputStream.println(selection + NON_EXISTENT_MENU_OPTION);
    }

    /**
     * Displays the available files and downloads the file selected.
     *
     * @param reader       for input from the command line
     * @param outputStream for output to the command line
     * @throws IOException
     * @throws InterruptedException
     */
    private void processDisplayFiles(BufferedReader reader, PrintStream outputStream) throws IOException, InterruptedException {
        List<FileInfo> files = new ArrayList<>(node.getAvailableFiles());
        if (files.size() > 0) {
            outputStream.println(AVAILABLE_FILES_BANNER);
            IntStream.range(0, files.size()).forEach(i -> {
                FileInfo fileInfo = files.get(i);
                outputStream.printf(DISPLAY_FILE_INFO, (i + 1), fileInfo.getFilename(), fileInfo.getSize());
            });
            String line;
            int fileNumber;
            outputStream.print(DOWNLOAD_INPUT_PROMPT);
            while (!(line = reader.readLine()).equals("")) {
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

    /**
     * Makes the file at the given path available for download from other peers in the network.
     *
     * @param reader for input from the command line
     * @param out    for output to the command line
     * @throws IOException
     * @throws SeedFileException
     */
    private void processSeedFile(BufferedReader reader, PrintStream out) throws IOException, SeedFileException {
        out.print(INPUT_PROMPT);
        String filename;
        while (!(filename = reader.readLine().trim()).equals("")) {
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
