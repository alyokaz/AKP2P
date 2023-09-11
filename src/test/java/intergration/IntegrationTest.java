package intergration;

import com.alyokaz.aktorrent.AKTorrent;
import com.alyokaz.aktorrent.beacon.Beacon;
import com.alyokaz.aktorrent.fileservice.FileInfo;
import com.alyokaz.aktorrent.fileservice.FileService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String FILENAME = "test_file.mp4";
    private static final String FILENAME_2 = "test_file_2.mp4";
    private final static ExecutorService executor = Executors.newCachedThreadPool();

    @Test
    public void canSeedAndReceiveFile() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File file = getFile(FILENAME);
        AKTorrent server = AKTorrent.createAndInitializeNoBeacon();
        server.seedFile(file);

        AKTorrent client = AKTorrent.createAndInitializeNoBeacon();
        client.addPeer(server.getAddress());
        client.downloadFile(FileService.getFileInfo(file));
        File completedFile = getDownloadedFile(client, FILENAME).get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(), completedFile.toPath()));
        server.shutDown();
    }

    @Test
    public void canDownloadFromTwoPeers() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File file = getFile(FILENAME);

        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeC = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeD = AKTorrent.createAndInitializeNoBeacon();

        nodeA.seedFile(file);
        nodeB.seedFile(file);
        nodeC.seedFile(file);

        nodeD.addPeer(nodeA.getAddress());
        nodeD.addPeer(nodeB.getAddress());
        nodeD.addPeer(nodeC.getAddress());

        nodeD.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(nodeD, FILENAME).get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.toPath()));

        nodeA.shutDown();
        nodeB.shutDown();
        nodeC.shutDown();
        nodeD.shutDown();

    }

    @Test
    public void canDownloadFromMultiplePeers() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        final int numberOfPeers = 10;
        File file = getFile(FILENAME);

        Set<AKTorrent> nodes = Stream.generate(() -> {
            AKTorrent node = AKTorrent.createAndInitializeNoBeacon();
            node.seedFile(file);
            return node;
        }).limit(numberOfPeers).collect(Collectors.toSet());

        AKTorrent client = AKTorrent.createAndInitializeNoBeacon();

        nodes.forEach(node -> client.addPeer(node.getAddress()));

        client.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(client, FILENAME).get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.toPath()));
        nodes.forEach(AKTorrent::shutDown);
    }

    @Test
    public void getAvailableFiles() {
        File testFileA = getFile(FILENAME);
        File testFileB = getFile(FILENAME_2);

        AKTorrent node_A = AKTorrent.createAndInitializeNoBeacon();

        node_A.seedFile(testFileA);
        node_A.seedFile(testFileB);

        AKTorrent node_B = AKTorrent.createAndInitializeNoBeacon();
        node_B.addPeer(node_A.getAddress());

        AKTorrent client = AKTorrent.createAndInitializeNoBeacon();
        client.addPeer(node_B.getAddress());

        Set<FileInfo> files = client.getAvailableFiles();

        Set<FileInfo> expected = Set.of(FileService.getFileInfo(testFileA), FileService.getFileInfo(testFileB));
        assertNotNull(files);
        assertTrue(files.size() > 0);
        assertTrue(files.containsAll(expected));

        node_A.shutDown();
        node_B.shutDown();
    }

    // Why is this passing if with beacon it fails?
    // The test only works because the client node runs updatePeers transitively
    // through getAvailableFiles. It gathers all peers and then tries to download
    // the file from all of them. It should only download from nodes that have
    // the file. The address of the node(s) should be stored with the file for
    // this.
    @Test
    public void testDiscoverTransientPeers() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeC = AKTorrent.createAndInitializeNoBeacon();

        File file = getFile(FILENAME);
        nodeC.seedFile(file);

        nodeB.addPeer(nodeC.getAddress());

        nodeA.addPeer(nodeB.getAddress());

        AKTorrent client = AKTorrent.createAndInitializeNoBeacon();

        client.addPeer(nodeA.getAddress());
        client.getAvailableFiles();
        client.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(client, file.getName()).get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.toPath()));
        nodeA.shutDown();
        nodeB.shutDown();
        nodeC.shutDown();
        client.shutDown();
    }

    @Test
    public void pingPeerWhenAdded() {
        AKTorrent server = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent client = AKTorrent.createAndInitializeNoBeacon();
        client.addPeer(server.getAddress());
        assertTrue(client.getLivePeers().contains(server.getAddress()));
        server.shutDown();
        client.shutDown();
    }

    @Test
    public void pingByMultipleNodes() throws IOException {
        AKTorrent server = AKTorrent.createAndInitializeNoBeacon();

        Set<AKTorrent> nodes = Stream.generate(AKTorrent::createAndInitializeNoBeacon).limit(100).collect(Collectors.toSet());
        InetSocketAddress serverAddress = server.getAddress();
        nodes.forEach(node -> node.addPeer(serverAddress));

        nodes.forEach(node -> assertTrue(node.getLivePeers().contains(serverAddress)));
        server.shutDown();
    }

    @Test
    public void canDownloadPeersFromBeacon() throws IOException {
        Beacon beacon = Beacon.createAndInitialise();

        AKTorrent nodeA = AKTorrent.createAndInitialize(beacon.getAddress());
        File file = getFile(FILENAME);
        nodeA.seedFile(file);

        AKTorrent nodeB = AKTorrent.createAndInitialize(beacon.getAddress());
        Set<FileInfo> fileInfos = nodeB.getAvailableFiles();
        assertTrue(fileInfos.contains(FileService.getFileInfo(file)));

        beacon.shutDown();
        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void canDownloadFileWithBeacon() throws IOException,
            ExecutionException, InterruptedException, TimeoutException {
        Beacon beacon = Beacon.createAndInitialise();

        AKTorrent nodeA = AKTorrent.createAndInitialize(beacon.getAddress());
        AKTorrent nodeB = AKTorrent.createAndInitialize(beacon.getAddress());

        File file = getFile(FILENAME);
        nodeA.seedFile(file);

        nodeB.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(nodeB, FILENAME)
                .get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(),
                downloadedFile.toPath()));

        beacon.shutDown();
        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void canDownloadFileWithBeaconBtoA() throws IOException,
            ExecutionException, InterruptedException, TimeoutException {
        Beacon beacon = Beacon.createAndInitialise();

        AKTorrent nodeA = AKTorrent.createAndInitialize(beacon.getAddress());
        AKTorrent nodeB = AKTorrent.createAndInitialize(beacon.getAddress());

        File file = getFile(FILENAME);
        nodeB.seedFile(file);

        nodeA.getAvailableFiles();
        nodeA.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(nodeA, FILENAME)
                .get(30000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(),
                downloadedFile.toPath()));

        beacon.shutDown();
        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void canDownloadFromTransientPeerWithBeacon() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        Beacon beacon = Beacon.createAndInitialise();
        InetSocketAddress beaconAddress = beacon.getAddress();

        AKTorrent nodeA = AKTorrent.createAndInitialize(beaconAddress);
        AKTorrent nodeB = AKTorrent.createAndInitialize(beaconAddress);
        nodeA.getAvailableFiles();
        AKTorrent nodeC = AKTorrent.createAndInitializeNoBeacon();

        nodeC.addPeer(nodeB.getAddress());
        File file = getFile(FILENAME);
        nodeC.seedFile(file);
        nodeC.getAvailableFiles();
        nodeB.getAvailableFiles();

        nodeA.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(nodeA, FILENAME)
                .get(10000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.toPath()));

        nodeA.shutDown();
        nodeB.shutDown();
        nodeC.shutDown();
    }

    @Test
    public void deadNodeWillNotBeAddedToLivePeers() throws InterruptedException {
        AKTorrent deadNode = AKTorrent.createAndInitializeNoBeacon();
        InetSocketAddress deadNodeAddress = deadNode.getAddress();
        deadNode.shutDown();

        AKTorrent liveNode = AKTorrent.createAndInitializeNoBeacon();
        InetSocketAddress liveNodeAddress = liveNode.getAddress();

        AKTorrent clientNode = AKTorrent.createAndInitializeNoBeacon();

        clientNode.addPeer(deadNodeAddress);
        clientNode.addPeer(liveNodeAddress);

        assertTrue(clientNode.getLivePeers().contains(liveNodeAddress));
        assertFalse(clientNode.getLivePeers().contains(deadNodeAddress));

        liveNode.shutDown();
        clientNode.shutDown();
    }

    @Test
    public void willNotDownloadFromDeadNode() {
        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();
        File file = getFile(FILENAME);
        nodeA.seedFile(file);
        nodeB.addPeer(nodeA.getAddress());

        nodeA.shutDown();
        nodeB.downloadFile(FileService.getFileInfo(file));

        assertThrows(TimeoutException.class,
                () -> getDownloadedFile(nodeB, FILENAME).get(3000, TimeUnit.MILLISECONDS));
        // dead node removed from live peers and placed back into peers
        assertFalse(nodeB.getLivePeers().contains(nodeA.getAddress()));
        assertTrue(nodeB.getPeers().contains(nodeA.getAddress()));

        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void gettingAvailableFilesShouldNotCauseLoop() {
        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();
        nodeA.addPeer(nodeB.getAddress());
        nodeB.addPeer(nodeA.getAddress());
        nodeB.getAvailableFiles();
    }

    @Test
    public void shouldNotAddSelfToPeers() {
        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();
        nodeA.addPeer(nodeB.getAddress());
        nodeB.addPeer(nodeA.getAddress());

        nodeA.getAvailableFiles();

        assertFalse(nodeA.getLivePeers().contains(nodeA.getAddress()));
    }

    @Test
    public void canAddNodes() {
        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeC = AKTorrent.createAndInitializeNoBeacon();

        nodeA.addPeer(nodeB.getAddress());
        nodeA.addPeer(nodeC.getAddress());

        assertTrue(nodeA.getLivePeers().size() == 2);
        assertTrue(nodeA.getLivePeers().containsAll(List.of(nodeB.getAddress(), nodeC.getAddress())));
    }

    @Test
    public void peerIsRegisteredOnContact() {
        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();

        nodeB.addPeer(nodeA.getAddress());

        assertTrue(nodeA.getLivePeers().contains(nodeB.getAddress()));
    }


    @Test
    public void preventDuplicatePeer() {
        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();

        nodeA.addPeer(nodeB.getAddress());
        nodeA.addPeer(nodeB.getAddress());

        assertEquals(1, nodeA.getLivePeers().size());
    }

    //TODO move timeout to this method
    private static Future<File> getDownloadedFile(AKTorrent node, String filename) {
        return executor.submit(() -> {
            Optional<File> downloadedFile;
            do {
                downloadedFile = node.getFile(filename);
            } while (!Thread.interrupted() && downloadedFile.isEmpty());
            return downloadedFile.get();
        });
    }

    private File getFile(String filename) {
        return new File(getClass().getResource("/" + filename).getFile());
    }
    
}
