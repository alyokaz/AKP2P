package aktorrent;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    private final int PORT;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<String, PieceContainer> files;

    public Server(int port, Map<String, PieceContainer> files) {
        this.PORT = port;
        this.files = files;
    }

    public void start() {
        executor.execute(() -> {
            try(ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println(Thread.currentThread().getName() + " Server Started");
                while(!Thread.currentThread().isInterrupted()) {
                    PeerHandler peerHandler = new PeerHandler(serverSocket.accept(), files);
                    executor.execute(peerHandler);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public void shutdown() {
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
