package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.pingserver.PingServer;
import com.alyokaz.aktorrent.server.message.BeaconMessage;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Beacon {

    final private List<InetSocketAddress> peers = new ArrayList<>();

    final private ExecutorService executor = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;

    public int start() {
        BlockingQueue<Integer> chosenPort = new LinkedBlockingQueue<>();
        executor.execute(() -> {
            try(ServerSocket serverSocket = new ServerSocket(0)){
                chosenPort.add(serverSocket.getLocalPort());
                this.serverSocket = serverSocket;
                while(!Thread.currentThread().isInterrupted()) {
                    try(Socket socket = serverSocket.accept();
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                        Object obj = in.readObject();
                        if(obj instanceof BeaconMessage) {
                            BeaconMessage message = (BeaconMessage) obj;
                            out.writeObject(peers);
                            peers.add(message.getServerAddress());
                        }
                    } catch (IOException | ClassNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        try {
            int port = chosenPort.take();
            PingServer pingServer = new PingServer(new DatagramSocket(port));
            pingServer.start();
            return port;
        } catch (InterruptedException | SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutDown() {
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
