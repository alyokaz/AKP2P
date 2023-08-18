import aktorrent.AKTorrent;
import aktorrent.CLI;
import aktorrent.FileInfo;
import aktorrent.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CLITests {
    private static final String FILENAME = "test_file.mp4";


    @Test
    public void seedFileTest() throws IOException {
        File file = new File(getClass().getResource(FILENAME).getFile());
        InputStream in = new ByteArrayInputStream(("seed " + FILENAME).getBytes());
        BufferedOutputStream out = new BufferedOutputStream(new ByteArrayOutputStream(1024));
        AKTorrent client = mock(AKTorrent.class);
        CLI sut = new CLI(in, out, client);
        sut.start();
        verify(client).seedFile(file);
    }
    
}
