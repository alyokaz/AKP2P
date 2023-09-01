package intergration;

import com.alyokaz.aktorrent.AKTorrent;
import com.alyokaz.aktorrent.FileService;
import com.alyokaz.aktorrent.cli.CLI;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CLIIntegrationTests {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final int BUFFER_SIZE = 1000000;
    private static final String FILENAME = "test_file.mp4";
    private static final String FILENAME_2 = "test_file_2.mp4";

    @Test
    public void testCliSendReceiveFile() throws IOException, InterruptedException {
        CountDownLatch countDownLatch_A = new CountDownLatch(1);
        CountDownLatch countDownLatch_B = new CountDownLatch(1);
        BlockingQueue<Integer> port = new LinkedBlockingQueue<>();

        new Thread(()-> {
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
            AKTorrent node = new AKTorrent();
            port.add(node.startServer());
            CLI cli = new CLI(in, out, node);
            try {
                cli.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        countDownLatch_B.await();

        AKTorrent client = new AKTorrent();
        client.addPeer(LOCAL_HOST, port.take());
        File file = getFile(FILENAME);
        client.downloadFile(FileService.getFileInfo(file));

        Optional<File> downloadedFile;
        do{
            downloadedFile = client.getFile(file.getName());
        } while (downloadedFile.isEmpty());
        
        assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
        countDownLatch_A.countDown();
    }

    @Test
    public void canDownloadFileByNumber() throws InterruptedException {
        CountDownLatch exit = new CountDownLatch(1);

        AKTorrent server = new AKTorrent();
        File file = getFile(FILENAME);
        BlockingQueue<Integer> port = new LinkedBlockingQueue<>();
        port.add(server.seedFile(file));

        Thread clientThread = new Thread(() -> {
            AKTorrent client = new AKTorrent();
            try {
                client.addPeer(LOCAL_HOST, port.take());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            CLI cli = new CLI(
                    new ByteArrayInputStream(("2\n1").getBytes()),
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
            do{
                downloadedFile = client.getFile(FILENAME);
            } while (downloadedFile.isEmpty());
            try {
                assertEquals(-1, Files.mismatch(file.toPath(), downloadedFile.get().toPath()));
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
        BlockingQueue<Integer> port = new LinkedBlockingQueue<>();

        AKTorrent server = new AKTorrent();
        port.add(server.startServer());

        AKTorrent client = new AKTorrent();
        new Thread(() -> {
            try {
                final int serverPort = port.take();
            CLI cli = new CLI(new ByteArrayInputStream(("3\n" + LOCAL_HOST + " " + serverPort).getBytes()),
                    new PrintStream(new ByteArrayOutputStream()),
                    client);

                cli.start();
                assertTrue(client.getConnectedPeers().contains(new InetSocketAddress(LOCAL_HOST, serverPort)));
                exitLatch.countDown();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        exitLatch.await();
        server.shutDown();
    }

    private File getFile(String filename) {
        return new File(getClass().getResource("/" + FILENAME).getFile());
    }
}
