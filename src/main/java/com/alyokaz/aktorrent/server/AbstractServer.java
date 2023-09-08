package com.alyokaz.aktorrent.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractServer implements Server {

    final ServerSocket serverSocket;
    final ExecutorService executor = Executors.newCachedThreadPool();

    public AbstractServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void start() {
        executor.execute(() -> {
            try (serverSocket) {
                System.out.println(Thread.currentThread().getName() + " Server Started");
                while (!Thread.currentThread().isInterrupted()) {
                    executor.execute(process(serverSocket.accept()));
                }
            } catch (IOException e) {
                System.out.println("Server Closed: " + e.getMessage());
            }
        });
    }

    @Override
    public void shutdown() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InetSocketAddress getServerAddress() {
        return new InetSocketAddress(serverSocket.getInetAddress(),
                serverSocket.getLocalPort());
    }


}
