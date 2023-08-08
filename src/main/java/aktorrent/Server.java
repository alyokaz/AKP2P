package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.RequestFilenames;
import aktorrent.message.RequestPieceIDs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    private final int PORT;

    private ExecutorService executor = Executors.newCachedThreadPool();

    private Map<String, PieceContainer> files;

    private boolean running;

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
