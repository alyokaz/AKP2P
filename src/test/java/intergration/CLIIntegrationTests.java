package intergration;

import com.alyokaz.akp2p.AKP2P;
import com.alyokaz.akp2p.cli.CLI;
import com.alyokaz.akp2p.fileservice.FileService;
import com.alyokaz.akp2p.fileservice.exceptions.SeedFileException;
import com.alyokaz.akp2p.peerservice.exceptions.PingPeerException;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

public class CLIIntegrationTests {

    private static final String FILENAME = "test_file.mp4";

    @Test
    public void testCliSendReceiveFile() throws IOException, InterruptedException, PingPeerException {
        CountDownLatch countDownLatch_A = new CountDownLatch(1);
        CountDownLatch countDownLatch_B = new CountDownLatch(1);
        BlockingQueue<InetSocketAddress> serverAddress = new LinkedBlockingQueue<>();

        new Thread(() -> {
            InputStream in = new InputStream() {
                private final byte[] command = ("1\n " + FILENAME + "\n").getBytes();
                int index = 0;

                @Override
                public int read() {
                    if (index == command.length) {
                        index++;
                        return -1;
                    } else if (index > command.length) {
                        try {
                            countDownLatch_B.countDown();
                            countDownLatch_A.await();
                            return -1;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return command[index++];
                }
            };

            PrintStream out = new PrintStream(new ByteArrayOutputStream(1024));
            AKP2P node = AKP2P.createAndInitializeNoBeacon();
            serverAddress.add(node.getAddress());
            CLI cli = new CLI(in, out, node);
            try {
                cli.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        countDownLatch_B.await();

        AKP2P client = AKP2P.createAndInitializeNoBeacon();
        client.addPeer(serverAddress.take());
        File file = getFile(FILENAME);
        client.downloadFile(FileService.getFileInfo(file));

        Optional<File> downloadedFile;
        do {
            downloadedFile = client.getFile(file.getName());
        } while (downloadedFile.isEmpty());

        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
        countDownLatch_A.countDown();
        client.shutDown();
    }

    @Test
    public void canDownloadFileByNumber() throws InterruptedException, SeedFileException {
        CountDownLatch countDownLatch_A = new CountDownLatch(1);
        CountDownLatch exit = new CountDownLatch(1);

        AKP2P server = AKP2P.createAndInitializeNoBeacon();
        File file = getFile(FILENAME);
        BlockingQueue<InetSocketAddress> serverAddress = new LinkedBlockingQueue<>();
        server.seedFile(file);
        serverAddress.add(server.getAddress());

        InputStream in = new InputStream() {
            private final byte[] command = ("2\n1\n\n").getBytes();
            int index = 0;

            @Override
            public int read() {
                if (index == command.length) {
                    index++;
                    return -1;
                } else if (index > command.length) {
                    try {
                        countDownLatch_A.await();
                        return -1;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                return command[index++];
            }
        };

        Thread clientThread = new Thread(() -> {
            AKP2P client = AKP2P.createAndInitializeNoBeacon();
            try {
                client.addPeer(serverAddress.take());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (PingPeerException e) {
                throw new RuntimeException(e);
            }
            CLI cli = new CLI(
                    in,
                    new PrintStream(new ByteArrayOutputStream()),
                    client
            );
            new Thread(() -> {
                try {
                    cli.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            Optional<File> downloadedFile;
            //TODO refactor out and make timeout
            do {
                downloadedFile = client.getFile(FILENAME);
            } while (downloadedFile.isEmpty());
            try {
                assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
                countDownLatch_A.countDown();
                exit.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        clientThread.setName("CLIENT");
        clientThread.start();

        exit.await();
        server.shutDown();
    }

    @Test
    public void canAddPeer() throws InterruptedException {
        CountDownLatch exitLatch = new CountDownLatch(1);
        BlockingQueue<InetSocketAddress> serverAddress = new LinkedBlockingQueue<>();

        AKP2P server = AKP2P.createAndInitializeNoBeacon();
        serverAddress.add(server.getAddress());

        new Thread(() -> {
            try {
                final InetSocketAddress socketAddress = serverAddress.take();
                AKP2P client = AKP2P.createAndInitializeNoBeacon();

                CLI cli = new CLI(new ByteArrayInputStream(("3\n" + socketAddress.getHostName() + " "
                        + socketAddress.getPort()).getBytes()),
                        new PrintStream(new ByteArrayOutputStream()), client);
                cli.start();

                assertTrue(client.getLivePeers().contains(socketAddress));
                exitLatch.countDown();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        exitLatch.await();
        server.shutDown();
    }

    @Test
    public void canHandleBadPeerAddress() throws InterruptedException {
        CountDownLatch exitLatch = new CountDownLatch(1);
        BlockingQueue<InetSocketAddress> serverAddress = new LinkedBlockingQueue<>();

        AKP2P server = AKP2P.createAndInitializeNoBeacon();
        serverAddress.add(server.getAddress());

        new Thread(() -> {
            try {
                final InetSocketAddress socketAddress = serverAddress.take();
                AKP2P client = AKP2P.createAndInitializeNoBeacon();

                CLI cli = new CLI(new ByteArrayInputStream(("3\n" + "bad peer address\n\n").getBytes()),
                        new PrintStream(new ByteArrayOutputStream()), client);
                cli.start();

                assertFalse(client.getLivePeers().contains(socketAddress));
                exitLatch.countDown();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        exitLatch.await();
        server.shutDown();
    }

    @Test
    public void canHandleDeadPeer() throws InterruptedException {
        CountDownLatch exitLatch = new CountDownLatch(1);
        BlockingQueue<InetSocketAddress> serverAddress = new LinkedBlockingQueue<>();

        AKP2P server = AKP2P.createAndInitializeNoBeacon();
        serverAddress.add(server.getAddress());
        server.shutDown();

        new Thread(() -> {
            try {
                final InetSocketAddress socketAddress = serverAddress.take();
                AKP2P client = AKP2P.createAndInitializeNoBeacon();

                CLI cli = new CLI(new ByteArrayInputStream(("3\n" + socketAddress.getHostName() + " "
                        + socketAddress.getPort() + "\n\n").getBytes()),
                        new PrintStream(new ByteArrayOutputStream()), client);
                cli.start();

                assertFalse(client.getLivePeers().contains(socketAddress));
                exitLatch.countDown();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        exitLatch.await();

    }

    private File getFile(String filename) {
        return new File(getClass().getResource("/" + FILENAME).getFile());
    }
}
