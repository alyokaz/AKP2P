import aktorrent.AKTorrent;
import aktorrent.Piece;
import aktorrent.PieceContainer;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class IntegrationTest {

    @Test
    public void canSeedAndReceiveFile() throws InterruptedException, ExecutionException {
        File file = new File(getClass().getResource("matrix.mp4").getFile());
        AKTorrent akTorrent = new AKTorrent(4444);
        akTorrent.seedFile(file);

        AKTorrent akTorrent2 = new AKTorrent(4441);
        akTorrent2.addPeer(new InetSocketAddress("127.0.0.1", 4444));
        Future future = akTorrent2.downloadFile(
                new PieceContainer(file.getName(), getNoOfPieces(file), new HashSet<>())
        );
        Thread.sleep(10000);
        future.get();
    }

    private int getNoOfPieces(File file) {
        int numberOfPieces = (int) file.length() / 1024;
        if((file.length() % 1024) == 0) {
            numberOfPieces++;
        }
        return numberOfPieces;
    }

    @Test
    public void canDownloadFromTwoPeers() throws ExecutionException, InterruptedException {
        File file = new File(getClass().getResource("matrix.mp4").getFile());
        AKTorrent ak1 = new AKTorrent(4444);
        AKTorrent ak2 = new AKTorrent(4441);
        AKTorrent ak3 = new AKTorrent(4442);
        ak1.seedFile(file);
        ak2.seedFile(file);
        ak3.addPeer(new InetSocketAddress("127.0.0.1", 4444));
        ak3.addPeer(new InetSocketAddress("127.0.0.1", 4441));
        ak3.downloadFile(new PieceContainer(file.getName(), getNoOfPieces(file), new HashSet<>())).get();
    }
}
