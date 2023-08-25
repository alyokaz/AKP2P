package com.alyokaz.aktorrent.server;

import com.alyokaz.aktorrent.FileInfo;
import com.alyokaz.aktorrent.FileService;
import com.alyokaz.aktorrent.PeerService;
import com.alyokaz.aktorrent.PieceContainer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ServerSocket serverSocket;
    private final PeerService peerService;
    private final FileService fileService;

    public Server(ServerSocket serverSocket, PeerService peerService, FileService fileService) {
        this.serverSocket = serverSocket;
        this.peerService = peerService;
        this.fileService = fileService;
    }

    public void start() {
        executor.execute(() -> {
            try (serverSocket) {
                System.out.println(Thread.currentThread().getName() + " Server Started");
                while (!Thread.currentThread().isInterrupted()) {
                    PeerHandler peerHandler = new PeerHandler(serverSocket.accept(), peerService, fileService);
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
