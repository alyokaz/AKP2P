package com.alyokaz.akp2p.cli;

import com.alyokaz.akp2p.AKP2P;
import com.alyokaz.akp2p.fileservice.FileInfo;
import com.alyokaz.akp2p.fileservice.exceptions.SeedFileException;
import com.alyokaz.akp2p.peerservice.exceptions.ContactBeaconException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CLITests {
    private static final String FILENAME = "test_file.mp4";
    private static final String FILENAME_2 = "test_file_2.mp4";
    private static final int PORT = 4444;

    public static final String DELIMITER = ">";
    public static final int NUMBER_OF_LIVE_PEERS = 10;
    private ByteArrayOutputStream bytes;
    private PrintStream out;
    private AKP2P node;

    @BeforeEach
    public void setUp() {
        bytes = new ByteArrayOutputStream();
        out = new PrintStream(bytes);
        node = mock(AKP2P.class);
        when(node.getAddress()).thenReturn(new InetSocketAddress(PORT));
        Set<InetSocketAddress> livePeers = mock(HashSet.class);
        when(livePeers.size()).thenReturn(NUMBER_OF_LIVE_PEERS);
        when(node.getLivePeers()).thenReturn(livePeers);
    }

    @Test
    public void seedFileTest() throws IOException, SeedFileException {
        String filepath = this.getClass().getResource("/" + FILENAME).getPath();
        String command = "1\n " + filepath + "\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.INPUT_PROMPT, extractCommandPrompt(scanner));
            assertEquals(String.format(stripEOL(CLI.SUCCESSFUL_SEED_MESSAGE), filepath),
                    scanner.nextLine());
        }, scanner);

        File file = buildFile(FILENAME);
        verify(node).seedFile(file);
    }

    @Test
    public void displayMenu() throws IOException {
        String command = "";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayWelcomeMessage(scanner);
        assertEquals("", scanner.nextLine());
        assertDisplayMenu(scanner);
    }

    @Test
    public void displayAvailableFiles() throws IOException {
        FileInfo fileInfo_A = new FileInfo(FILENAME, 100, 100);
        FileInfo fileInfo_B = new FileInfo(FILENAME_2, 100, 100);

        Set<FileInfo> files = Stream.of(fileInfo_A, fileInfo_B)
                .sorted(Comparator.comparing(FileInfo::getFilename))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        when(node.getAvailableFiles()).thenReturn(files);

        String command = "2\n\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);

        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.AVAILABLE_FILES_BANNER, scanner.nextLine());
            assertEquals(String.format(stripEOL(CLI.DISPLAY_FILE_INFO), 1, FILENAME, fileInfo_A.getSize()), scanner.nextLine());
            assertEquals(String.format(stripEOL(CLI.DISPLAY_FILE_INFO), 2, FILENAME_2, fileInfo_B.getSize()), scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, extractCommandPrompt(scanner));
        }, scanner);
    }

    @Test
    public void downloadFileByDisplayNumber() throws IOException {
        FileInfo fileInfo = new FileInfo(FILENAME, 100, 100);
        when(node.getAvailableFiles()).thenReturn(Set.of(fileInfo));
        when(node.getProgressOfDownload(any())).thenReturn(1.0);

        String command = "2\n1\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);

        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.AVAILABLE_FILES_BANNER, scanner.nextLine());
            assertEquals(String.format(stripEOL(CLI.DISPLAY_FILE_INFO), 1, fileInfo.getFilename(), fileInfo.getSize()),
                    scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, extractCommandPrompt(scanner));
            assertEquals(String.format(stripCr(CLI.DOWNLOAD_PROGRESS), 100.00), scanner.nextLine());
            assertEquals(String.format(stripEOL(CLI.DOWNLOAD_COMPLETE), fileInfo.getFilename()), scanner.nextLine());
        }, scanner);

        verify(node).downloadFile(fileInfo);
    }

    @Test
    public void willDisplayDownloadProgress() throws IOException {
        String command = "2\n1\n";
        InputStream in = buildInputStream(command);
        FileInfo fileInfo = new FileInfo(FILENAME, 100, 100);

        when(node.getAvailableFiles()).thenReturn(Set.of(fileInfo));
        double first_value = 0.50;
        double second_value = 0.75;
        double third_value = 1.00;
        when(node.getProgressOfDownload(any())).thenReturn(first_value, second_value, third_value);

        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.AVAILABLE_FILES_BANNER, scanner.nextLine());
            assertEquals(String.format(stripEOL(CLI.DISPLAY_FILE_INFO), 1, fileInfo.getFilename(), fileInfo.getSize()),
                    scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, extractCommandPrompt(scanner));
            assertEquals(String.format(stripCr(CLI.DOWNLOAD_PROGRESS), first_value * 100), scanner.nextLine());
            assertEquals(String.format(stripCr(CLI.DOWNLOAD_PROGRESS), second_value * 100), scanner.nextLine());
            assertEquals(String.format(stripCr(CLI.DOWNLOAD_PROGRESS), third_value * 100), scanner.nextLine());
            assertEquals(String.format(CLI.DOWNLOAD_COMPLETE.replace("%n", ""),
                    fileInfo.getFilename()), scanner.nextLine());
        }, scanner);
    }

    @Test
    public void recoverFromWrongMenuInput() throws IOException {
        String command = "X";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.NOT_A_NUMBER_ERROR, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void recoverFromNonExistentMenuOption() throws IOException {
        String command = "99";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(command + CLI.NON_EXISTENT_MENU_OPTION, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void recoverFromNoFileFoundForSeed() throws IOException {
        String filename = "nonexistentFile.mp4";
        String command = "1\n" + filename + "\n\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.INPUT_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.FILE_NOT_FOUND + filename, scanner.nextLine());
            assertEquals(CLI.INPUT_PROMPT, extractCommandPrompt(scanner));
        }, scanner);
    }

    @Test
    public void canAddPeer() throws IOException {
        String command = "3\n" + "localhost " + "4441\n";
        InputStream in = buildInputStream(command);
        when(node.addPeer(any())).thenReturn(true);

        Thread spinnerThread = mock(Thread.class);
        SpinnerFactory spinnerFactory = mock(SpinnerFactory.class);
        when(spinnerFactory.buildSpinnerThread()).thenReturn(spinnerThread);

        CLI cli = new CLI(in, out, node, spinnerFactory);
        cli.start();
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(stripEOL(CLI.INPUT_PEER_ADDRESS_PROMPT), scanner.nextLine());
            assertEquals(String.format(stripEOL(CLI.PEER_CONNECTED_MESSAGE), "localhost", "4441"), scanner.nextLine());
        }, scanner);
    }

    @Test
    public void canHandleSeedFileException() throws IOException, SeedFileException {
        String command = "1\n " + this.getClass().getResource("/" + FILENAME).getPath() + "\n";
        InputStream in = buildInputStream(command);
        doThrow(SeedFileException.class).when(node).seedFile(any());
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.INPUT_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.SEED_FILE_EXCEPTION, scanner.nextLine());
        }, scanner);

    }

    @Test
    public void canHandleBeaconExceptionAtInit() throws IOException {
        String command = "2\n";
        InputStream in = buildInputStream(command);
        doThrow(ContactBeaconException.class).when(node).getAvailableFiles();
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.BEACON_EXCEPTION, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void badPeerAddressFormat() throws IOException {
        String command = "3\n" + "fasdf23423 ds fa sdf d\n\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.INPUT_PEER_ADDRESS_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.BAD_ADDRESS_FORMAT_ERROR, scanner.nextLine());
            assertEquals(CLI.INPUT_PEER_ADDRESS_PROMPT, extractCommandPrompt(scanner));
        }, scanner);

    }

    @Test
    public void noFilesAvailable() throws IOException {
        String command = "2\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.NO_FILES_AVAILABLE, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void noFileForNumber() throws IOException {
        String command = "2\n2\n\n";
        InputStream in = buildInputStream(command);
        FileInfo fileInfo = new FileInfo(FILENAME, 100, 100);
        when(node.getAvailableFiles()).thenReturn(Set.of(fileInfo));
        buildAndStartCLI(in, out, node);
        Scanner scanner = buildScanner(bytes);

        assertDisplayOutput(() -> {
            assertEquals(CLI.OPTION_PROMPT, extractCommandPrompt(scanner));
            assertEquals(CLI.AVAILABLE_FILES_BANNER, scanner.nextLine());
            assertEquals(String.format(stripEOL(CLI.DISPLAY_FILE_INFO), 1, fileInfo.getFilename(), fileInfo.getSize()),
                    scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, extractCommandPrompt(scanner));
            assertEquals(String.format(stripEOL(CLI.NON_EXISTENT_MENU_OPTION), 1), scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, extractCommandPrompt(scanner));
        }, scanner);
    }

    @Test
    public void canShutDownFromMenu() throws IOException {
        String command = "4\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);

        verify(node).shutDown();
    }

    private static String stripEOL(String string) {
        return string.replace("%n", "");
    }

    private static String stripCr(String string) {
        return string.replace("\r", "");
    }

    private String extractCommandPrompt(Scanner scanner) {
        return scanner.next() + scanner.findInLine(DELIMITER);
    }

    private static InputStream buildInputStream(String command) {
        return new ByteArrayInputStream(command.getBytes());
    }

    private static File buildFile(String filename) {
        return new File(CLITests.class.getResource("/" + filename).getFile());
    }

    private static void buildAndStartCLI(InputStream in, PrintStream out, AKP2P node) throws IOException {
        CLI cli = new CLI(in, out, node);
        cli.start();
    }

    private Scanner buildScanner(ByteArrayOutputStream bytes) {
        return new Scanner(bytes.toString()).useDelimiter(DELIMITER);
    }

    public static void assertDisplayWelcomeMessage(Scanner scanner) {
        assertDisplayLogo(scanner);
        assertEquals(String.format(stripEOL(CLI.SERVER_STARTUP_MESSAGE), PORT), scanner.nextLine());

    }

    private static void assertDisplayLogo(Scanner scanner) {
        Scanner logoScanner = new Scanner(CLI.WELCOME_MESSAGE);
        while (logoScanner.hasNext()) {
            String logoString = logoScanner.nextLine();
            String outString = scanner.nextLine();
            assertEquals(logoString, outString);
        }

    }


    public static void assertDisplayMenu(Scanner scanner) {
        assertEquals(String.format(stripEOL(CLI.CONNECTED_PEERS_MESSAGE), NUMBER_OF_LIVE_PEERS), scanner.nextLine());
        assertEquals("", scanner.nextLine());
        Scanner menuScanner = new Scanner(CLI.MAIN_MENU);
        while (menuScanner.hasNext()) {
            assertEquals(menuScanner.nextLine(), scanner.nextLine());
        }
        assertEquals("", scanner.nextLine());
    }

    public static void assertDisplayOutput(Runnable assertions, Scanner scanner) {
        assertDisplayWelcomeMessage(scanner);
        assertEquals("", scanner.nextLine());
        assertDisplayMenu(scanner);
        assertions.run();
        assertEquals("", scanner.nextLine());
        assertDisplayMenu(scanner);
    }


}
