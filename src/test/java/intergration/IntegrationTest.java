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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String FILENAME = "test_file.mp4";
    private static final String FILENAME_2 = "test_file_2.mp4";

    @Test
    public void canSeedAndReceiveFile() throws IOException {
        File file = getFile(FILENAME);
        AKTorrent server = AKTorrent.createAndInitializeNoBeacon();
        server.seedFile(file);

        AKTorrent client = AKTorrent.createAndInitializeNoBeacon();
        client.addPeer(server.getAddress());
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

    @Test
    public void testDiscoverTransientPeers() throws IOException {
        AKTorrent nodeA = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeB = AKTorrent.createAndInitializeNoBeacon();
        AKTorrent nodeC = AKTorrent.createAndInitializeNoBeacon();

        File file = getFile(FILENAME);
        nodeC.seedFile(file);

        nodeB.addPeer(nodeC.getAddress());

        nodeA.addPeer(nodeB.getAddress());

        AKTorrent client = AKTorrent.createAndInitializeNoBeacon();

        client.addPeer(nodeA.getAddress());

        client.downloadFile(FileService.getFileInfo(file));

        Optional<File> downloadedFile;
        do{
            downloadedFile = client.getFile(file.getName());
        } while (downloadedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
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
        Beacon beacon = new Beacon();
        final int beaconPort = beacon.start();

        AKTorrent nodeA = AKTorrent.createAndInitialize(new InetSocketAddress(LOCAL_HOST, beaconPort));
        File file = getFile(FILENAME);
        nodeA.seedFile(file);

        AKTorrent nodeB = AKTorrent.createAndInitialize(new InetSocketAddress(LOCAL_HOST, beaconPort));
        Set<FileInfo> fileInfos = nodeB.getAvailableFiles();
        assertTrue(fileInfos.contains(FileService.getFileInfo(file)));

        beacon.shutDown();
        nodeA.shutDown();
        nodeB.shutDown();
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

    private File getFile(String filename) {
        return new File(getClass().getResource("/" + filename).getFile());
    }
    
}
