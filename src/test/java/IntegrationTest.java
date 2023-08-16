import aktorrent.AKTorrent;
import aktorrent.FileInfo;
import aktorrent.FileUtils;
import aktorrent.PieceContainer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final int NODE_A_PORT = 4441;
    private static final int NODE_B_PORT = 4442;

    private static final int NODE_C_PORT = 4443;

    private static final int NODE_D_PORT = 4444;

    private static final int BUFFER_SIZE = 1000000;

    private static final String FILENAME = "test_file.mp4";

    private static final String FILENAME_2 = "test_file_2.mp4";

    @Test
    public void canSeedAndReceiveFile() throws IOException, InterruptedException {
        File file = new File(getClass().getResource(FILENAME).getFile());
        AKTorrent server = new AKTorrent(NODE_A_PORT);
        server.seedFile(file);

        AKTorrent client = new AKTorrent(NODE_B_PORT);
        client.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_A_PORT));
        client.downloadFile(FileUtils.getFileInfo(file));
        Optional<File> completedFile;
        do {
            completedFile = client.getFile(FILENAME);
        } while(completedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), completedFile.get().toPath()));
        server.shutDown();
    }

    @Test
    public void canDownloadFromTwoPeers() throws IOException {
        File file = new File(getClass().getResource(FILENAME).getFile());

        AKTorrent ak1 = new AKTorrent(NODE_A_PORT);
        AKTorrent ak2 = new AKTorrent(NODE_B_PORT);
        AKTorrent ak3 = new AKTorrent(NODE_C_PORT);
        AKTorrent ak4 = new AKTorrent(NODE_D_PORT);

        ak1.seedFile(file);
        ak2.seedFile(file);
        ak3.seedFile(file);

        ak4.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_A_PORT));
        ak4.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_B_PORT));
        ak4.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_C_PORT));

        ak4.downloadFile(FileUtils.getFileInfo(file));

        Optional<File> downloadedFile;
        do {
            downloadedFile = ak4.getFile(FILENAME);
        } while (downloadedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
        ak1.shutDown();
        ak2.shutDown();
        ak3.shutDown();
        ak4.shutDown();

    }

    @Test
    public void canDownloadFromMultiplePeers() throws IOException {
        final int minPort = 4444;
        final int maxPort = minPort + 10;
        final int clientPort = maxPort + 1;
        File file = new File(getClass().getResource(FILENAME).getFile());

        Set<AKTorrent> nodes = new HashSet<>();
        IntStream.range(minPort, maxPort).forEach(port -> nodes.add(new AKTorrent(port)));
        nodes.forEach(node -> node.seedFile(file));

        AKTorrent client = new AKTorrent(clientPort);

        IntStream.range(minPort, maxPort).forEach(port -> client.addPeer(new InetSocketAddress(LOCAL_HOST, port)));

        client.downloadFile(FileUtils.getFileInfo(file));

        Optional<File> downloadedFile;
        do {
            downloadedFile = client.getFile(FILENAME);
        } while (downloadedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
        nodes.forEach(AKTorrent::shutDown);
    }

    @Test
    public void getAvailableFiles() throws ExecutionException, InterruptedException {
        File testFileA = new File(getClass().getResource(FILENAME).getFile());
        File testFileB = new File(getClass().getResource(FILENAME_2).getFile());

        AKTorrent node_A = new AKTorrent(NODE_A_PORT);

        node_A.seedFile(testFileA);
        node_A.seedFile(testFileB);

        AKTorrent node_B = new AKTorrent(NODE_B_PORT);
        node_B.addPeer(new InetSocketAddress(NODE_A_PORT));
        node_B.startServer();

        AKTorrent client = new AKTorrent(NODE_C_PORT);
        client.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_B_PORT));

        Set<FileInfo> files = client.getAvailableFiles().get();

        Set<FileInfo> expected = Set.of(FileUtils.getFileInfo(testFileA), FileUtils.getFileInfo(testFileB));
        assertNotNull(files);
        assertTrue(files.size() > 0);
        assertTrue(files.containsAll(expected));

        node_A.shutDown();
        node_B.shutDown();
    }

    @Test
    public void testDiscoverTransientPeers() throws IOException {
        AKTorrent nodeA = new AKTorrent(NODE_A_PORT);
        AKTorrent nodeB = new AKTorrent(NODE_B_PORT);
        AKTorrent nodeC = new AKTorrent(NODE_C_PORT);

        File file = new File(getClass().getResource(FILENAME).getFile());
        nodeC.seedFile(file);

        nodeB.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_C_PORT));
        nodeB.startServer();

        nodeA.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_B_PORT));
        nodeA.startServer();

        AKTorrent client = new AKTorrent(NODE_D_PORT);

        client.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_A_PORT));

        client.downloadFile(FileUtils.getFileInfo(file));

        Optional<File> downloadedFile;
        do{
            downloadedFile = client.getFile(file.getName());
        } while (downloadedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
        nodeA.shutDown();
        nodeB.shutDown();
        nodeC.shutDown();
    }

}
