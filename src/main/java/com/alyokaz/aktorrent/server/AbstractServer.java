package com.alyokaz.aktorrent.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class AbstractServer implements Server {

    final ServerSocket serverSocket;
    final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Logger logger = LogManager.getLogger();

    public AbstractServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void start() {
        executor.execute(() -> {
            try (serverSocket) {
                logger.info("Server at {} has started", serverSocket.getLocalSocketAddress());
                while (!Thread.currentThread().isInterrupted()) {
                    executor.execute(process(serverSocket.accept()));
                }
            } catch (IOException e) {
                logger.info("Server at {} shutdown", serverSocket.getLocalSocketAddress());
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
