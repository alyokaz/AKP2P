package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class GetPeersTaskBase implements Runnable {

    private final InetSocketAddress address;
    protected final List<InetSocketAddress> peers;
    private final BiConsumer<ObjectInputStream, ObjectOutputStream> func;

    public GetPeersTaskBase(InetSocketAddress address, List<InetSocketAddress> peers, BiConsumer<ObjectInputStream, ObjectOutputStream> func) {
        this.address = address;
        this.peers = peers;
        this.func = func;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(address.getHostName(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            func.accept(in, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
