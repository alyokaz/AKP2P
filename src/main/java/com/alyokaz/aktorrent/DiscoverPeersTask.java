package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class DiscoverPeersTask implements Runnable {

    InetSocketAddress address;
    List<InetSocketAddress> peers;

    public DiscoverPeersTask(InetSocketAddress address, List<InetSocketAddress> peers) {
        this.address = address;
        this.peers = peers;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(address.getHostName(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(new Message(MessageType.REQUEST_PEERS));
            Object obj = in.readObject();
            List<InetSocketAddress> peerList;
            if (obj instanceof List<?>) {
                peerList = (List<InetSocketAddress>) obj;
                peers.addAll(peerList);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
