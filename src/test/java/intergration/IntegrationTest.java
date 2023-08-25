package intergration;

import com.alyokaz.aktorrent.AKTorrent;
import com.alyokaz.aktorrent.FileInfo;
import com.alyokaz.aktorrent.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
    public void canSeedAndReceiveFile() throws IOException {
        File file = getFile(FILENAME);
        AKTorrent server = new AKTorrent(NODE_A_PORT);
        server.seedFile(file);

        AKTorrent client = new AKTorrent(NODE_B_PORT);
        client.addPeer(LOCAL_HOST, NODE_A_PORT);
        client.downloadFile(FileUtils.getFileInfo(file));
        Optional<File> completedFile;
        //TODO add some form of timeout
        do {
            completedFile = client.getFile(FILENAME);
        } while(completedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), completedFile.get().toPath()));
        server.shutDown();
    }

    @Test
    public void canDownloadFromTwoPeers() throws IOException {
        File file = getFile(FILENAME);

        AKTorrent ak1 = new AKTorrent(NODE_A_PORT);
        AKTorrent ak2 = new AKTorrent(NODE_B_PORT);
        AKTorrent ak3 = new AKTorrent(NODE_C_PORT);
        AKTorrent ak4 = new AKTorrent(NODE_D_PORT);

        ak1.seedFile(file);
        ak2.seedFile(file);
        ak3.seedFile(file);

        ak4.addPeer(LOCAL_HOST, NODE_A_PORT);
        ak4.addPeer(LOCAL_HOST, NODE_B_PORT);
        ak4.addPeer(LOCAL_HOST, NODE_C_PORT);

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
        File file = getFile(FILENAME);

        Set<AKTorrent> nodes = new HashSet<>();
        IntStream.range(minPort, maxPort).forEach(port -> nodes.add(new AKTorrent(port)));
        nodes.forEach(node -> node.seedFile(file));

        AKTorrent client = new AKTorrent(clientPort);

        IntStream.range(minPort, maxPort).forEach(port -> client.addPeer(LOCAL_HOST, port));

        client.downloadFile(FileUtils.getFileInfo(file));

        Optional<File> downloadedFile;
        do {
            downloadedFile = client.getFile(FILENAME);
        } while (downloadedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
        nodes.forEach(AKTorrent::shutDown);
    }

    @Test
    public void getAvailableFiles() {
        File testFileA = getFile(FILENAME);
        File testFileB = getFile(FILENAME_2);

        AKTorrent node_A = new AKTorrent(NODE_A_PORT);

        node_A.seedFile(testFileA);
        node_A.seedFile(testFileB);

        AKTorrent node_B = new AKTorrent(NODE_B_PORT);
        node_B.addPeer(LOCAL_HOST, NODE_A_PORT);
        node_B.startServer();

        AKTorrent client = new AKTorrent(NODE_C_PORT);
        client.addPeer(LOCAL_HOST, NODE_B_PORT);

        Set<FileInfo> files = client.getAvailableFiles();

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

        File file = getFile(FILENAME);
        nodeC.seedFile(file);

        nodeB.addPeer(LOCAL_HOST, NODE_C_PORT);
        nodeB.startServer();

        nodeA.addPeer(LOCAL_HOST, NODE_B_PORT);
        nodeA.startServer();

        AKTorrent client = new AKTorrent(NODE_D_PORT);

        client.addPeer(LOCAL_HOST, NODE_A_PORT);

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

    @Test
    public void pingPeerWhenAdded() {
        AKTorrent server = new AKTorrent(NODE_A_PORT);
        server.startServer();
        AKTorrent client = new AKTorrent(NODE_B_PORT);
        client.addPeer(LOCAL_HOST, NODE_A_PORT);
        assertTrue(client.getConnectedPeers().contains(new InetSocketAddress(LOCAL_HOST, NODE_A_PORT)));
        server.shutDown();
    }

    @Test
    public void pingByMultipleNodes() {
        AKTorrent server = new AKTorrent(NODE_A_PORT);
        server.startServer();

        Set<AKTorrent> nodes = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> nodes.add(new AKTorrent(NODE_B_PORT + i)));
        nodes.forEach(node -> node.addPeer(LOCAL_HOST, NODE_A_PORT));

        InetSocketAddress expectedAddress = new InetSocketAddress(LOCAL_HOST, NODE_A_PORT);
        nodes.forEach(node -> assertTrue(node.getConnectedPeers().contains(expectedAddress)));
        server.shutDown();
    }

    private File getFile(String filename) {
        return new File(getClass().getResource("/" + filename).getFile());
    }


}
