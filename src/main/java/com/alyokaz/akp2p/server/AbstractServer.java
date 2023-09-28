package com.alyokaz.akp2p.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Abstract class implementing the {@code Server} interface with boilerplate code common to all {@code Server}
 * implementations.
 */
public abstract class AbstractServer implements Server {

    final ServerSocket serverSocket;
    final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Logger logger = LogManager.getLogger();

    /**
     * Constructs a new instance of this class with the given {@code ServerSocket}
     * @param serverSocket the {@code ServerSocket} for the server to listen for connections on
     */
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

    /**
     * Returns a {@code Runnable} to handle a connection from a peer to the {@code Server}.
     * <p>
     * Called from within the {@link #start()} method upon a new connection to the {@code Server} from a peer.
     *
     * @param socket the {@code Socket} for the connection to the peer
     * @return a {@code Runnable} to handle to connection to the peer
     */
    protected abstract Runnable process(Socket socket);



}
