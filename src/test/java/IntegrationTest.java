import aktorrent.AKTorrent;
import aktorrent.PieceContainer;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class IntegrationTest {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final int NODE_A_PORT = 4444;
    private static final int NODE_B_PORT = 4441;

    private static final int NODE_C_PORT = 4442;

    private static final int BUFFER_SIZE = 1000000;

    private static final String FILENAME = "matrix.mp4";

    @Test
    public void canSeedAndReceiveFile() throws InterruptedException, ExecutionException, IOException {
        final String filename = "matrix.mp4";
        File file = new File(getClass().getResource(filename).getFile());
        AKTorrent server = new AKTorrent(NODE_A_PORT);
        server.seedFile(file);

        AKTorrent client = new AKTorrent(NODE_B_PORT);
        client.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_A_PORT));
        Future future = client.downloadFile(
                new PieceContainer(file.getName(), getNoOfPieces(file))
        );
        future.get();
        Optional<File> completedFile;
        do {
            completedFile = client.getFile(filename);
        } while(completedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), completedFile.get().toPath()));
    }

    @Test
    public void canDownloadFromTwoPeers() throws ExecutionException, InterruptedException, IOException {
        File file = new File(getClass().getResource(FILENAME).getFile());

        AKTorrent ak1 = new AKTorrent(NODE_A_PORT);
        AKTorrent ak2 = new AKTorrent(NODE_B_PORT);
        AKTorrent ak3 = new AKTorrent(NODE_C_PORT);

        ak1.seedFile(file);
        ak2.seedFile(file);

        ak3.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_A_PORT));
        ak3.addPeer(new InetSocketAddress(LOCAL_HOST, NODE_B_PORT));

        ak3.downloadFile(new PieceContainer(file.getName(), getNoOfPieces(file))).get();

        assertEquals(-1, Files.mismatch(file.toPath(), ak3.getFile(FILENAME).get().toPath()));
    }

    static private int getNoOfPieces(File file) {
        int numberOfPieces = (int) file.length() / BUFFER_SIZE;
        if((file.length() % BUFFER_SIZE) != 0) {
            numberOfPieces++;
        }
        return numberOfPieces;
    }
}
