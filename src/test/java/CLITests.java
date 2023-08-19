import aktorrent.AKTorrent;
import aktorrent.CLI;
import aktorrent.FileInfo;
import aktorrent.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CLITests {
    private static final String FILENAME = "test_file.mp4";

    private static final String FILENAME_2 = "test_file_2.mp4";

    @Test
    public void seedFileTest() throws IOException {
        File file = new File(getClass().getResource(FILENAME).getFile());
        InputStream in = new ByteArrayInputStream(("1\n " + FILENAME + "\n").getBytes());
        PrintStream out = new PrintStream(new ByteArrayOutputStream(1024));
        AKTorrent client = mock(AKTorrent.class);
        CLI sut = new CLI(in, out, client);
        sut.start();
        verify(client).seedFile(file);
    }

    @Test
    public void displayMenu() throws IOException {
        InputStream in = new ByteArrayInputStream("".getBytes());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        AKTorrent node = mock(AKTorrent.class);
        CLI sut = new CLI(in, out, node);
        sut.start();
        Scanner scanner = new Scanner(bytes.toString());
        scanner.useDelimiter("\n");
        assertEquals(scanner.next(), CLI.WELCOME_MESSAGE);
        assertEquals(scanner.next(), CLI.MAIN_MENU);
    }

    @Test
    public void canDownloadFile() throws IOException {
        String command = "2\n " + FILENAME;
        InputStream in = new ByteArrayInputStream(command.getBytes());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);

        AKTorrent node = mock(AKTorrent.class);

        CLI sut = new CLI(in, out, node);
        sut.start();

        Scanner scanner = new Scanner(bytes.toString());
        scanner.useDelimiter("\n");

        File file = new File(AKTorrent.class.getResource("/" + FILENAME).getFile());

        verify(node).downloadFile(FileUtils.getFileInfo(file));
    }

    @Test
    public void displayAvailableFiles() throws IOException {
        String command = "3\n";
        InputStream in = new ByteArrayInputStream(command.getBytes());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);

        AKTorrent node = mock(AKTorrent.class);

        FileInfo fileInfo_A = new FileInfo(FILENAME, 100, 100);
        FileInfo fileInfo_B = new FileInfo(FILENAME_2, 100, 100);

        Set<FileInfo> files = Set.of(fileInfo_A, fileInfo_B).stream()
                .sorted(Comparator.comparing(FileInfo::getFilename))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        when(node.getAvailableFiles()).thenReturn(CompletableFuture.completedFuture(files));

        CLI sut = new CLI(in, out, node);
        sut.start();

        Scanner scanner = new Scanner(bytes.toString());
        scanner.useDelimiter("\n");

        scanner.nextLine();
        scanner.nextLine();
        assertEquals(FILENAME, scanner.nextLine());
        assertEquals(FILENAME_2, scanner.nextLine());
    }

}
