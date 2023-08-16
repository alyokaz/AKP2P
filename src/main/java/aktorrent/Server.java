package aktorrent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<String, PieceContainer> files;

    private final List<InetSocketAddress> peers;

    private final Set<FileInfo> availableFiles;

    private final ServerSocket serverSocket;
    public Server(ServerSocket serverSocket, Map<String, PieceContainer> files, List<InetSocketAddress> peers,
                  Set<FileInfo> availableFiles) {
        this.serverSocket = serverSocket;
        this.files = files;
        this.peers = peers;
        this.availableFiles = availableFiles;
    }

    public void start() {
        executor.execute(() -> {
            try(serverSocket) {
                System.out.println(Thread.currentThread().getName() + " Server Started");
                while(!Thread.currentThread().isInterrupted()) {
                    PeerHandler peerHandler = new PeerHandler(serverSocket.accept(), files, peers, availableFiles);
                    executor.execute(peerHandler);
                }
            } catch (IOException e) {
                System.out.println("Server Closed: " + e.getMessage());
            }
        });

    }

    public void shutdown() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
