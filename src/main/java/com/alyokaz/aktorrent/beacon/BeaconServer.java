package com.alyokaz.aktorrent.beacon;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeaconServer {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ServerSocket serverSocket;
    private final List<InetSocketAddress> peers;

    public BeaconServer(ServerSocket serverSocket, List<InetSocketAddress> peers) {
        this.serverSocket = serverSocket;
        this.peers = peers;
    }

    public void start() {
        executor.execute(()-> {
            try(ServerSocket serverSocket = this.serverSocket){
                while(!Thread.currentThread().isInterrupted()) {
                    executor.execute(new BeaconTask(serverSocket.accept(), peers));
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void shutDown() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InetSocketAddress getAddress() {
        return new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());
    }
}
