import aktorrent.AKTorrent;
import aktorrent.CLI;
import aktorrent.FileInfo;
import aktorrent.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CLITests {
    private static final String FILENAME = "test_file.mp4";


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

}
