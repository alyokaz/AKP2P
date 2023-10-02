package intergration;

import com.alyokaz.akp2p.AKP2P;
import com.alyokaz.akp2p.beacon.Beacon;
import com.alyokaz.akp2p.fileservice.FileInfo;
import com.alyokaz.akp2p.fileservice.FileService;
import com.alyokaz.akp2p.fileservice.exceptions.SeedFileException;
import com.alyokaz.akp2p.peerservice.exceptions.PingPeerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    private static final String FILENAME = "test_file.mp4";
    private static final String FILENAME_2 = "test_file_2.mp4";
    private final static ExecutorService executor = Executors.newCachedThreadPool();
    private Logger logger = LogManager.getLogger();

    @Test
    public void canSeedAndReceiveFile() throws IOException, ExecutionException, InterruptedException, TimeoutException, PingPeerException, SeedFileException {
        File file = getFile(FILENAME);
        AKP2P server = AKP2P.createAndInitializeNoBeacon();
        server.seedFile(file);

        AKP2P client = AKP2P.createAndInitializeNoBeacon();
        client.addPeer(server.getAddress());
        client.downloadFile(FileService.getFileInfo(file));
        File completedFile = getDownloadedFile(client, FILENAME).get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(), completedFile.toPath()));
        client.shutDown();
        server.shutDown();
    }

    @Test
    public void canDownloadFromThreePeers() throws IOException, ExecutionException, InterruptedException, TimeoutException, PingPeerException, SeedFileException {
        File file = getFile(FILENAME);

        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeC = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeD = AKP2P.createAndInitializeNoBeacon();

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

        Set<AKP2P> nodes = Stream.generate(() -> {
            AKP2P node = AKP2P.createAndInitializeNoBeacon();
            try {
                node.seedFile(file);
            } catch (SeedFileException e) {
                throw new RuntimeException(e);
            }
            return node;
        }).limit(numberOfPeers).collect(Collectors.toSet());

        AKP2P client = AKP2P.createAndInitializeNoBeacon();

        nodes.forEach(node -> {
            try {
                client.addPeer(node.getAddress());
            } catch (PingPeerException e) {
                throw new RuntimeException(e);
            }
        });

        client.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(client, FILENAME).get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.toPath()));
        nodes.forEach(AKP2P::shutDown);
    }

    @Test
    public void getAvailableFiles() throws PingPeerException, SeedFileException {
        File testFileA = getFile(FILENAME);
        File testFileB = getFile(FILENAME_2);

        AKP2P node_A = AKP2P.createAndInitializeNoBeacon();

        node_A.seedFile(testFileA);
        node_A.seedFile(testFileB);

        AKP2P node_B = AKP2P.createAndInitializeNoBeacon();
        node_B.addPeer(node_A.getAddress());

        AKP2P client = AKP2P.createAndInitializeNoBeacon();
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
    public void testDiscoverTransientPeers() throws IOException, ExecutionException, InterruptedException, TimeoutException, PingPeerException, SeedFileException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeC = AKP2P.createAndInitializeNoBeacon();

        File file = getFile(FILENAME);
        nodeC.seedFile(file);

        nodeB.addPeer(nodeC.getAddress());

        nodeA.addPeer(nodeB.getAddress());

        AKP2P client = AKP2P.createAndInitializeNoBeacon();

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
    public void pingPeerWhenAdded() throws PingPeerException {
        AKP2P server = AKP2P.createAndInitializeNoBeacon();
        AKP2P client = AKP2P.createAndInitializeNoBeacon();
        client.addPeer(server.getAddress());
        assertTrue(client.getLivePeers().contains(server.getAddress()));
        server.shutDown();
        client.shutDown();
    }

    //TODO this is now more of addPeers functionality stress test rather than stress test of pinging.
    @Test
    public void pingByMultipleNodes() throws IOException {
        int numberOfNodes = 10;
        AKP2P server = AKP2P.createAndInitializeNoBeacon();

        Set<AKP2P> nodes = Stream.generate(AKP2P::createAndInitializeNoBeacon)
                .limit(numberOfNodes)
                .collect(Collectors.toSet());
        InetSocketAddress serverAddress = server.getAddress();
        nodes.forEach(node -> {
            try {
                node.addPeer(serverAddress);
            } catch (PingPeerException e) {
                throw new RuntimeException(e);
            }
        });

        nodes.forEach(node -> assertTrue(node.getLivePeers().contains(serverAddress)));
        nodes.forEach(AKP2P::shutDown);
        server.shutDown();
    }

    @Test
    public void canDownloadPeersFromBeacon() throws IOException, SeedFileException {
        Beacon beacon = Beacon.createAndInitialise();

        AKP2P nodeA = AKP2P.createAndInitialize(beacon.getAddress());
        File file = getFile(FILENAME);
        nodeA.seedFile(file);

        AKP2P nodeB = AKP2P.createAndInitialize(beacon.getAddress());
        Set<FileInfo> fileInfos = nodeB.getAvailableFiles();
        assertTrue(fileInfos.contains(FileService.getFileInfo(file)));

        beacon.shutDown();
        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void canDownloadFileWithBeacon() throws IOException,
            ExecutionException, InterruptedException, TimeoutException, SeedFileException {
        Beacon beacon = Beacon.createAndInitialise();

        AKP2P nodeA = AKP2P.createAndInitialize(beacon.getAddress());
        AKP2P nodeB = AKP2P.createAndInitialize(beacon.getAddress());

        File file = getFile(FILENAME);
        nodeA.seedFile(file);

        nodeB.getAvailableFiles();
        nodeB.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(nodeB, FILENAME).get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(),
                downloadedFile.toPath()));

        beacon.shutDown();
        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void canDownloadFileWithBeaconBtoA() throws IOException,
            ExecutionException, InterruptedException, TimeoutException, SeedFileException {
        Beacon beacon = Beacon.createAndInitialise();

        AKP2P nodeA = AKP2P.createAndInitialize(beacon.getAddress());
        AKP2P nodeB = AKP2P.createAndInitialize(beacon.getAddress());

        File file = getFile(FILENAME);
        nodeB.seedFile(file);

        nodeA.getAvailableFiles();
        nodeA.downloadFile(FileService.getFileInfo(file));

        File downloadedFile = getDownloadedFile(nodeA, FILENAME).get(30000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(),
                downloadedFile.toPath()));

        beacon.shutDown();
        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void canDownloadFromTransientPeerWithBeacon() throws ExecutionException, InterruptedException,
            TimeoutException, IOException, PingPeerException, SeedFileException {
        Beacon beacon = Beacon.createAndInitialise();
        InetSocketAddress beaconAddress = beacon.getAddress();

        AKP2P nodeA = AKP2P.createAndInitialize(beaconAddress);
        AKP2P nodeB = AKP2P.createAndInitialize(beaconAddress);
        AKP2P nodeC = AKP2P.createAndInitializeNoBeacon();

        nodeC.addPeer(nodeB.getAddress());

        File file = getFile(FILENAME);
        nodeC.seedFile(file);

        nodeB.getAvailableFiles();

        FileInfo fileInfo = nodeA.getAvailableFiles().stream().findFirst().get();
        nodeA.downloadFile(fileInfo);

        File downloadedFile = getDownloadedFile(nodeA, FILENAME).get(10000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.toPath()));

        nodeA.shutDown();
        nodeB.shutDown();
        nodeC.shutDown();
    }

    @Test
    public void deadNodeWillNotBeAddedToLivePeers()  {
        AKP2P deadNode = AKP2P.createAndInitializeNoBeacon();
        InetSocketAddress deadNodeAddress = deadNode.getAddress();
        deadNode.shutDown();

        AKP2P liveNode = AKP2P.createAndInitializeNoBeacon();
        InetSocketAddress liveNodeAddress = liveNode.getAddress();

        AKP2P clientNode = AKP2P.createAndInitializeNoBeacon();

        clientNode.addPeer(liveNodeAddress);

        assertTrue(clientNode.getLivePeers().contains(liveNodeAddress));
        assertFalse(clientNode.getLivePeers().contains(deadNodeAddress));

        liveNode.shutDown();
        clientNode.shutDown();
    }

    @Test
    public void willNotDownloadFromDeadNode() throws PingPeerException, SeedFileException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();
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
    public void gettingAvailableFilesShouldNotCauseLoop() throws PingPeerException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();

        nodeA.addPeer(nodeB.getAddress());
        nodeB.addPeer(nodeA.getAddress());

        nodeB.getAvailableFiles();

        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void shouldNotAddSelfToPeers() throws PingPeerException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();
        nodeA.addPeer(nodeB.getAddress());
        nodeB.addPeer(nodeA.getAddress());

        nodeA.getAvailableFiles();

        assertFalse(nodeA.getLivePeers().contains(nodeA.getAddress()));

        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void canAddNodes() throws PingPeerException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeC = AKP2P.createAndInitializeNoBeacon();

        nodeA.addPeer(nodeB.getAddress());
        nodeA.addPeer(nodeC.getAddress());

        assertTrue(nodeA.getLivePeers().size() == 2);
        assertTrue(nodeA.getLivePeers().containsAll(List.of(nodeB.getAddress(), nodeC.getAddress())));

        nodeA.shutDown();
        nodeB.shutDown();
        nodeC.shutDown();
    }

    @Test
    public void peerIsRegisteredOnContact() throws PingPeerException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();

        nodeB.addPeer(nodeA.getAddress());

        assertTrue(nodeA.getLivePeers().contains(nodeB.getAddress()));

        nodeA.shutDown();
        nodeB.shutDown();
    }


    @Test
    public void preventDuplicatePeer() throws PingPeerException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();

        nodeA.addPeer(nodeB.getAddress());
        nodeA.addPeer(nodeB.getAddress());

        assertEquals(1, nodeA.getLivePeers().size());

        nodeA.shutDown();
        nodeB.shutDown();
    }

    @Test
    public void canHandleAttemptToAddBadPeerAddress() {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        nodeA.addPeer(new InetSocketAddress("dsafdf", 80));
        nodeA.shutDown();
    }

    @Test
    public void canRegisterFileFromPeerByAddress() throws SeedFileException, PingPeerException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();

        File file = getFile(FILENAME);
        nodeA.seedFile(file);
        nodeB.addPeer(nodeA.getAddress());

        Map<FileInfo, Set<InetSocketAddress>> fileAddressRegistry = nodeB.getFileRegistry();
        assertTrue(fileAddressRegistry.containsKey(FileService.getFileInfo(file)));
        assertTrue(fileAddressRegistry.get(FileService.getFileInfo(file)).contains(nodeA.getAddress()));

        nodeA.shutDown();
        nodeB.shutDown();
    }

    //TODO ability to see if a file has been scheduled for download or is in progress would make this redundant
    @Test
    public void willContactOnlyPeersWithFile() throws SeedFileException, PingPeerException, IOException, ExecutionException, InterruptedException, TimeoutException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeC = AKP2P.createAndInitializeNoBeacon();

        File file = getFile(FILENAME);
        nodeA.seedFile(file);
        nodeB.addPeer(nodeA.getAddress());
        nodeB.addPeer(nodeC.getAddress());


        FileInfo fileInfo = nodeB.getAvailableFiles().stream().findFirst().get();

        nodeB.downloadFile(fileInfo);

        File downloadedFile = getDownloadedFile(nodeB, file.getName()).get(3000, TimeUnit.MILLISECONDS);

        assertEquals(-1, Files.mismatch(downloadedFile.toPath(), file.toPath()));

        nodeA.shutDown();
        nodeB.shutDown();
        nodeC.shutDown();
    }

    @Test
    public void deadPeerRemovedWhenUpdatingFiles() {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeC = AKP2P.createAndInitializeNoBeacon();

        nodeB.addPeer(nodeA.getAddress());
        nodeB.addPeer(nodeC.getAddress());
        nodeC.shutDown();
        nodeB.getAvailableFiles();
        assertEquals(1, nodeB.getLivePeers().size());
    }

    @Test
    public void canGetProgressOfDownload() throws SeedFileException, InterruptedException {
        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon();
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();

        File file = getFile(FILENAME);
        nodeA.seedFile(file);

        nodeB.addPeer(nodeA.getAddress());
        Set<FileInfo> availableFiles = nodeB.getAvailableFiles();
        nodeB.downloadFile(availableFiles.stream().findFirst().get());

        while(nodeB.getProgressOfDownload(file.getName()) < 1) {
            double progress = nodeB.getProgressOfDownload(file.getName());
            logger.debug("Download progress = {}", progress * 100);
            assertTrue(progress <= 1) ;
            Thread.sleep(1);
        }
    }

    @Test
    public void canBuildWithCustomPort() throws IOException {
        int customPort = getFreePort();

        AKP2P nodeA = AKP2P.createAndInitializeNoBeacon(customPort);
        AKP2P nodeB = AKP2P.createAndInitializeNoBeacon();

        nodeB.addPeer(nodeA.getAddress());
        assertTrue(nodeB.getLivePeers().contains(nodeA.getAddress()));
    }

    @Test
    public void canBuildWithCustomPortAndBeacon() throws IOException {
        Beacon beacon = Beacon.createAndInitialise();
        int customPort = getFreePort();
        AKP2P nodeA = AKP2P.createAndInitialize(customPort, beacon.getAddress());
        AKP2P nodeB = AKP2P.createAndInitialize(beacon.getAddress());

        assertTrue(nodeB.getLivePeers().contains(nodeA.getAddress()));
    }

    private int getFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        socket.close();
        return socket.getLocalPort();
    }


    //TODO move timeout to this method
    private static Future<File> getDownloadedFile(AKP2P node, String filename) {
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
