package com.alyokaz.aktorrent.peerservice;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class AbstractPeersServiceTask implements Runnable {

    private final InetSocketAddress address;
    protected final PeerService peerService;

    public AbstractPeersServiceTask(InetSocketAddress address, PeerService peerService) {
        this.address = address;
        this.peerService = peerService;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(address.getHostName(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            process(in, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    abstract void process(ObjectInputStream in, ObjectOutputStream out);
}
