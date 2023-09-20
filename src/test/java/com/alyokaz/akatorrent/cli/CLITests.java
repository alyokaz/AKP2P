package com.alyokaz.akatorrent.cli;

import com.alyokaz.aktorrent.AKTorrent;
import com.alyokaz.aktorrent.cli.CLI;
import com.alyokaz.aktorrent.fileservice.FileInfo;
import com.alyokaz.aktorrent.fileservice.exceptions.SeedFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CLITests {
    private static final String FILENAME = "test_file.mp4";
    private static final String FILENAME_2 = "test_file_2.mp4";
    private ByteArrayOutputStream bytes;
    private PrintStream out;
    private AKTorrent node;

    @BeforeEach
    public void setUp() {
        bytes = new ByteArrayOutputStream();
        out = new PrintStream(bytes);
        node = mock(AKTorrent.class);
    }

    @Test
    public void seedFileTest() throws IOException, SeedFileException {
        String command = "1\n " + this.getClass().getResource("/" + FILENAME).getPath() + "\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> assertEquals(CLI.INPUT_PROMPT, scanner.nextLine()), scanner);

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

        String command = "2\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);

        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals("1: " + FILENAME, scanner.nextLine());
            assertEquals("2: " + FILENAME_2, scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void downloadFileByDisplayNumber() throws IOException {
        FileInfo fileInfo = new FileInfo(FILENAME, 100, 100);
        when(node.getAvailableFiles()).thenReturn(Set.of(fileInfo));

        String command = "2\n1";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);

        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals("1: " + FILENAME, scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, scanner.nextLine());
        }, scanner);

        verify(node).downloadFile(fileInfo);
    }

    @Test
    public void recoverFromWrongMenuInput() throws IOException {
        String command = "X";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> assertEquals(CLI.NOT_A_NUMBER_ERROR, scanner.nextLine()), scanner);
    }

    @Test
    public void recoverFromNonExistentMenuOption() throws IOException {
        String command = "99";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() ->
                assertEquals(command + CLI.NON_EXISTENT_MENU_OPTION, scanner.nextLine()), scanner);
    }

    @Test
    public void recoverFromNoFileFoundForSeed() throws IOException {
        String filename = "nonexistentFile.mp4";
        String command = "1\n" + filename;
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals(CLI.INPUT_PROMPT, scanner.nextLine());
            assertEquals(CLI.FILE_NOT_FOUND + filename, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void canAddPeer() throws IOException {
        String command = "3\n" + "localhost " + "4441";
        InputStream in = buildInputStream(command);
        when(node.addPeer(any())).thenReturn(true);
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals(CLI.INPUT_PEER_ADDRESS_PROMPT, scanner.nextLine());
            assertEquals(CLI.PEER_CONNECTED_MESSAGE, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void canHandleSeedFileException() throws IOException, SeedFileException {
        String command = "1\n " + this.getClass().getResource("/" + FILENAME).getPath() + "\n";
        InputStream in = buildInputStream(command);
        doThrow(SeedFileException.class).when(node).seedFile(any());
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals(CLI.INPUT_PROMPT, scanner.nextLine());
            assertEquals(CLI.SEED_FILE_EXCEPTION, scanner.nextLine());
        }, scanner);

    }

    @Test
    public void badPeerAddressFormat() throws IOException {
        String command = "3\n" + "fasdf23423 ds fa sdf d";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals(CLI.INPUT_PEER_ADDRESS_PROMPT, scanner.nextLine());
            assertEquals(CLI.BAD_ADDRESS_FORMAT_ERROR, scanner.nextLine());
        }, scanner);

    }

    @Test
    public void noFilesAvailable() throws IOException {
        String command = "2\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals(CLI.NO_FILES_AVAILABLE, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void noFileForNumber() throws IOException {
        String command = "2\n2";
        InputStream in = buildInputStream(command);
        when(node.getAvailableFiles()).thenReturn(Set.of(new FileInfo(FILENAME, 100, 100)));
        buildAndStartCLI(in, out, node);
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals("1: " + FILENAME, scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, scanner.nextLine());
            assertEquals(CLI.NON_EXISTENT_MENU_OPTION, scanner.nextLine());
        }, scanner);
    }

    @Test
    public void canShutDownFromMenu() throws IOException {
        String command = "4\n";
        InputStream in = buildInputStream(command);
        buildAndStartCLI(in, out, node);

        verify(node).shutDown();
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
        Scanner scanner = new Scanner(bytes.toString());

        assertDisplayOutput(() -> {
            assertEquals("1: " + fileInfo.getFilename(), scanner.nextLine());
            assertEquals(CLI.DOWNLOAD_INPUT_PROMPT, scanner.nextLine());
            assertEquals(String.format(removeEOL(CLI.DOWNLOAD_PROGRESS), first_value * 100), scanner.nextLine());
            assertEquals(String.format(removeEOL(CLI.DOWNLOAD_PROGRESS), second_value * 100), scanner.nextLine());
            assertEquals(String.format(removeEOL(CLI.DOWNLOAD_PROGRESS), third_value * 100), scanner.nextLine());
            assertEquals(String.format(CLI.DOWNLOAD_COMPLETE.replace("%n", ""),
                    fileInfo.getFilename()), scanner.nextLine());
        }, scanner);
    }

    private String removeEOL(String string) {
        return string.replace("\r", "");
    }



    private static InputStream buildInputStream(String command) {
        return new ByteArrayInputStream(command.getBytes());
    }

    private static File buildFile(String filename) {
        return new File(CLITests.class.getResource("/" + filename).getFile());
    }

    private static void buildAndStartCLI(InputStream in, PrintStream out, AKTorrent node) throws IOException {
        CLI cli = new CLI(in, out, node);
        cli.start();
    }

    public static void assertDisplayWelcomeMessage(Scanner scanner) {
        assertEquals(CLI.WELCOME_MESSAGE, scanner.nextLine());
    }

    public static void assertDisplayMenu(Scanner scanner) {
        assertEquals(CLI.MAIN_MENU, scanner.nextLine());
    }

    public static void assertDisplayOutput(Runnable assertions, Scanner scanner) {
        assertDisplayWelcomeMessage(scanner);
        assertDisplayMenu(scanner);
        assertions.run();
        assertDisplayMenu(scanner);
    }



}
