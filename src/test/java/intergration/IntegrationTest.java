package intergration;

import com.alyokaz.aktorrent.AKTorrent;
import com.alyokaz.aktorrent.beacon.Beacon;
import com.alyokaz.aktorrent.FileInfo;
import com.alyokaz.aktorrent.fileservice.FileService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String FILENAME = "test_file.mp4";
    private static final String FILENAME_2 = "test_file_2.mp4";

    @Test
    public void canSeedAndReceiveFile() throws IOException {
        File file = getFile(FILENAME);
        AKTorrent server = new AKTorrent();
        final int serverPort = server.seedFile(file);

        AKTorrent client = new AKTorrent();
        client.addPeer(LOCAL_HOST, serverPort);
        client.downloadFile(FileService.getFileInfo(file));
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

        AKTorrent nodeA = new AKTorrent();
        AKTorrent nodeB = new AKTorrent();
        AKTorrent nodeC = new AKTorrent();
        AKTorrent nodeD = new AKTorrent();

        final int nodeAPort = nodeA.seedFile(file);
        final int nodeBPort = nodeB.seedFile(file);
        final int nodeCPort = nodeC.seedFile(file);

        nodeD.addPeer(LOCAL_HOST, nodeAPort);
        nodeD.addPeer(LOCAL_HOST, nodeBPort);
        nodeD.addPeer(LOCAL_HOST, nodeCPort);

        nodeD.downloadFile(FileService.getFileInfo(file));

        Optional<File> downloadedFile;
        do {
            downloadedFile = nodeD.getFile(FILENAME);
        } while (downloadedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
        nodeA.shutDown();
        nodeB.shutDown();
        nodeC.shutDown();
        nodeD.shutDown();

    }

    @Test
    public void canDownloadFromMultiplePeers() throws IOException {
        final int min = 0;
        final int max = 10;
        File file = getFile(FILENAME);

        Set<AKTorrent> nodes = new HashSet<>();
        IntStream.range(min, max).forEach(port -> nodes.add(new AKTorrent()));
        Set<Integer> ports = nodes.stream().map(node -> node.seedFile(file)).collect(Collectors.toSet());

        AKTorrent client = new AKTorrent();

        ports.forEach(port -> client.addPeer(LOCAL_HOST, port));

        client.downloadFile(FileService.getFileInfo(file));

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

        AKTorrent node_A = new AKTorrent();

        final int nodeAPort = node_A.seedFile(testFileA);
        node_A.seedFile(testFileB);

        AKTorrent node_B = new AKTorrent();
        node_B.addPeer(LOCAL_HOST, nodeAPort);
         final int nodeBPort = node_B.startServer();

        AKTorrent client = new AKTorrent();
        client.addPeer(LOCAL_HOST, nodeBPort);

        Set<FileInfo> files = client.getAvailableFiles();

        Set<FileInfo> expected = Set.of(FileService.getFileInfo(testFileA), FileService.getFileInfo(testFileB));
        assertNotNull(files);
        assertTrue(files.size() > 0);
        assertTrue(files.containsAll(expected));

        node_A.shutDown();
        node_B.shutDown();
    }

    @Test
    public void testDiscoverTransientPeers() throws IOException {
        AKTorrent nodeA = new AKTorrent();
        AKTorrent nodeB = new AKTorrent();
        AKTorrent nodeC = new AKTorrent();

        File file = getFile(FILENAME);
        final int nodeCPort = nodeC.seedFile(file);

        nodeB.addPeer(LOCAL_HOST, nodeCPort);
        final int nodeBPort = nodeB.startServer();

        nodeA.addPeer(LOCAL_HOST, nodeBPort);
        final int nodeAPort = nodeA.startServer();

        AKTorrent client = new AKTorrent();

        client.addPeer(LOCAL_HOST, nodeAPort);

        client.downloadFile(FileService.getFileInfo(file));

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
        AKTorrent server = new AKTorrent();
        final int serverPort = server.startServer();
        AKTorrent client = new AKTorrent();
        client.addPeer(LOCAL_HOST, serverPort);
        assertTrue(client.getConnectedPeers().contains(new InetSocketAddress(LOCAL_HOST, serverPort)));
        server.shutDown();
    }

    @Test
    public void pingByMultipleNodes() {
        AKTorrent server = new AKTorrent();
        final int serverPort = server.startServer();

        Set<AKTorrent> nodes = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> nodes.add(new AKTorrent()));
        nodes.forEach(node -> node.addPeer(LOCAL_HOST, serverPort));

        InetSocketAddress expectedAddress = new InetSocketAddress(LOCAL_HOST, serverPort);
        nodes.forEach(node -> assertTrue(node.getConnectedPeers().contains(expectedAddress)));
        server.shutDown();
    }

    @Test
    public void canDownloadPeersFromBeacon() throws InterruptedException {
        Beacon beacon = new Beacon();
        final int beaconPort = beacon.start();

        Thread.sleep(1000);

        AKTorrent nodeA = new AKTorrent();
        nodeA.setBeaconAddress(LOCAL_HOST, beaconPort);
        File file = getFile(FILENAME);
        nodeA.seedFile(file);

        Thread.sleep(1000);

        AKTorrent nodeB = new AKTorrent();
        nodeB.setBeaconAddress(LOCAL_HOST, beaconPort);
        nodeB.startServer();
        Set<FileInfo> fileInfos = nodeB.getAvailableFiles();
        assertTrue(fileInfos.contains(FileService.getFileInfo(file)));

        beacon.shutDown();
        nodeA.shutDown();
        nodeB.shutDown();
    }

    private File getFile(String filename) {
        return new File(getClass().getResource("/" + filename).getFile());
    }
    
}
